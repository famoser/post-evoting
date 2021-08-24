/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.proof;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;

class ProofTest {

	private static BigInteger two;
	private static BigInteger three;
	private static BigInteger four;
	private static BigInteger q;
	private static Proof proof;
	private static Exponent hashValue;
	private static List<Exponent> z;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		q = new BigInteger("11");

		two = new BigInteger("2");
		three = new BigInteger("3");
		four = new BigInteger("4");

		hashValue = new Exponent(q, two);
		Exponent q11Value3 = new Exponent(q, three);
		Exponent q11Value4 = new Exponent(q, four);

		z = new ArrayList<>();
		z.add(q11Value3);
		z.add(q11Value4);

		proof = new Proof(hashValue, z);
	}

	@Test
	void givenNullCWhenCreateProofThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new Proof(null, z));
	}

	@Test
	void givenNullZWhenCreateProofThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new Proof(hashValue, null));
	}

	@Test
	void givenEmptyZWhenCreateProofThenException() {
		List<Exponent> emptyZ = new ArrayList<>();
		assertThrows(GeneralCryptoLibException.class, () -> new Proof(hashValue, emptyZ));
	}

	@Test
	void givenDifferentQsWhenCreateProofThenException() throws GeneralCryptoLibException {
		BigInteger differentQ = q.add(BigInteger.ONE);

		Exponent exponentWithDifferentQ = new Exponent(differentQ, two);
		assertThrows(GeneralCryptoLibException.class, () -> new Proof(exponentWithDifferentQ, z));
	}

	@Test
	void givenProofWhenToJsonCanReconstructProof() throws GeneralCryptoLibException {
		String jsonStr = proof.toJson();

		Proof reconstructedProof = Proof.fromJson(jsonStr);

		String errorMsg = "Returned proof does not equal expected proof";
		assertEquals(reconstructedProof, proof, errorMsg);
	}

	@Test
	void givenProofWhenGetValueThenExpectedValue() throws GeneralCryptoLibException {
		Exponent retrievedHashValue = proof.getHashValue();
		Exponent expectedHashValue = new Exponent(q, two);

		String errorMsg = "Returned hash value does not match expected value";
		assertEquals(expectedHashValue, retrievedHashValue, errorMsg);
	}

	@Test
	void givenProofWhenGetZThenExpectedValue() throws GeneralCryptoLibException {
		Exponent e3 = new Exponent(q, three);
		Exponent e4 = new Exponent(q, four);

		List<Exponent> listOfExponents = new ArrayList<>();
		listOfExponents.add(e3);
		listOfExponents.add(e4);

		List<Exponent> retrievedZ = proof.getValuesList();

		String errorMsg = "Returned z does not match expected value";
		assertEquals(listOfExponents, retrievedZ, errorMsg);
	}
}
