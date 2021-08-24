/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomString;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.SecureRandomFactory;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKeyGenerator;

/**
 * Service responsible of generating the start voting key
 */
@Service
public class StartVotingKeyService {

	protected static final int DESIRED_BASE = 2;

	/**
	 * The default charset for the SVK is 32 so 5 is the number of bits that cover this representation
	 *
	 * @see Constants#SVK_ALPHABET
	 */
	protected static final int NUMBER_OF_BITS_PER_SVK_CHARACTER = 5;

	private final AuthenticationKeyGenerator authenticationKeyGenerator;
	private final CryptoRandomString cryptoRandomString;
	private int startVotingKeyLength;

	public StartVotingKeyService(AuthenticationKeyGenerator authenticationKeyGenerator) {
		this.authenticationKeyGenerator = authenticationKeyGenerator;
		SecureRandomFactory secureRandomFactory = new SecureRandomFactory();
		cryptoRandomString = secureRandomFactory.createStringRandom(Constants.SVK_ALPHABET);
	}

	@PostConstruct
	public void init() {

		startVotingKeyLength = calculateStartVotingKeyLength();
	}

	public String generateStartVotingKey() throws GeneralCryptoLibException {

		return cryptoRandomString.nextRandom(getStartVotingKeyLength());

	}

	/**
	 * Calculates the necessary length for the start voting key to guarantee the same entropy as the authentication key
	 *
	 * @return length of start voting key
	 */
	private int calculateStartVotingKeyLength() {

		int result;

		if (authenticationKeyGenerator.getAlphabet().length() == Constants.SVK_ALPHABET.length()) {
			// By default, the start voting key length equals the authentication key length.
			// We use a different start voting key length only
			// in case there are multiple extended authentication factors
			result = authenticationKeyGenerator.getSecretsLength();
		} else {

			int alphabetLength = authenticationKeyGenerator.getAlphabet().length();
			int secretsLength = authenticationKeyGenerator.getSecretsLength();

			double computation = computeAuthenticationEntropy(alphabetLength, secretsLength);
			result = (int) computation;

		}

		return result;

	}

	private double computeAuthenticationEntropy(final int alphabetLength, final int secretsLength) {

		double pow = Math.pow(alphabetLength, secretsLength);
		double log = Math.log(pow) / Math.log(DESIRED_BASE);
		double logValueRounded = Math.round(log);

		return Math.round(logValueRounded / NUMBER_OF_BITS_PER_SVK_CHARACTER);

	}

	/**
	 * Gets startVotingKeyLength.
	 *
	 * @return Value of startVotingKeyLength.
	 */
	public int getStartVotingKeyLength() {
		return startVotingKeyLength;
	}
}
