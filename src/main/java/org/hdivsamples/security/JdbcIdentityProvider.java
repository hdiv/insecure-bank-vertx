package org.hdivsamples.security;

import static org.hdivsamples.util.JsonUtils.mapToList;
import static org.hdivsamples.verticle.Functions.FIND_ACCOUNTS_BY_USERNAME_AND_PASSWORD;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hdivsamples.bean.Account;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.evidence.PasswordGuessEvidence;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.subscription.UniEmitter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class JdbcIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

	@Inject
	Vertx vertx;

	@Inject
	SecurityDomain domain;

	@Override
	public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
		return UsernamePasswordAuthenticationRequest.class;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(final UsernamePasswordAuthenticationRequest request,
			final AuthenticationRequestContext context) {
		return Uni.createFrom().emitter(em -> {
			vertx.eventBus().request(FIND_ACCOUNTS_BY_USERNAME_AND_PASSWORD.name(), JsonObject.mapFrom(request),
					this.onAccounts(request, em));
		});
	}

	private Handler<AsyncResult<Message<JsonArray>>> onAccounts(final UsernamePasswordAuthenticationRequest request,
			final UniEmitter<? super SecurityIdentity> emitter) {
		return message -> {
			try {
				if (message.failed()) {
					emitter.fail(message.cause());
				}
				else {
					List<Account> result = mapToList(message.result(), Account.class);
					if (result.isEmpty()) {
						throw new AuthenticationFailedException();
					}
					Account account = result.get(0);
					org.wildfly.security.auth.server.SecurityIdentity identity = domain.authenticate(account.getUsername(),
							new PasswordGuessEvidence(request.getPassword().getPassword()));
					QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
					builder.setPrincipal(identity.getPrincipal());
					builder.addCredential(request.getPassword());
					for (String role : identity.getRoles()) {
						builder.addRole(role);
					}
					emitter.complete(builder.build());
				}
			}
			catch (Exception e) {
				emitter.fail(e);
			}
		};
	}
}
