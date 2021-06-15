package org.hdivsamples.resource;

import static org.hdivsamples.util.JsonUtils.mapToList;
import static org.hdivsamples.verticle.Functions.FIND_ACCOUNTS;
import static org.hdivsamples.verticle.Functions.FIND_ACCOUNTS_BY_USERNAME;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.hdivsamples.bean.Account;
import org.hdivsamples.bean.CashAccount;
import org.hdivsamples.bean.CreditAccount;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;

@Path("/admin")
public class AdminResource {

	@Inject
	Vertx vertx;

	@Inject
	SecurityContext security;

	@CheckedTemplate
	public static class Templates {
		public static native TemplateInstance admin(Account account, List<Account> accounts);
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Uni<TemplateInstance> index() {
		JsonObject request = new JsonObject();
		request.put("username", security.getUserPrincipal().getName());
		return Uni.combine().all().unis(
				vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), request),
				vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS.name(), request))
				.asTuple().map(tuple -> {
					Account account = mapToList(tuple.getItem1(), Account.class).get(0);
					List<Account> accounts = mapToList(tuple.getItem2(), Account.class);
					return Templates.admin(account, accounts);
				});
	}
}
