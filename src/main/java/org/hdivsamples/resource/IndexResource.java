package org.hdivsamples.resource;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.FOUND;

@Path("/")
public class IndexResource {

	@GET
	@Produces(MediaType.TEXT_HTML)
	public Response index() {
		return Response.status(FOUND).location(URI.create("/dashboard/")).build();
	}
}
