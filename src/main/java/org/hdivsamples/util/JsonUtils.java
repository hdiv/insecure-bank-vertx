package org.hdivsamples.util;

import java.util.List;
import java.util.stream.Collectors;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

;

public abstract class JsonUtils {

	private JsonUtils() {

	}

	public static <E> List<E> mapToList(final io.vertx.mutiny.core.eventbus.Message<JsonArray> message, final Class<E> target) {
		return mapToList(message.body(), target);
	}

	public static <E> List<E> mapToList(final io.vertx.core.eventbus.Message<JsonArray> message, final Class<E> target) {
		return mapToList(message.body(), target);
	}

	public static <E> List<E> mapToList(final JsonArray array, final Class<E> target) {
		return array.stream().map(JsonObject.class::cast).map(it -> it.mapTo(target)).collect(Collectors.toList());
	}
}
