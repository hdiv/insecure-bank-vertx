package org.hdivsamples.resource;

import static javax.ws.rs.core.Response.Status.FOUND;
import static org.hdivsamples.util.JsonUtils.mapToList;
import static org.hdivsamples.verticle.Functions.EXISTS_STORAGE;
import static org.hdivsamples.verticle.Functions.FIND_ACCOUNTS_BY_USERNAME;
import static org.hdivsamples.verticle.Functions.FIND_CASH_ACCOUNTS_BY_USERNAME;
import static org.hdivsamples.verticle.Functions.FIND_CREDIT_ACCOUNTS_BY_USERNAME;
import static org.hdivsamples.verticle.Functions.LOAD_STORAGE;
import static org.hdivsamples.verticle.Functions.SAVE_STORAGE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.hdivsamples.bean.Account;
import org.hdivsamples.bean.CashAccount;
import org.hdivsamples.bean.CreditAccount;
import org.hdivsamples.bean.FileUntrusted;
import org.hdivsamples.bean.FileUntrustedValid;
import org.hdivsamples.util.InsecureBankUtils;
import org.jboss.resteasy.reactive.MultipartForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;

@Path("/dashboard")
public class DashboardResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(DashboardResource.class);

	@Inject
	Vertx vertx;

	@Inject
	SecurityContext security;

	@CheckedTemplate
	public static class Templates {
		public static native TemplateInstance dashboard(Account account, List<CashAccount> cashAccounts,
				List<CreditAccount> creditAccounts);

		public static native TemplateInstance userDetail(Account account, List<CashAccount> cashAccounts,
				List<CreditAccount> creditAccounts);
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Uni<TemplateInstance> index() {
		JsonObject request = new JsonObject();
		request.put("username", security.getUserPrincipal().getName());
		return Uni.combine().all()
				.unis(vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), request),
						vertx.eventBus().<JsonArray> request(FIND_CASH_ACCOUNTS_BY_USERNAME.name(), request),
						vertx.eventBus().<JsonArray> request(FIND_CREDIT_ACCOUNTS_BY_USERNAME.name(), request))
				.asTuple().map(tuple -> {
					Account account = mapToList(tuple.getItem1(), Account.class).get(0);
					List<CashAccount> cashAccounts = mapToList(tuple.getItem2(), CashAccount.class);
					List<CreditAccount> creditAccounts = mapToList(tuple.getItem3(), CreditAccount.class);
					return Templates.dashboard(account, cashAccounts, creditAccounts);
				});
	}

	@Path("/userDetail")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Uni<TemplateInstance> index(@QueryParam("username") final String username) {
		JsonObject request = new JsonObject();
		request.put("username", username);
		return Uni.combine().all()
				.unis(vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), request),
						vertx.eventBus().<JsonArray> request(FIND_CASH_ACCOUNTS_BY_USERNAME.name(), request),
						vertx.eventBus().<JsonArray> request(FIND_CREDIT_ACCOUNTS_BY_USERNAME.name(), request))
				.asTuple().map(tuple -> {
					Account account = mapToList(tuple.getItem1(), Account.class).get(0);
					List<CashAccount> cashAccounts = mapToList(tuple.getItem2(), CashAccount.class);
					List<CreditAccount> creditAccounts = mapToList(tuple.getItem3(), CreditAccount.class);
					return Templates.userDetail(account, cashAccounts, creditAccounts);
				});
	}

	@Path("/userDetail/avatar")
	@GET
	@Consumes("*/*")
	@Produces("image/*")
	public Uni<Response> getImage(@QueryParam(value = "image") final String image) {
		JsonObject request = new JsonObject();
		request.put("fileName", image);
		return vertx.eventBus().<JsonObject> request(EXISTS_STORAGE.name(), request).flatMap(exists -> {
			request.put("fileName", exists.body().getBoolean("exists") ? image : "avatar.png");
			return vertx.eventBus().<JsonObject> request(LOAD_STORAGE.name(), request).map(load -> {
				File file = new File(load.body().getString("file"));
				return Response.ok(file, "image/*").header("Content-Disposition", "attachment; fileName=\"" + file.getName() + "\"")
						.build();
			});
		});
	}

	@Path("/userDetail/creditCardImage")
	@GET
	@Consumes("*/*")
	@Produces("image/*")
	public Response getCreditCardImage(@QueryParam(value = "url") final String url) {
		String downLoadImgFileName = InsecureBankUtils.getNameWithoutExtension(url) + "." + InsecureBankUtils.getFileExtension(url);
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(url);
		return Response.ok(stream, "image/*").header("Content-Disposition", "attachment; fileName=\"" + downLoadImgFileName + "\"").build();
	}

	@Path("/userDetail/avatar/update")
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_HTML)
	public Uni<Response> updateAvatar(@MultipartForm final AvatarUpdateForm form) {
		if (form != null && form.imageFile != null) {
			JsonObject request = new JsonObject();
			request.put("fileName", form.imageFile.toString());
			request.put("target", security.getUserPrincipal().getName() + ".png");
			String uri = "/dashboard/userDetail?username=" + security.getUserPrincipal().getName();
			return vertx.eventBus().<JsonObject> request(SAVE_STORAGE.name(), request)
					.map(result -> Response.status(FOUND).location(URI.create(uri)).build());
		}
		else {
			return Uni.createFrom().item(Response.status(400).build());
		}
	}

	@Path("/userDetail/certificate")
	@POST
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Uni<Response> getCertificate(@FormParam(value = "username") final String username) {
		JsonObject request = new JsonObject();
		request.put("username", username);
		return vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), request).map(message -> {
			Account account = mapToList(message, Account.class).get(0);
			try {
				File tmpFile = File.createTempFile("serial", ".ser");
				FileOutputStream fos = new FileOutputStream(tmpFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(new FileUntrustedValid(account.getName()));
				oos.close();
				fos.close();
				return Response.ok(new FileInputStream(tmpFile), MediaType.APPLICATION_OCTET_STREAM)
						.header("Content-Disposition", "attachment; fileName=\"" + account.getName() + ".jks\"").build();
			}
			catch (Exception e) {
				LOGGER.error("Error downloading certificate", e);
				return Response.serverError().build();
			}
		});
	}

	@Path("/userDetail/maliciouscertificate")
	@POST
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Uni<Response> getMaliciousCertificate(@FormParam(value = "username") final String username) {
		JsonObject request = new JsonObject();
		request.put("username", username);
		return vertx.eventBus().<JsonArray> request(FIND_ACCOUNTS_BY_USERNAME.name(), request).map(message -> {
			Account account = mapToList(message, Account.class).get(0);
			try {
				File tmpFile = File.createTempFile("serial", ".ser");
				FileOutputStream fos = new FileOutputStream(tmpFile);
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				oos.writeObject(new FileUntrusted(account.getName()));
				oos.close();
				fos.close();
				return Response.ok(new FileInputStream(tmpFile), MediaType.APPLICATION_OCTET_STREAM)
						.header("Content-Disposition", "attachment; fileName=\"MaliciousCertificate" + account.getName() + ".jks\"").build();
			}
			catch (Exception e) {
				LOGGER.error("Error downloading certificate", e);
				return Response.serverError().build();
			}
		});
	}

	@Path("/userDetail/newcertificate")
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.TEXT_PLAIN)
	public String processSimple(@MultipartForm final NewCertificateForm form) throws IOException, ClassNotFoundException {
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(form.file));
		ois.readObject();
		ois.close();
		return "<p>File '" + form.file.getName() + "' uploaded successfully</p>";
	}

	public static class AvatarUpdateForm {
		@FormParam("imageFile")
		public File imageFile;
	}

	public static class NewCertificateForm {
		@FormParam("file")
		public File file;
	}

}
