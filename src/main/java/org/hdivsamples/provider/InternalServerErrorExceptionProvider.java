package org.hdivsamples.provider;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.ext.Provider;

@Provider
public class InternalServerErrorExceptionProvider extends AbstractExceptionProvider<InternalServerErrorException> {
}
