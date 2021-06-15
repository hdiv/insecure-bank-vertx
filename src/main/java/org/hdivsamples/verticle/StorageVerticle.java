package org.hdivsamples.verticle;

import static org.hdivsamples.verticle.Functions.EXISTS_STORAGE;
import static org.hdivsamples.verticle.Functions.LOAD_STORAGE;
import static org.hdivsamples.verticle.Functions.SAVE_STORAGE;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import javax.enterprise.context.ApplicationScoped;

import org.hdivsamples.util.ExceptionUtils;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class StorageVerticle extends AbstractVerticle {

	private final String url = this.getClass().getClassLoader().getResource("").getPath() + "/avatars/";

	@Override
	public void start(final Promise<Void> startPromise) throws Exception {
		try {
			this.vertx.eventBus().consumer(EXISTS_STORAGE.name(), this::exists);
			this.vertx.eventBus().consumer(LOAD_STORAGE.name(), this::load);
			this.vertx.eventBus().consumer(SAVE_STORAGE.name(), this::save);
			startPromise.complete();
		}
		catch (Exception e) {
			startPromise.fail(e);
		}
	}

	private void exists(final Message<JsonObject> message) {
		try {
			JsonObject body = message.body();
			String fileName = body.getString("fileName");
			File file = new File(url + fileName);
			JsonObject response = new JsonObject();
			response.put("exists", file.exists());
			message.reply(response);
		}
		catch (Exception e) {
			message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
		}
	}

	private void save(final Message<JsonObject> message) {
		try {
			JsonObject body = message.body();
			File source = new File(body.getString("fileName"));
			File target = new File(url + body.getString("target"));
			Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			JsonObject response = new JsonObject();
			response.put("saved", true);
			message.reply(response);
		}
		catch (Exception e) {
			message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
		}
	}

	private void load(final Message<JsonObject> message) {
		try {
			JsonObject body = message.body();
			String fileName = body.getString("fileName");
			JsonObject response = new JsonObject();
			response.put("file", url + fileName);
			message.reply(response);
		}
		catch (Exception e) {
			message.fail(500, "Internal Server error: " + ExceptionUtils.getStackTrace(e));
		}
	}
}
