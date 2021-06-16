package org.hdivsamples.provider;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionProvider extends AbstractExceptionProvider<NotFoundException> {
}
