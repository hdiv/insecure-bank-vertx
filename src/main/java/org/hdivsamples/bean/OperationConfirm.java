package org.hdivsamples.bean;

import javax.ws.rs.FormParam;

public class OperationConfirm {

	@FormParam("code")
	public String code;

	@FormParam("action")
	public String action;

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public String getAction() {
		return action;
	}

	public void setAction(final String action) {
		this.action = action;
	}
}
