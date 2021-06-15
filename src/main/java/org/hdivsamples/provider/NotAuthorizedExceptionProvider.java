package org.hdivsamples.provider;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.ext.Provider;

@Provider
public class NotAuthorizedExceptionProvider extends AbstractExceptionProvider<NotAuthorizedException>{
}
