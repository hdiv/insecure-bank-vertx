package org.hdivsamples.verticle;

import static org.hdivsamples.verticle.Functions.FIND_ACCOUNTS;
import static org.hdivsamples.verticle.Functions.FIND_ACCOUNTS_BY_USERNAME;
import static org.hdivsamples.verticle.Functions.FIND_ACCOUNTS_BY_USERNAME_AND_PASSWORD;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.sql.DataSource;

import org.hdivsamples.bean.Account;
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
public class AccountVerticle extends AbstractVerticle {

	SQLClient client;

	public void init(final @Observes StartupEvent e, final Vertx vertx, final DataSource dataSource) {
		client = JDBCClient.create(vertx, dataSource);
	}

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		try {
			this.vertx.eventBus().consumer(FIND_ACCOUNTS_BY_USERNAME_AND_PASSWORD.name(), this::findAccountsByUsernameAndPassword);
			this.vertx.eventBus().consumer(FIND_ACCOUNTS_BY_USERNAME.name(), this::findAccountsByUsername);
			this.vertx.eventBus().consumer(FIND_ACCOUNTS.name(), this::findAccounts);
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

	private void findAccountsByUsernameAndPassword(final Message<JsonObject> message) {
		try {
			JsonObject body = message.body();
			String username = body.getString("username");
			String password = body.getJsonObject("password").getString("password");
			String str = "select * from account where username='" + username + "' AND password='" + password + "'";
			client.query(str, result -> {
				if (result.failed()) {
					message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(result.cause()));
				}
				else {
					ResultSet resultSet = result.result();
					List<Account> accounts = resultSet.getRows().stream().map(new AccountMapper()).collect(Collectors.toList());
					message.reply(new JsonArray(Json.encodeToBuffer(accounts)));
				}
			});
		}
		catch (Exception e) {
			message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
		}
	}

	private void findAccountsByUsername(final Message<JsonObject> message) {
		try {
			JsonObject body = message.body();
			String username = body.getString("username");
			String str = "select * from account where username='" + username + "'";
			client.query(str, result -> {
				if (result.failed()) {
					message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(result.cause()));
				}
				else {
					ResultSet resultSet = result.result();
					List<Account> accounts = resultSet.getRows().stream().map(new AccountMapper()).collect(Collectors.toList());
					message.reply(new JsonArray(Json.encodeToBuffer(accounts)));
				}
			});
		}
		catch (Exception e) {
			message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
		}
	}

	private void findAccounts(final Message<JsonObject> message) {
		try {
			String str = "select * from account";
			client.query(str, result -> {
				if (result.failed()) {
					message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(result.cause()));
				}
				else {
					ResultSet resultSet = result.result();
					List<Account> accounts = resultSet.getRows().stream().map(new AccountMapper()).collect(Collectors.toList());
					message.reply(new JsonArray(Json.encodeToBuffer(accounts)));
				}
			});
		}
		catch (Exception e) {
			message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
		}
	}

	private static class AccountMapper implements Function<JsonObject, Account> {
		@Override
		public Account apply(final JsonObject rs) {
			Account localAccount = new Account();
			localAccount.setUsername(rs.getString("USERNAME"));
			localAccount.setName(rs.getString("NAME"));
			localAccount.setSurname(rs.getString("SURNAME"));
			localAccount.setPassword(rs.getString("PASSWORD"));
			return localAccount;
		}
	}
}
