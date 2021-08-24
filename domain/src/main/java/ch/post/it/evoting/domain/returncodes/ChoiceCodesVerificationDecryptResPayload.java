/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.msgpack.core.MessagePacker;
import org.msgpack.core.MessageUnpacker;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.domain.election.model.messaging.CryptolibPayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.Payload;
import ch.post.it.evoting.domain.election.model.messaging.PayloadSignature;
import ch.post.it.evoting.domain.election.model.messaging.SafeStreamDeserializationException;
import ch.post.it.evoting.domain.election.model.messaging.StreamSerializableClassType;
import ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil;

public class ChoiceCodesVerificationDecryptResPayload implements Payload {

	private static final Executor executor = Executors.newSingleThreadExecutor();

	private List<String> decryptContributionResult;

	private String exponentiationProofJson;

	private String publicKeyJson;

	@JsonDeserialize(as = CryptolibPayloadSignature.class)
	private PayloadSignature signature;

	public ChoiceCodesVerificationDecryptResPayload() {
		super();
	}

	public String getExponentiationProofJson() {
		return exponentiationProofJson;
	}

	public void setExponentiationProofJson(String exponentiationProofJson) {
		this.exponentiationProofJson = exponentiationProofJson;
	}

	public List<String> getDecryptContributionResult() {
		return decryptContributionResult;
	}

	public void setDecryptContributionResult(List<String> decryptContributionResult) {
		this.decryptContributionResult = decryptContributionResult;
	}

	public String getPublicKeyJson() {
		return publicKeyJson;
	}

	public void setPublicKeyJson(String publicKeyJson) {
		this.publicKeyJson = publicKeyJson;
	}

	@Override
	public void serialize(MessagePacker packer) throws IOException {
		if (decryptContributionResult == null) {
			packer.packNil();
		} else {
			packer.packArrayHeader(decryptContributionResult.size());
			for (String string : decryptContributionResult) {
				StreamSerializableUtil.storeStringValueWithNullCheck(packer, string);
			}
		}
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, exponentiationProofJson);
		StreamSerializableUtil.storeStringValueWithNullCheck(packer, publicKeyJson);
		if (signature == null) {
			packer.packNil();
		} else {
			packer.packArrayHeader(signature.getCertificateChain().length);
			for (X509Certificate cert : signature.getCertificateChain()) {
				try {
					StreamSerializableUtil.storeCertificateValueWithNullCheck(packer, cert);
				} catch (GeneralCryptoLibException e) {
					throw new IOException(e);
				}
			}
			packer.packBinaryHeader(signature.getSignatureContents().length);
			packer.addPayload(signature.getSignatureContents());
		}
	}

	@Override
	public void deserialize(MessageUnpacker unpacker) throws SafeStreamDeserializationException {
		try {
			if (unpacker.tryUnpackNil()) {
				decryptContributionResult = null;
			} else {
				int listSize = unpacker.unpackArrayHeader();
				decryptContributionResult = new ArrayList<>(listSize);
				for (int i = 0; i < listSize; i++) {
					decryptContributionResult.add(StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker));
				}
			}
			exponentiationProofJson = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			publicKeyJson = StreamSerializableUtil.retrieveStringValueWithNullCheck(unpacker);
			if (unpacker.tryUnpackNil()) {
				this.signature = null;
			} else {
				int arraySize = unpacker.unpackArrayHeader();
				X509Certificate[] certs = new X509Certificate[arraySize];
				for (int i = 0; i < arraySize; i++) {
					try {
						certs[i] = StreamSerializableUtil.retrieveCertificateValueWithNullCheck(unpacker);
					} catch (GeneralCryptoLibException e) {
						throw new SafeStreamDeserializationException(e);
					}
				}

				int signatureLength = unpacker.unpackBinaryHeader();
				byte[] signatureContents = unpacker.readPayload(signatureLength);

				this.signature = new CryptolibPayloadSignature(signatureContents, certs);
			}
		} catch (IOException e) {
			throw new SafeStreamDeserializationException(e);
		}
	}

	@Override
	public InputStream getSignableContent() throws IOException {
		// A pipe is created to be able to push the relevant objects in the payload to an output
		// stream. Another pipe is connected to the former, and provides an input stream to read
		// the aforementioned objects, which will be the source of the signature.

		// This Sonar's rule declares that 'Resources should be closed'.
		// We deliberately use piped input/output streams to save memory.
		// The garbage collector removes the stream from memory before it becomes critical.
		@SuppressWarnings("squid:S2095")
		PipedInputStream pis = new PipedInputStream();
		@SuppressWarnings("squid:S2095")
		PipedOutputStream pos = new PipedOutputStream(pis);

		// Feed the piped output stream from another thread to prevent deadlocks.
		executor.execute(() -> {
			try (PrintWriter writer = new PrintWriter(pos)) {
				for (String decryptionContributionResultEntry : decryptContributionResult) {
					writer.write(decryptionContributionResultEntry);
				}
				writer.write(exponentiationProofJson);
				writer.write(publicKeyJson);
			}
		});

		return pis;
	}

	@Override
	public PayloadSignature getSignature() {
		return signature;
	}

	@Override
	public void setSignature(PayloadSignature signature) {
		this.signature = signature;
	}

	@Override
	public StreamSerializableClassType type() {
		return StreamSerializableClassType.CHOICE_CODES_VERIFICATION_DECRYPT_RES_PAYLOAD;
	}

}
