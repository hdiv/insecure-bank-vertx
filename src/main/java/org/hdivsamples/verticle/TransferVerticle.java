package org.hdivsamples.verticle;

import static org.hdivsamples.verticle.Functions.CREATE_NEW_TRANSFER;

import java.util.Date;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.sql.DataSource;

import org.hdivsamples.bean.Transfer;
import org.hdivsamples.util.ExceptionUtils;
import org.hdivsamples.util.InsecureBankUtils;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

@ApplicationScoped
public class TransferVerticle extends AbstractVerticle {

	SQLClient client;

	public void init(final @Observes StartupEvent e, final Vertx vertx, final DataSource dataSource) {
		client = JDBCClient.create(vertx, dataSource);
	}

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		try {
			this.vertx.eventBus().consumer(CREATE_NEW_TRANSFER.name(), this::createNewTransfer);
			startPromise.complete();
		}
		catch (Exception e) {
			startPromise.fail(e);
		}
	}

	@Override
	public void stop(final Promise<Void> stopPromise) throws Exception {
		client.close();
		stopPromise.complete();
	}

	private void createNewTransfer(final Message<JsonObject> message) {
		try {
			client.getConnection(result -> {
				if (result.failed()) {
					message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(result.cause()));
				}
				else {
					Transfer transfer = message.body().mapTo(Transfer.class);
					SQLConnection connection = result.result();
					insertTransfer(connection, transfer).flatMap(r -> debitFromAccount(connection, transfer))
							.flatMap(r -> creditToAccount(connection, transfer)).subscribe().with(r -> {
								connection.close();
								JsonObject reply = new JsonObject();
								reply.put("succeeded", true);
								message.reply(reply);
							}, e ->
								connection.rollback(handler -> {
									connection.close();
									message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
								})
							);
				}
			});
		}
		catch (Exception e) {
			message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
		}
	}

	private Uni<Void> debitFromAccount(final SQLConnection connection, final Transfer transfer) {
		return getCashAccountBalance(connection, transfer.getFromAccount()).flatMap(actualAmount -> {
			double amountTotal = actualAmount - (transfer.getAmount() + transfer.getFee());
			double amount = actualAmount - transfer.getAmount();
			double amountWithFees = amount - transfer.getFee();
			return updateCashAccount(connection, transfer.getFromAccount(), amountTotal)
					.flatMap(r -> getCreditAccountId(connection, transfer.getFromAccount()))
					.flatMap(accountId -> updateCreditAccount(connection, accountId, amountTotal))
					.flatMap(r -> insertNewActivity(connection, transfer.getDate(), getTransferDesc(transfer), transfer.getFromAccount(),
							-transfer.getAmount(), amount))
					.flatMap(r -> insertNewActivity(connection, transfer.getDate(), "TRANSFER FEE", transfer.getFromAccount(),
							-transfer.getFee(), amountWithFees));
		});
	}

	private Uni<Void> creditToAccount(final SQLConnection connection, final Transfer transfer) {
		return getCreditAccountId(connection, transfer.getToAccount()).flatMap(accountId -> {
			if (accountId <= 0) {
				return Uni.createFrom().voidItem();
			}
			else {
				return getCashAccountBalance(connection, transfer.getToAccount()).flatMap(actualAmount -> {
					double amountTotal = actualAmount + transfer.getAmount();
					return updateCreditAccount(connection, accountId, amountTotal).flatMap(r -> insertNewActivity(connection,
							transfer.getDate(), getTransferDesc(transfer), transfer.getToAccount(), transfer.getAmount(), amountTotal));
				});
			}
		});
	}

	private Uni<Void> insertTransfer(final SQLConnection connection, final Transfer transfer) {
		return Uni.createFrom().emitter(emitter -> {
			String sql = "INSERT INTO transfer "
					+ "(fromAccount, toAccount, description, amount, fee, username, date) VALUES (?, ?, ?, ?, ?, ?, ?)";
			JsonArray params = new JsonArray();
			params.add(transfer.getFromAccount());
			params.add(transfer.getToAccount());
			params.add(transfer.getDescription());
			params.add(transfer.getAmount());
			params.add(transfer.getFee());
			params.add(transfer.getUsername());
			params.add(transfer.getDate().toInstant());
			connection.updateWithParams(sql, params, result -> {
				if (result.failed()) {
					emitter.fail(result.cause());
				}
				else {
					emitter.complete(null);
				}
			});
		});
	}

	private Uni<Double> getCashAccountBalance(final SQLConnection connection, final String account) {
		return Uni.createFrom().emitter(emitter -> {
			String select = "SELECT availablebalance FROM cashaccount WHERE number = ?";
			JsonArray selectParams = new JsonArray();
			selectParams.add(account);
			connection.querySingleWithParams(select, selectParams, selectResult -> {
				if (selectResult.failed()) {
					emitter.fail(selectResult.cause());
				}
				else {
					double actualAmount = selectResult.result().getDouble(0);
					emitter.complete(actualAmount);
				}
			});
		});
	}

	private Uni<Void> updateCashAccount(final SQLConnection connection, final String account, final double actualAmount) {
		return Uni.createFrom().emitter(emitter -> {
			String update = "UPDATE cashaccount SET availablebalance= ? WHERE number = ?";
			JsonArray updateParams = new JsonArray();
			updateParams.add(actualAmount);
			updateParams.add(account);
			connection.updateWithParams(update, updateParams, updateResult -> {
				if (updateResult.failed()) {
					emitter.fail(updateResult.cause());
				}
				else {
					emitter.complete(null);
				}
			});
		});
	}

	private Uni<Void> updateCreditAccount(final SQLConnection connection, final Long cashAccountId, final double balance) {
		return Uni.createFrom().emitter(emitter -> {
			String update = "UPDATE creditAccount SET availablebalance='" + balance + "' WHERE cashaccountid ='" + cashAccountId + "'";
			connection.update(update, updateResult -> {
				if (updateResult.failed()) {
					emitter.fail(updateResult.cause());
				}
				else {
					emitter.complete(null);
				}
			});
		});
	}

	private Uni<Long> getCreditAccountId(final SQLConnection connection, final String account) {
		return Uni.createFrom().emitter(emitter -> {
			String select = "SELECT id FROM cashaccount WHERE number = ?";
			JsonArray selectParams = new JsonArray();
			selectParams.add(account);
			connection.querySingleWithParams(select, selectParams, selectResult -> {
				if (selectResult.failed()) {
					emitter.fail(selectResult.cause());
				}
				else {
					JsonArray result = selectResult.result();
					emitter.complete(result == null || result.isEmpty() ? -1 : result.getLong(0));
				}
			});
		});
	}

	private Uni<Void> insertNewActivity(final SQLConnection connection, final Date date, final String description, final String account,
			final double amount, final double availablebalance) {
		return Uni.createFrom().emitter(emitter -> {
			String sql = "INSERT INTO transaction (date, description, number, amount, availablebalance) VALUES (?, ?, ?, ?, ?)";
			JsonArray params = new JsonArray();
			params.add(date.toInstant());
			params.add(description);
			params.add(account);
			params.add(InsecureBankUtils.round(amount, 2));
			params.add(InsecureBankUtils.round(availablebalance, 2));
			connection.updateWithParams(sql, params, result -> {
				if (result.failed()) {
					emitter.fail(result.cause());
				}
				else {
					emitter.complete(null);
				}
			});
		});
	}

	private String getTransferDesc(final Transfer transfer) {
		return "TRANSFER: "
				+ (transfer.getDescription().length() > 12 ? transfer.getDescription().substring(0, 12) : transfer.getDescription());
	}
}
