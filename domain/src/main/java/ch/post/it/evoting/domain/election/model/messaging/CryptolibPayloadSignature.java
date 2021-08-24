/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election.model.messaging;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;

/**
 * Cryptolib payload signature implementation
 */
public class CryptolibPayloadSignature implements PayloadSignature {

	private static final long serialVersionUID = 1L;

	@JsonProperty
	private final byte[] signatureContents;

	@JsonSerialize(contentUsing = PemSerializer.class)
	@JsonDeserialize(contentUsing = PemDeserializer.class)
	private final X509Certificate[] certificateChain;

	/**
	 * Creates the representation of a Cryptolib-based signature.
	 *
	 * @param signatureContents the byte stream containing the signature
	 * @param certificateChain  the certificate chain to be used when validating the signature
	 */
	@JsonCreator
	public CryptolibPayloadSignature(
			@JsonProperty("signatureContents")
					byte[] signatureContents,
			@JsonProperty("certificateChain")
					X509Certificate[] certificateChain) {
		this.signatureContents = signatureContents;
		this.certificateChain = certificateChain;
	}

	@Override
	public X509Certificate[] getCertificateChain() {
		return certificateChain;
	}

	/**
	 * @return the byte array representing the signature
	 */
	@Override
	public byte[] getSignatureContents() {
		return signatureContents;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final CryptolibPayloadSignature that = (CryptolibPayloadSignature) o;
		return Arrays.equals(signatureContents, that.signatureContents) && Arrays.equals(certificateChain, that.certificateChain);
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(signatureContents);
		result = 31 * result + Arrays.hashCode(certificateChain);
		return result;
	}

	public static class PemSerializer extends JsonSerializer<X509Certificate> {
		@Override
		public void serialize(X509Certificate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			try {
				gen.writeString(PemUtils.certificateToPem(value));
			} catch (GeneralCryptoLibException e) {
				throw new IOException(e);
			}
		}
	}

	public static class PemDeserializer extends JsonDeserializer<X509Certificate> {
		@Override
		public X509Certificate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
			try {
				return (X509Certificate) PemUtils.certificateFromPem(p.readValueAs(String.class));
			} catch (GeneralCryptoLibException e) {
				throw new IOException(e);
			}
		}
	}
}
