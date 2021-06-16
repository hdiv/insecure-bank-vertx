package org.hdivsamples.resource;

import static javax.ws.rs.core.Response.Status.FOUND;
import static org.hdivsamples.util.JsonUtils.mapToList;
import static org.hdivsamples.verticle.Functions.CREATE_NEW_TRANSFER;
import static org.hdivsamples.verticle.Functions.FIND_ACCOUNTS_BY_USERNAME;
import static org.hdivsamples.verticle.Functions.FIND_CASH_ACCOUNTS_BY_USERNAME;

import java.net.URI;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.hdivsamples.bean.Account;
import org.hdivsamples.bean.CashAccount;
import org.hdivsamples.bean.OperationConfirm;
import org.hdivsamples.bean.Transfer;
import org.hdivsamples.util.InsecureBankUtils;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;

@Path("/transfer")
public class TransferResource {

	private static final String PENDING_TRANSFER = "PENDING_TRANSFER";

	@CheckedTemplate
	public static class Templates {
		public static native TemplateInstance newTransfer(Account account, List<CashAccount> cashAccounts, Transfer transfer,
				boolean error);

		public static native TemplateInstance transferCheck(Account account, Transfer transfer, OperationConfirm operationConfirm);

		public static native TemplateInstance transferConfirmation(Account account, Transfer transfer, String accountType);
	}

	static class AccountType {
		public static final String PERSONAL = "Personal";

		public static final String BUSINESS = "Business";
	}

	@Inject
	Vertx vertx;

	@Inject
	SecurityContext security;

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Uni<Response> newTransferForm() {
		return newTransferForm(AccountType.PERSONAL);
	}

	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Uni<Response> transfer(@BeanParam final Transfer transfer, @CookieParam("accountType") final String accountType) {
		if (transfer != null) {
			return AccountType.PERSONAL.equals(accountType) ? transferCheck(transfer) : transferConfirmation(transfer, accountType);
		}
		else {
			return newTransferForm(accountType);
		}
	}

	@Path("/confirm")
	@POST
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Uni<Response> transferCheck(@BeanParam final OperationConfirm confirm, @CookieParam("accountType") final String accountType,
			@CookieParam("transfer") final Transfer transfer) {
		if ("confirm".equals(confirm.getAction()) && transfer != null) {
			return transferConfirmation(transfer, accountType);
		}
		else {
			return Uni.createFrom()
					.item(Response.status(FOUND).cookie(new NewCookie("transfer", "")).location(URI.create("/transfer")).build());
		}
	}

	private Uni<Response> transferCheck(final Transfer transfer) {
		JsonObject accountsRequest = new JsonObject();
		accountsRequest.put("username", security.getUserPrincipal().getName());
		return vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), accountsRequest).map(message -> {
			Account account = mapToList(message, Account.class).get(0);
			return addTransfer(Response.ok(Templates.transferCheck(account, transfer, new OperationConfirm())), transfer).build();
		});
	}

	private Uni<Response> transferConfirmation(final Transfer transfer, final String accountType) {
		JsonObject accountsRequest = new JsonObject();
		accountsRequest.put("username", security.getUserPrincipal().getName());

		return Uni.combine().all()
				.unis(vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), accountsRequest),
						vertx.eventBus().<JsonArray> request(FIND_CASH_ACCOUNTS_BY_USERNAME.name(), accountsRequest))
				.asTuple().flatMap(tuple -> {
					Account account = mapToList(tuple.getItem1(), Account.class).get(0);
					List<CashAccount> cashAccounts = mapToList(tuple.getItem2(), CashAccount.class);

					double aux = transfer.getAmount();
					if (aux == 0.0) {
						return Uni.createFrom()
								.item(clearTransfer(Response.ok(Templates.newTransfer(account, cashAccounts, transfer, true))).build());
					}

					transfer.setUsername(security.getUserPrincipal().getName());
					transfer.setDate(new Date());

					double amount = transfer.getAmount();
					transfer.setAmount(InsecureBankUtils.round(amount, 2));

					double feeAmount = transfer.getAmount() * transfer.getFee() / 100.0;
					transfer.setFee(InsecureBankUtils.round(feeAmount, 2));

					return vertx.eventBus().<JsonObject> request(CREATE_NEW_TRANSFER.name(), new JsonObject(Json.encodeToBuffer(transfer)))
							.map(message -> clearTransfer(Response.ok(Templates.transferConfirmation(account, transfer, accountType)))
									.build());
				});
	}

	private Uni<Response> newTransferForm(final String accountType) {
		JsonObject accountsRequest = new JsonObject();
		accountsRequest.put("username", security.getUserPrincipal().getName());

		return Uni.combine().all()
				.unis(vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), accountsRequest),
						vertx.eventBus().<JsonArray> request(FIND_CASH_ACCOUNTS_BY_USERNAME.name(), accountsRequest))
				.asTuple().map(tuple -> {
					Account account = mapToList(tuple.getItem1(), Account.class).get(0);
					List<CashAccount> cashAccounts = mapToList(tuple.getItem2(), CashAccount.class);

					Transfer newTransfer = new Transfer();
					newTransfer.setFee(5.00);

					return Response.ok(Templates.newTransfer(account, cashAccounts, newTransfer, false).render())
							.cookie(new NewCookie("accountType", accountType)).build();
				});
	}

	// we don't have sessions so we map it as a cookie and forget about trust boundary

	private Response.ResponseBuilder addTransfer(final Response.ResponseBuilder builder, final Transfer transfer) {
		return builder.cookie(new NewCookie("transfer", transfer.asString()));
	}

	private Response.ResponseBuilder clearTransfer(final Response.ResponseBuilder builder) {
		return builder.cookie(new NewCookie("transfer", ""));
	}
}
