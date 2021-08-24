/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.messaging;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The contents of a message that can be serialised. The contents can also be signed.
 */
public interface Payload extends StreamSerializable {

	/**
	 * Gets an input stream with the data from the payload that is susceptible of being signed, in order to feed it to the signing service.
	 *
	 * @return the input stream to be signed
	 * @throws java.io.IOException if the content could not be made available as an input stream
	 */
	@JsonIgnore
	// The signable content must never be serialised.
	InputStream getSignableContent() throws IOException;

	/**
	 * @return a signature of the contents of the payload.
	 */
	PayloadSignature getSignature();

	/**
	 * @param signature the signature of the contents of the payload, as generated by an external signer
	 */
	void setSignature(PayloadSignature signature);
}
