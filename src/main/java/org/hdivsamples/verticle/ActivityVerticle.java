package org.hdivsamples.verticle;

import static org.hdivsamples.verticle.Functions.FIND_TRANSACTIONS_BY_CASH_ACCOUNT_NUMBER;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.sql.DataSource;

import org.hdivsamples.bean.Transaction;
import org.hdivsamples.util.ExceptionUtils;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLClient;

@ApplicationScoped
public class ActivityVerticle extends AbstractVerticle {

	SQLClient client;

	public void init(final @Observes StartupEvent e, final Vertx vertx, final DataSource dataSource) {
		client = JDBCClient.create(vertx, dataSource);
	}

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		try {
			this.vertx.eventBus().consumer(FIND_TRANSACTIONS_BY_CASH_ACCOUNT_NUMBER.name(), this::findTransactionsByCashAccountNumber);
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

	private void findTransactionsByCashAccountNumber(final Message<JsonObject> message) {
		try {
			JsonObject body = message.body();
			String number = body.getString("number");
			String str = "SELECT * FROM transaction WHERE number = '" + number + "'";
			client.query(str, result -> {
				if (result.failed()) {
					message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(result.cause()));
				}
				else {
					ResultSet resultSet = result.result();
					List<Transaction> transactions = resultSet.getRows().stream().map(new TransactionMapper()).collect(Collectors.toList());
					message.reply(new JsonArray(Json.encodeToBuffer(transactions)));
				}
			});
		}
		catch (Exception e) {
			message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
		}
	}

	private static class TransactionMapper implements Function<JsonObject, Transaction> {

		@Override
		public Transaction apply(final JsonObject rs) {
			Transaction transaction = new Transaction();
			transaction.setId(rs.getInteger("ID"));
			transaction.setDate(Date.from(Instant.parse(rs.getString("DATE"))));
			transaction.setDescription(rs.getString("DESCRIPTION"));
			transaction.setNumber(rs.getString("NUMBER"));
			transaction.setAmount(rs.getDouble("AMOUNT"));
			transaction.setAvailablebalance(rs.getDouble("AVAILABLEBALANCE"));
			return transaction;
		}
	}
}