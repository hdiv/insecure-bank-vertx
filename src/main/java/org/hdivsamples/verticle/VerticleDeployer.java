package org.hdivsamples.verticle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

@ApplicationScoped
public class VerticleDeployer {

	public void init(final @Observes StartupEvent e, final Vertx vertx, final Instance<Verticle> verticles) {
		verticles.forEach(vertx::deployVerticle);
	}
}
