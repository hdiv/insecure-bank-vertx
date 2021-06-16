package org.hdivsamples.bean;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

import javax.ws.rs.FormParam;
import java.util.Base64;
import java.util.Date;

public class Transfer {

	private int id;

	@FormParam("fromAccount")
	private String fromAccount;

	@FormParam("toAccount")
	private String toAccount;

	@FormParam("description")
	private String description;

	@FormParam("amount")
	private double amount;

	private double fee = 20.0;

	private String username;

	private Date date;

	public static Transfer valueOf(final String cookie) {
		if (cookie == null || cookie.isEmpty()) {
			return null;
		}
		return Json.decodeValue(Buffer.buffer(Base64.getDecoder().decode(cookie)), Transfer.class);
	}

	public String asString() {
		return Base64.getEncoder().encodeToString(Json.encodeToBuffer(this).getBytes());
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public String getFromAccount() {
		return fromAccount;
	}

	public void setFromAccount(final String fromAccount) {
		this.fromAccount = fromAccount;
	}

	public String getToAccount() {
		return toAccount;
	}

	public void setToAccount(final String toAccount) {
		this.toAccount = toAccount;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(final double amount) {
		this.amount = amount;
	}

	public double getFee() {
		return fee;
	}

	public void setFee(final double fee) {
		this.fee = fee;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(final Date date) {
		this.date = date;
	}
}
