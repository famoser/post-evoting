/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.sign.beans;

import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

public class SignedRequestContent {

	private String url;

	private String method;

	private String body;

	private String originator;

	public SignedRequestContent(final String url, final String method, final String body, final String originator) {
		this.url = url;
		this.method = method;
		this.body = body;
		this.originator = originator;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getOriginator() {
		return originator;
	}

	public void setOriginator(String originator) {
		this.originator = originator;
	}

	public byte[] getBytes() {
		StringBuilder sb = new StringBuilder();

		// If the body is empty it will not be included in signature.
		if (StringUtils.isEmpty(body)) {
			return sb.append(method).append(originator).toString().getBytes(StandardCharsets.UTF_8);
		} else {
			return sb.append(method).append(body).append(originator).toString().getBytes(StandardCharsets.UTF_8);
		}
	}

}
