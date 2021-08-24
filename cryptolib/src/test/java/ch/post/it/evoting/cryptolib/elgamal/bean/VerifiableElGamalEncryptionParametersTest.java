/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;

class VerifiableElGamalEncryptionParametersTest {

	@Test
	void testJsonRoundTrip() throws GeneralCryptoLibException {
		BigInteger _p = new BigInteger("23");
		BigInteger _q = new BigInteger("11");
		BigInteger _g = new BigInteger("2");
		String seed = "ElectionName";
		int pCounter = 1;
		int qCounter = 2;

		VerifiableElGamalEncryptionParameters encryptionParameters = new VerifiableElGamalEncryptionParameters(_p, _q, _g, seed, pCounter, qCounter);
		String jsonStr = encryptionParameters.toJson();

		VerifiableElGamalEncryptionParameters reconstructedElGamalEncryptionParameters = VerifiableElGamalEncryptionParameters.fromJson(jsonStr);

		String errorMsg = "The reconstructed ElGamal encryption parameters are not equal to the expected parameters";

		assertEquals(encryptionParameters, reconstructedElGamalEncryptionParameters, errorMsg);
	}

	@Test
	void testVerifiable() throws GeneralCryptoLibException, IOException {
		String jsonStr = loadJson("verifiable_encryption_parameters");

		VerifiableElGamalEncryptionParameters sut = VerifiableElGamalEncryptionParameters.fromJson(jsonStr);

		assertAll(() -> assertEquals(23, sut.getP().intValue()), () -> assertEquals(11, sut.getQ().intValue()),
				() -> assertEquals(2, sut.getG().intValue()), () -> assertEquals("ElectionName", sut.getSeed()),
				() -> assertEquals(123, sut.getPCounter()), () -> assertEquals(456, sut.getQCounter()));

	}

	@Test
	void fromVerifiableParametersToRegularParametersThroughJson() throws GeneralCryptoLibException {
		BigInteger _p = new BigInteger("23");
		BigInteger _q = new BigInteger("11");
		BigInteger _g = new BigInteger("2");
		String seed = "ElectionName";
		int pCounter = 1;
		int qCounter = 2;

		VerifiableElGamalEncryptionParameters verifiableParams = new VerifiableElGamalEncryptionParameters(_p, _q, _g, seed, pCounter, qCounter);

		String json = verifiableParams.toJson();

		ElGamalEncryptionParameters regularParams = ElGamalEncryptionParameters.fromJson(json);

		assertAll(() -> assertEquals(verifiableParams.getP(), regularParams.getP()),
				() -> assertEquals(verifiableParams.getQ(), regularParams.getQ()), () -> assertEquals(verifiableParams.getG(), regularParams.getG()),
				() -> assertEquals(verifiableParams.getGroup(), regularParams.getGroup()));

	}

	@Test
	void failOnMalformedJson() throws IOException {
		String jsonStr = loadJson("verifiable_encryption_parameters-malformed");

		assertThrows(GeneralCryptoLibException.class, () -> VerifiableElGamalEncryptionParameters.fromJson(jsonStr));
	}

	private String loadJson(String fileName) throws IOException {
		String fullPath = fileName + ".json";
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fullPath); Scanner scanner = new Scanner(is)) {
			scanner.useDelimiter("\\A");
			return scanner.hasNext() ? scanner.next() : "";
		}
	}
}
