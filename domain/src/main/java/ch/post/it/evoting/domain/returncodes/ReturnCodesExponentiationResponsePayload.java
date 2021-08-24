/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.returncodes;

import static ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil.retrieveBigIntegerValueWithNullCheck;
import static ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil.retrieveCertificateValueWithNullCheck;
import static ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil.retrieveStringValueWithNullCheck;
import static ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil.storeBigIntegerValueWithNullCheck;
import static ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil.storeCertificateValueWithNullCheck;
import static ch.post.it.evoting.domain.returncodes.safestream.StreamSerializableUtil.storeStringValueWithNullCheck;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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

public class ReturnCodesExponentiationResponsePayload implements Payload, Serializable {

	private static final long serialVersionUID = 7270602003518788884L;

	private static final Executor executor = Executors.newSingleThreadExecutor();

	// Maps the partial Choice Return Codes (or the Confirmation Key) to the long Return Codes share.
	private Map<BigInteger, BigInteger> pccOrCkToLongReturnCodeShare;

	private String exponentiationProofJson;

	private String voterChoiceReturnCodeGenerationPublicKeyJson;

	private String voterVoteCastReturnCodeGenerationPublicKeyJson;

	@JsonDeserialize(as = CryptolibPayloadSignature.class)
	private PayloadSignature signature;

	public Map<BigInteger, BigInteger> getPccOrCkToLongReturnCodeShare() {
		return pccOrCkToLongReturnCodeShare;
	}

	public void setPccOrCkToLongReturnCodeShare(Map<BigInteger, BigInteger> pccOrCkToLongReturnCodeShare) {
		this.pccOrCkToLongReturnCodeShare = pccOrCkToLongReturnCodeShare;
	}

	public String getExponentiationProofJson() {
		return exponentiationProofJson;
	}

	public void setExponentiationProofJson(String exponentiationProofJson) {
		this.exponentiationProofJson = exponentiationProofJson;
	}

	public String getVoterChoiceReturnCodeGenerationPublicKeyJson() {
		return voterChoiceReturnCodeGenerationPublicKeyJson;
	}

	public void setVoterChoiceReturnCodeGenerationPublicKeyJson(String voterChoiceReturnCodeGenerationPublicKeyJson) {
		this.voterChoiceReturnCodeGenerationPublicKeyJson = voterChoiceReturnCodeGenerationPublicKeyJson;
	}

	@Override
	public void serialize(MessagePacker packer) throws IOException {
		if (pccOrCkToLongReturnCodeShare == null) {
			packer.packNil();
		} else {
			packer.packMapHeader(pccOrCkToLongReturnCodeShare.size());
			Set<Entry<BigInteger, BigInteger>> entrySet = pccOrCkToLongReturnCodeShare.entrySet();
			for (Entry<BigInteger, BigInteger> entry : entrySet) {
				storeBigIntegerValueWithNullCheck(packer, entry.getKey());
				storeBigIntegerValueWithNullCheck(packer, entry.getValue());
			}
		}
		storeStringValueWithNullCheck(packer, exponentiationProofJson);
		storeStringValueWithNullCheck(packer, voterChoiceReturnCodeGenerationPublicKeyJson);
		storeStringValueWithNullCheck(packer, voterVoteCastReturnCodeGenerationPublicKeyJson);
		if (signature == null) {
			packer.packNil();
		} else {
			packer.packArrayHeader(signature.getCertificateChain().length);
			for (X509Certificate cert : signature.getCertificateChain()) {
				try {
					storeCertificateValueWithNullCheck(packer, cert);
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
				pccOrCkToLongReturnCodeShare = null;
			} else {
				int mapSize = unpacker.unpackMapHeader();
				pccOrCkToLongReturnCodeShare = new LinkedHashMap<>(mapSize);
				for (int i = 0; i < mapSize; i++) {
					BigInteger key = retrieveBigIntegerValueWithNullCheck(unpacker);
					BigInteger value = retrieveBigIntegerValueWithNullCheck(unpacker);
					pccOrCkToLongReturnCodeShare.put(key, value);
				}
			}
			exponentiationProofJson = retrieveStringValueWithNullCheck(unpacker);
			voterChoiceReturnCodeGenerationPublicKeyJson = retrieveStringValueWithNullCheck(unpacker);
			voterVoteCastReturnCodeGenerationPublicKeyJson = retrieveStringValueWithNullCheck(unpacker);
			if (unpacker.tryUnpackNil()) {
				this.signature = null;
			} else {
				int arraySize = unpacker.unpackArrayHeader();
				X509Certificate[] certs = new X509Certificate[arraySize];
				for (int i = 0; i < arraySize; i++) {
					try {
						certs[i] = retrieveCertificateValueWithNullCheck(unpacker);
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
		// A pipe is created to be able to push the relevant objects in the payload to an output stream.
		// Another pipe is connected to the former, and provides an input stream to read the aforementioned objects
		// which will be the source of the signature.

		// This Sonar's rule declares that 'Resources should be closed'.
		// We deliberately use piped input/output streams to save memory.
		// The garbage collector removes the stream from memory before it becomes critical.
		@SuppressWarnings("squid:S2095")
		PipedInputStream pis = new PipedInputStream();
		@SuppressWarnings("squid:S2095")
		PipedOutputStream pos = new PipedOutputStream(pis);

		// Feed the piped output stream from another thread to prevent
		// deadlocks.
		executor.execute(() -> {
			try (PrintWriter writer = new PrintWriter(pos)) {
				for (Map.Entry<BigInteger, BigInteger> entry : pccOrCkToLongReturnCodeShare.entrySet()) {
					writer.write(entry.getKey() + ":" + entry.getValue());
				}
				writer.write(exponentiationProofJson);
				if (voterChoiceReturnCodeGenerationPublicKeyJson != null) {
					writer.write(voterChoiceReturnCodeGenerationPublicKeyJson);
				}
				if (voterVoteCastReturnCodeGenerationPublicKeyJson != null) {
					writer.write(voterVoteCastReturnCodeGenerationPublicKeyJson);
				}
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
		return StreamSerializableClassType.RETURN_CODES_EXPONENTIATION_RESPONSE_PAYLOAD;
	}

	public String getVoterVoteCastReturnCodeGenerationPublicKeyJson() {
		return voterVoteCastReturnCodeGenerationPublicKeyJson;
	}

	public void setVoterVoteCastReturnCodeGenerationPublicKeyJson(String voterVoteCastReturnCodeGenerationPublicKeyJson) {
		this.voterVoteCastReturnCodeGenerationPublicKeyJson = voterVoteCastReturnCodeGenerationPublicKeyJson;
	}
}
