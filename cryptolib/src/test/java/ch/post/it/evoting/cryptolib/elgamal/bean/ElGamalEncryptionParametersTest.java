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

class ElGamalEncryptionParametersTest {

	@Test
	void testJsonRoundTrip() throws GeneralCryptoLibException {
		BigInteger _p = new BigInteger("23");
		BigInteger _q = new BigInteger("11");
		BigInteger _g = new BigInteger("2");

		ElGamalEncryptionParameters _elGamalEncryptionParameters = new ElGamalEncryptionParameters(_p, _q, _g);
		String jsonStr = _elGamalEncryptionParameters.toJson();

		ElGamalEncryptionParameters reconstructedElGamalEncryptionParameters = ElGamalEncryptionParameters.fromJson(jsonStr);

		String errorMsg = "The reconstructed ElGamal encryption parameters are not equal to the expected parameters";

		assertEquals(_elGamalEncryptionParameters, reconstructedElGamalEncryptionParameters, errorMsg);
	}

	@Test
	void testVerifiable() throws GeneralCryptoLibException, IOException {
		String jsonStr = loadJson("encryption_parameters");

		ElGamalEncryptionParameters sut = ElGamalEncryptionParameters.fromJson(jsonStr);

		assertAll(() -> assertEquals(23, sut.getP().intValue()), () -> assertEquals(11, sut.getQ().intValue()),
				() -> assertEquals(2, sut.getG().intValue()));

	}

	@Test
	void testNoSeedNoCounter() throws GeneralCryptoLibException, IOException {
		String jsonStr = loadJson("encryption_parameters-no_seed-no_counter");

		ElGamalEncryptionParameters sut = ElGamalEncryptionParameters.fromJson(jsonStr);

		assertAll(() -> assertEquals(23, sut.getP().intValue()), () -> assertEquals(11, sut.getQ().intValue()),
				() -> assertEquals(2, sut.getG().intValue()));
	}

	@Test
	void testNoSeed() throws GeneralCryptoLibException, IOException {
		String jsonStr = loadJson("encryption_parameters-no_seed");

		ElGamalEncryptionParameters sut = ElGamalEncryptionParameters.fromJson(jsonStr);

		assertAll(() -> assertEquals(23, sut.getP().intValue()), () -> assertEquals(11, sut.getQ().intValue()),
				() -> assertEquals(2, sut.getG().intValue()));
	}

	@Test
	void testNoCounter() throws GeneralCryptoLibException, IOException {
		String jsonStr = loadJson("encryption_parameters-no_counter");

		ElGamalEncryptionParameters sut = ElGamalEncryptionParameters.fromJson(jsonStr);

		assertAll(() -> assertEquals(23, sut.getP().intValue()), () -> assertEquals(11, sut.getQ().intValue()),
				() -> assertEquals(2, sut.getG().intValue()));
	}

	@Test
	void failOnMissingRequiredData() throws IOException {
		String jsonStr = loadJson("encryption_parameters-no_p");

		assertThrows(GeneralCryptoLibException.class, () -> ElGamalEncryptionParameters.fromJson(jsonStr));
	}

	@Test
	void failOnMalformedJson() throws IOException {
		String jsonStr = loadJson("encryption_parameters-malformed");

		assertThrows(GeneralCryptoLibException.class, () -> ElGamalEncryptionParameters.fromJson(jsonStr));
	}

	private String loadJson(String fileName) throws IOException {
		String fullPath = fileName + ".json";
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fullPath); Scanner scanner = new Scanner(is)) {
			scanner.useDelimiter("\\A");
			return scanner.hasNext() ? scanner.next() : "";
		}
	}
}
