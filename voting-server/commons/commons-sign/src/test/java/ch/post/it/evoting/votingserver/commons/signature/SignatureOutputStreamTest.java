/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.signature;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.SignatureException;

import org.junit.Before;
import org.junit.Test;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;

/**
 * Tests of {@link SignatureOutputStream}.
 */
public class SignatureOutputStreamTest {
	private static final byte[] DATA = { 0, 1, 2 };

	private AsymmetricServiceAPI service;

	private KeyPair pair;

	private Signature signature;

	private ByteArrayOutputStream bytes;

	private SignatureOutputStream stream;

	@Before
	public void setUp() throws GeneralCryptoLibException {
		service = new AsymmetricService();
		pair = service.getKeyPairForSigning();
		signature = SignatureFactoryImpl.newInstance().newSignature();
		bytes = new ByteArrayOutputStream(3);
		stream = new SignatureOutputStream(bytes, signature);
	}

	@Test(expected = IOException.class)
	public void testWriteArrayIntIntSignatureException() throws IOException {
		stream.write(DATA);
	}

	@Test
	public void testWriteByteArrayIntInt() throws InvalidKeyException, IOException, SignatureException, GeneralCryptoLibException {
		signature.initSign(pair.getPrivate());
		stream.write(DATA, 0, 2);
		stream.write(DATA, 2, 1);
		assertArrayEquals(DATA, bytes.toByteArray());
		assertTrue(service.verifySignature(signature.sign(), pair.getPublic(), DATA));
	}

	@Test
	public void testWriteInt() throws IOException, InvalidKeyException, SignatureException, GeneralCryptoLibException {
		signature.initSign(pair.getPrivate());
		for (byte b : DATA) {
			stream.write(b);
		}
		assertArrayEquals(DATA, bytes.toByteArray());
		assertTrue(service.verifySignature(signature.sign(), pair.getPublic(), DATA));
	}

	@Test(expected = IOException.class)
	public void testWriteIntSignatureException() throws IOException {
		stream.write(0);
	}
}
