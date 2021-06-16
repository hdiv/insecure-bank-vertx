package org.hdivsamples.resource;

import static javax.ws.rs.core.Response.Status.FOUND;

import java.net.URI;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

@Path("/")
public class LoginResource {

	@ConfigProperty(name = "quarkus.http.auth.form.cookie-name")
	String cookieName;

	@CheckedTemplate
	public static class Templates {
		public static native TemplateInstance login(final boolean failed, final boolean logout);
	}

	@Path("login.html")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public TemplateInstance login(@QueryParam("failed") @DefaultValue("false") final boolean failed,
			@QueryParam("logout") @DefaultValue("false") final boolean logout) {
		return Templates.login(failed, logout);
	}

	@Path("logout")
	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response logout() {
		return Response.status(FOUND).cookie(new NewCookie(cookieName, "")).location(URI.create("/login.html?logout=true")).build();
	}
}