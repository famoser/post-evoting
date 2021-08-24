/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIDerivedKey;
import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIKDFDeriver;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;

/**
 * Derive the Voter Choice Return Code generation private key k_(j,id) or the Voter Vote Cast Return Code generation private key kc_(j,id)
 */
@Service
public class VoterReturnCodeGenerationKeyDerivationService {

	private static final int BITS_TO_BYTES_FACTOR = 8;

	private final PrimitivesServiceAPI primitivesServiceAPI;

	public VoterReturnCodeGenerationKeyDerivationService(final PrimitivesServiceAPI primitivesServiceAPI) {
		this.primitivesServiceAPI = primitivesServiceAPI;
	}

	/**
	 * Derive the Voter Return Code Generation private key (Voter Choice Return Code generation private key or Voter Vote Cast Return Code generation
	 * private key). This method invokes the key derivation function (KDF) using the concatenation of the seed and the CCR_j Choice Return Code
	 * Generation private key k'_j as well as the byte length of p. The seed is the verification card id (for the Voter Choice Return Code generation
	 * private key) or the concatenation of the verification card id and the string "confirm" (for the Voter Vote Cast Return Code generation private
	 * key). If the output of the key derivation function (KDF) is equal to or larger than q, the key derivation function is invoked again using the
	 * output of the previous KDF invocation. The control component repeats this process until it obtains a KDF output that is smaller than q.
	 *
	 * @param ccrjChoiceReturnCodeGenerationPrivateKey the CCR_j Choice Return Code Generation private key k'_j
	 * @param seed                                     the verification card id or the verification card id || "confirm"
	 * @param q                                        the order of the group q
	 * @return the Voter Return Code Generation private key
	 * @throws GeneralCryptoLibException if key derivation fails.
	 */
	public Exponent deriveVoterReturnCodeGenerationPrivateKey(Exponent ccrjChoiceReturnCodeGenerationPrivateKey, String seed, BigInteger q)
			throws GeneralCryptoLibException {
		BigInteger derivedValue;
		boolean smallerThanQ;
		CryptoAPIDerivedKey derivedKey;
		CryptoAPIKDFDeriver kdfDeriver = primitivesServiceAPI.getKDFDeriver();
		String concatenation = seed.concat(ccrjChoiceReturnCodeGenerationPrivateKey.getValue().toString());
		byte[] derivedKeyBytes = concatenation.getBytes(StandardCharsets.UTF_8);

		int derivedKeyBitlength = q.bitLength() + 1;
		int derivedKeyBytelength = derivedKeyBitlength / BITS_TO_BYTES_FACTOR;

		do {
			derivedKey = kdfDeriver.deriveKey(derivedKeyBytes, derivedKeyBytelength);
			derivedKeyBytes = derivedKey.getEncoded();
			derivedValue = new BigInteger(1, derivedKeyBytes);
			derivedValue = derivedValue.clearBit(derivedKeyBitlength - 1);
			smallerThanQ = derivedValue.compareTo(q) < 0;
		} while (!smallerThanQ);

		return new Exponent(q, derivedValue);
	}
}
