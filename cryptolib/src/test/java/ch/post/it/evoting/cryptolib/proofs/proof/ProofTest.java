/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.proof;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;

class ProofTest {

	private Proof _target;

	@BeforeEach
	public void setup() throws GeneralCryptoLibException {

		BigInteger q = new BigInteger("11");

		Exponent exp = new Exponent(q, BigInteger.TEN);

		List<Exponent> expList = new ArrayList<>();
		expList.add(new Exponent(q, BigInteger.ONE));
		expList.add(new Exponent(q, new BigInteger("4")));

		_target = new Proof(exp, expList);
	}

	@Test
	void whenCreateProofFromJsonStringNewProofWillCreateSameJsonString() throws GeneralCryptoLibException {

		Proof proof = Proof.fromJson(_target.toJson());

		Assertions.assertEquals(_target.toJson(), proof.toJson());
	}

	@Test
	void whenWriteProofToJsonStringCanReconstructOriginalProof() throws GeneralCryptoLibException {

		Proof proof = Proof.fromJson(_target.toJson());

		Assertions.assertEquals(_target, proof);
	}
}
