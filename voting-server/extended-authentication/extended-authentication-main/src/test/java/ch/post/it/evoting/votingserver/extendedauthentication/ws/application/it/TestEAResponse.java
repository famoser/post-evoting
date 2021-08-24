/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.extendedauthentication.ws.application.it;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class TestEAResponse {

	private String responseCode;

	private Integer numberOfRemainingAttempts;

	private String encryptedSVK;

	public TestEAResponse(String responseCode, Integer numberOfRemainingAttempts, String encryptedSVK) {
		this.responseCode = responseCode;
		this.numberOfRemainingAttempts = numberOfRemainingAttempts;
		this.encryptedSVK = encryptedSVK;
	}

	public TestEAResponse() {
	}

	public String getResponseCode() {
		return responseCode;
	}

	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}

	public Integer getNumberOfRemainingAttempts() {
		return numberOfRemainingAttempts;
	}

	public void setNumberOfRemainingAttempts(Integer numberOfRemainingAttempts) {
		this.numberOfRemainingAttempts = numberOfRemainingAttempts;
	}

	public String getEncryptedSVK() {
		return encryptedSVK;
	}

	public void setEncryptedSVK(String encryptedSVK) {
		this.encryptedSVK = encryptedSVK;
	}
}
