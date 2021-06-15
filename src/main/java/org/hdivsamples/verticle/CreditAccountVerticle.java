package org.hdivsamples.verticle;

import static org.hdivsamples.verticle.Functions.FIND_CREDIT_ACCOUNTS_BY_USERNAME;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.sql.DataSource;

import org.hdivsamples.bean.CreditAccount;
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
public class CreditAccountVerticle extends AbstractVerticle {

	SQLClient client;

	public void init(final @Observes StartupEvent e, final Vertx vertx, final DataSource dataSource) {
		client = JDBCClient.create(vertx, dataSource);
	}

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		try {
			this.vertx.eventBus().consumer(FIND_CREDIT_ACCOUNTS_BY_USERNAME.name(), this::findCreditAccountsByUsername);
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

	private void findCreditAccountsByUsername(final Message<JsonObject> message) {
		try {
			JsonObject body = message.body();
			String username = body.getString("username");
			String str = "select * from creditaccount  where username='" + username + "'";
			client.query(str, result -> {
				if (result.failed()) {
					message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(result.cause()));
				}
				else {
					ResultSet resultSet = result.result();
					List<CreditAccount> accounts = resultSet.getRows().stream().map(new CreditAccountMapper()).collect(Collectors.toList());
					message.reply(new JsonArray(Json.encodeToBuffer(accounts)));
				}
			});
		}
		catch (Exception e) {
			message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
		}
	}

	private static class CreditAccountMapper implements Function<JsonObject, CreditAccount> {
		@Override
		public CreditAccount apply(final JsonObject rs) {
			CreditAccount localAccount = new CreditAccount();
			localAccount.setId(rs.getInteger("ID"));
			localAccount.setNumber(rs.getString("NUMBER"));
			localAccount.setUsername(rs.getString("USERNAME"));
			localAccount.setDescription(rs.getString("DESCRIPTION"));
			localAccount.setAvailableBalance(rs.getDouble("AVAILABLEBALANCE"));
			return localAccount;
		}
	}
}