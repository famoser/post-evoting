/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.primitives.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomString;
import ch.post.it.evoting.cryptolib.primitives.messagedigest.configuration.ConfigMessageDigestAlgorithmAndProvider;
import ch.post.it.evoting.cryptolib.primitives.securerandom.constants.SecureRandomConstants;

/**
 * Tests of the primitives service API.
 */
class PrimitivesServiceTest {

	private static final byte[] MESSAGE = "message".getBytes(StandardCharsets.UTF_8);

	private static PrimitivesService primitivesServiceForDefaultPolicy;

	private static byte[] data;

	@BeforeAll
	public static void setUp() {

		primitivesServiceForDefaultPolicy = new PrimitivesService();

		data = new PrimitivesService().genRandomBytes(getInt(1, SecureRandomConstants.MAXIMUM_GENERATED_BYTE_ARRAY_LENGTH));
	}

	private static int getInt(final int min, final int max) {

		SecureRandom secureRandom = new SecureRandom();

		int randomInt;
		do {
			randomInt = secureRandom.nextInt(max + 1);
		} while (randomInt < min);

		return randomInt;
	}

	private static void testSha3(String base64Digest, Properties properties) throws GeneralCryptoLibException {
		byte[] expected = Base64.getDecoder().decode(base64Digest);

		assertArrayEquals(expected, new PrimitivesService(properties).getHash(MESSAGE));
	}

	@Test
	void whenGetIntegerRandom() {
		assertNotNull(primitivesServiceForDefaultPolicy.getCryptoRandomInteger());
	}

	@Test
	void whenGetStringRandom() {
		CryptoAPIRandomString randomString = primitivesServiceForDefaultPolicy.get32CharAlphabetCryptoRandomString();

		assertNotNull(randomString);
	}

	@Test
	void whenGetKDFDeriver() {
		assertNotNull(primitivesServiceForDefaultPolicy.getKDFDeriver());
	}

	@Test
	void whenGetPBKDFDeriver() {
		assertNotNull(primitivesServiceForDefaultPolicy.getPBKDFDeriver());
	}

	@Test
	void whenGetDerivedFromPassword() {
		assertNotNull(primitivesServiceForDefaultPolicy.getPBKDFDeriver());
	}

	@Test
	void testThatGeneratesTheGivenNumberOfBytes() {

		byte[] bytes = primitivesServiceForDefaultPolicy.genRandomBytes(10);

		assertEquals(10, bytes.length);
	}

	@Test
	void whenGiveAnIncorrectLength() {

		assertThrows(IllegalArgumentException.class,
				() -> primitivesServiceForDefaultPolicy.genRandomBytes(SecureRandomConstants.MAXIMUM_GENERATED_BYTE_ARRAY_LENGTH + 1));
	}

	@Test
	void testThatNotGeneratesBytesWhenLengthIsNull() {

		assertThrows(IllegalArgumentException.class, () -> primitivesServiceForDefaultPolicy.genRandomBytes(0));
	}

	@Test
	void whenGetHash() throws GeneralCryptoLibException {
		assertNotNull(primitivesServiceForDefaultPolicy.getHash(data));
	}

	@Test
	void whenGetHashForDataInputStream() throws GeneralCryptoLibException, IOException {
		try (InputStream dataInputStream = new ByteArrayInputStream(data)) {
			assertNotNull(primitivesServiceForDefaultPolicy.getHash(dataInputStream));
		}
	}

	@Test
	void testHashOfObjects() throws GeneralCryptoLibException {
		Base64.Encoder encoder = Base64.getEncoder();
		Charset charset = Charset.defaultCharset();
		String object1 = "Test";
		Integer object2 = 1;
		BigInteger object3 = BigInteger.ZERO;

		byte[] message1 = encoder.encode(object1.getBytes(charset));
		byte[] message2 = encoder.encode(object2.toString().getBytes(charset));
		byte[] message3 = encoder.encode(object3.toString().getBytes(charset));

		// Message length = length of individual messages plus two separators.
		byte[] separator = PrimitivesService.HASH_OBJECT_SEPARATOR;
		int separatorLength = separator.length;
		byte[] message = new byte[message1.length + message2.length + message3.length + (2 * separatorLength)];
		System.arraycopy(message1, 0, message, 0, message1.length);
		System.arraycopy(separator, 0, message, message1.length, separatorLength);
		System.arraycopy(message2, 0, message, message1.length + separatorLength, message2.length);
		System.arraycopy(separator, 0, message, message1.length + separatorLength + message2.length, separatorLength);
		System.arraycopy(message3, 0, message, message1.length + message2.length + (2 * separatorLength), message3.length);

		// Regular hash
		byte[] digest1 = primitivesServiceForDefaultPolicy.getHash(message);
		// Objects hash
		byte[] digest2 = primitivesServiceForDefaultPolicy.getHashOfObjects(Stream.of(object1, object2, object3), charset);

		assertArrayEquals(digest1, digest2);
	}

	@Test
	void proveThatHashingAtOnceOrUpdatingYieldsTheSameResult() {
		MessageDigest md = primitivesServiceForDefaultPolicy.getRawMessageDigest();

		// Hash at once.
		String message1 = "Foo";
		String message2 = "Bar";
		byte[] hash1 = md.digest((message1 + message2).getBytes(StandardCharsets.UTF_8));

		// Hash each piece and then get the digest.
		md.reset();
		md.update(message1.getBytes(StandardCharsets.UTF_8));
		md.update(message2.getBytes(StandardCharsets.UTF_8));
		byte[] hash2 = md.digest();

		assertArrayEquals(hash1, hash2);
	}

	@Test
	void testCustomMessageDigestPolicyConstructor() throws GeneralCryptoLibException {
		Properties properties = new Properties();
		properties.setProperty("primitives.messagedigest", ConfigMessageDigestAlgorithmAndProvider.SHA512_224_DEFAULT.name());

		byte[] message = "Foo".getBytes(StandardCharsets.UTF_8);
		byte[] sha256Digest = primitivesServiceForDefaultPolicy.getHash(message);
		byte[] sha512Digest = new PrimitivesService(properties).getHash(message);

		assertNotEquals(sha256Digest.length, sha512Digest.length);
	}

	@Test
	void testSha3_256() throws GeneralCryptoLibException {
		Properties properties = new Properties();
		properties.setProperty("primitives.messagedigest", ConfigMessageDigestAlgorithmAndProvider.SHA3_256_BC.name());
		testSha3("f0oj2Q3pDRAHVPgtbBQHO3+0Zvdv0fYbGHufOcP/2JU=", properties);
	}

	@Test
	void testSha3_384() throws GeneralCryptoLibException {
		Properties properties = new Properties();
		properties.setProperty("primitives.messagedigest", ConfigMessageDigestAlgorithmAndProvider.SHA3_384_BC.name());
		testSha3("z/CoyzaqDeoMFKY5Y1lQiXmyTtSqn/H9pCjSbqj3MJYS9sTkQQQqILX94y4R4dm5", properties);
	}

	@Test
	void testSha3_512() throws GeneralCryptoLibException {
		Properties properties = new Properties();
		properties.setProperty("primitives.messagedigest", ConfigMessageDigestAlgorithmAndProvider.SHA3_512_BC.name());
		testSha3("a2hRa9/YXLW2zfp85plK6OWQU5l7ytk53XnyVN6ng+e5OoLUtKcpkCnGLgCfnxMN/UKCVSeYQVnHg0R6nsFCyw==", properties);
	}

}
