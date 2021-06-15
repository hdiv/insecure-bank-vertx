package org.hdivsamples.provider;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;

public class AbstractExceptionProvider<E extends Throwable> implements ExceptionMapper<E> {

	@CheckedTemplate
	public static class Templates {
		public static native TemplateInstance error(String message, int statusCode, Throwable error);
	}

	@Override
	public Response toResponse(final E exception) {
		String message = "Internal server error";
		int statusCode = INTERNAL_SERVER_ERROR.getStatusCode();
		if (exception instanceof NotFoundException) {
			message = "The resource you have requested has not been found";
			statusCode = NOT_FOUND.getStatusCode();
		}
		return Response.status(statusCode).entity(Templates.error(message, statusCode, exception).render()).type(MediaType.TEXT_HTML)
				.build();
	}

}
