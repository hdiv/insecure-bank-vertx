package org.hdivsamples.resource;

import static org.hdivsamples.util.JsonUtils.mapToList;
import static org.hdivsamples.verticle.Functions.FIND_ACCOUNTS_BY_USERNAME;
import static org.hdivsamples.verticle.Functions.FIND_CASH_ACCOUNTS_BY_USERNAME;
import static org.hdivsamples.verticle.Functions.FIND_TRANSACTIONS_BY_CASH_ACCOUNT_NUMBER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.hdivsamples.bean.Account;
import org.hdivsamples.bean.CashAccount;
import org.hdivsamples.bean.Transaction;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;

@Path("/activity")
public class ActivityResource {

	@Inject
	Vertx vertx;

	@Inject
	SecurityContext security;

	@CheckedTemplate
	public static class Templates {
		public static native TemplateInstance accountActivity(Account account, List<CashAccount> cashAccounts, CashAccount cashAccount,
				List<Transaction> firstCashAccountTransfers, String actualCashAccountNumber);

		public static native TemplateInstance creditActivity(Account account, String actualCreditCardNumber);
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Uni<TemplateInstance> activity() {
		JsonObject accountsRequest = new JsonObject();
		accountsRequest.put("username", security.getUserPrincipal().getName());

		return Uni.combine().all()
				.unis(vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), accountsRequest),
						vertx.eventBus().<JsonArray> request(FIND_CASH_ACCOUNTS_BY_USERNAME.name(), accountsRequest))
				.asTuple().flatMap(tuple -> {
					CashAccount cashAccount = new CashAccount();
					Account account = mapToList(tuple.getItem1(), Account.class).get(0);
					List<CashAccount> cashAccounts = mapToList(tuple.getItem2(), CashAccount.class);
					String number = cashAccounts.get(0).getNumber();

					JsonObject transactionsRequest = new JsonObject();
					transactionsRequest.put("number", number);
					return vertx.eventBus().<JsonArray> request(FIND_TRANSACTIONS_BY_CASH_ACCOUNT_NUMBER.name(), transactionsRequest)
							.map(message -> {
								List<Transaction> firstCashAccountTransfers = mapToList(message, Transaction.class);
								List<Transaction> reverseFirstCashAccountTransfers = new ArrayList<>(firstCashAccountTransfers);
								Collections.reverse(reverseFirstCashAccountTransfers);

								return Templates.accountActivity(account, cashAccounts, cashAccount, reverseFirstCashAccountTransfers,
										number);
							});
				});
	}

	@Path("/detail")
	@POST
	@Produces(MediaType.TEXT_HTML)
	public Uni<TemplateInstance> changeAccount(@FormParam("account") final String number) {
		return showDetail(number);
	}

	@Path("/credit.html")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Uni<TemplateInstance> viewActivityByCreditNumber(@QueryParam("number") final String number) {
		JsonObject accountsRequest = new JsonObject();
		accountsRequest.put("username", security.getUserPrincipal().getName());
		return vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), accountsRequest).map(message -> {
					Account account = mapToList(message, Account.class).get(0);
					return Templates.creditActivity(account, number);
				});
	}

	@Path("/{account}/detail")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Uni<TemplateInstance> viewActivityByNumber(@PathParam("account") final String number) {
		return showDetail(number);
	}

	private Uni<TemplateInstance> showDetail(final String number) {
		JsonObject accountsRequest = new JsonObject();
		accountsRequest.put("username", security.getUserPrincipal().getName());
		JsonObject transactionsRequest = new JsonObject();
		transactionsRequest.put("number", number);
		return Uni.combine().all()
				.unis(vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), accountsRequest),
						vertx.eventBus().<JsonArray> request(FIND_CASH_ACCOUNTS_BY_USERNAME.name(), accountsRequest),
						vertx.eventBus().<JsonArray> request(FIND_TRANSACTIONS_BY_CASH_ACCOUNT_NUMBER.name(), transactionsRequest))
				.asTuple().map(tuple -> {
					CashAccount cashAccount = new CashAccount();
					Account account = mapToList(tuple.getItem1(), Account.class).get(0);
					List<CashAccount> cashAccounts = mapToList(tuple.getItem2(), CashAccount.class);

					List<Transaction> firstCashAccountTransfers = mapToList(tuple.getItem3(), Transaction.class);
					List<Transaction> reverseFirstCashAccountTransfers = new ArrayList<>(firstCashAccountTransfers);
					Collections.reverse(reverseFirstCashAccountTransfers);

					return Templates.accountActivity(account, cashAccounts, cashAccount, reverseFirstCashAccountTransfers, number);
				});
	}
}
