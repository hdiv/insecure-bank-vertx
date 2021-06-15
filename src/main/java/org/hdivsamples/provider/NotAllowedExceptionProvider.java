package org.hdivsamples.provider;

import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ext.Provider;

@Provider
public class NotAllowedExceptionProvider extends AbstractExceptionProvider<NotAllowedException> {
}
