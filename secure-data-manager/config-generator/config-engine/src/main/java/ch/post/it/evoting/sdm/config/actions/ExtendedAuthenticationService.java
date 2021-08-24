/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.actions;

import java.util.Optional;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateAuthenticationValuesException;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationDerivedElement;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKey;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationKeyGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.ExtendedAuthChallenge;
import ch.post.it.evoting.sdm.config.model.authentication.ExtendedAuthInformation;
import ch.post.it.evoting.sdm.config.model.authentication.StartVotingKey;
import ch.post.it.evoting.sdm.config.model.authentication.service.AuthenticationKeyCryptoService;
import ch.post.it.evoting.sdm.config.model.authentication.service.ChallengeServiceAPI;

/**
 * Action responsible of the creation and handling of an authentication key
 */
public class ExtendedAuthenticationService {

	private final ChallengeServiceAPI challengeService;

	private final AuthenticationKeyCryptoService authKeyService;

	private final AuthenticationKeyGenerator authenticationKeyGenerator;

	public ExtendedAuthenticationService(final AuthenticationKeyCryptoService authKeyService,
			final AuthenticationKeyGenerator authenticationKeyGenerator, final ChallengeServiceAPI challengeService) {

		this.authenticationKeyGenerator = authenticationKeyGenerator;
		this.authKeyService = authKeyService;
		this.challengeService = challengeService;
	}

	/**
	 * Creates an Authentication Key based on the defined strategy
	 *
	 * @return
	 */
	private AuthenticationKey create(final StartVotingKey startVotingKey) {

		return authenticationKeyGenerator.generateAuthKey(startVotingKey);
	}

	/**
	 * Receives an authentication key and derives an auth id based on a specific salt and election event id
	 *
	 * @param electionEventId
	 * @param authenticationKey
	 * @return
	 */
	private AuthenticationDerivedElement deriveAuthenticationKeyId(final String electionEventId, final AuthenticationKey authenticationKey) {

		return authKeyService.deriveElement(Constants.AUTH_ID, electionEventId, authenticationKey.getValue());
	}

	/**
	 * Receives an authentication key and derives an auth id based on a specific salt and election event id
	 *
	 * @param electionEventId
	 * @param authenticationKey
	 * @return
	 */
	private AuthenticationDerivedElement deriveAuthenticationKeyPassword(final String electionEventId, final AuthenticationKey authenticationKey) {

		return authKeyService.deriveElement(Constants.AUTH_PW, electionEventId, authenticationKey.getValue());
	}

	/**
	 * Encrypts the SVK with the key generated in a AuthKey derivation
	 */
	private String encryptStartVotingKey(final StartVotingKey svk, final AuthenticationDerivedElement derivedKey) {
		return authKeyService.encryptSVK(svk, derivedKey);
	}

	/**
	 * Decrypts the SVK with the key generated in a AuthKey derivation
	 */
	public String decryptSVK(final String encryptedSVK, final AuthenticationDerivedElement derivedKey) {
		return authKeyService.decryptSVK(encryptedSVK, derivedKey);
	}

	public ExtendedAuthInformation create(final StartVotingKey startVotingKey, final String electionEventId) {

		try {
			final AuthenticationKey authenticationKey = create(startVotingKey);
			final AuthenticationDerivedElement authId = deriveAuthenticationKeyId(electionEventId, authenticationKey);
			final AuthenticationDerivedElement pin = deriveAuthenticationKeyPassword(electionEventId, authenticationKey);
			final String encryptedSVK = encryptStartVotingKey(startVotingKey, pin);
			final Optional<ExtendedAuthChallenge> extendedAuthChallenge = challengeService.createExtendedAuthChallenge();
			return new ExtendedAuthInformation.Builder().setAuthenticationId(authId).setAuthenticationKey(authenticationKey).setAuthenticationPin(pin)
					.setDerivedChallenges(extendedAuthChallenge).setEncryptedSVK(encryptedSVK).build();
		} catch (GeneralCryptoLibException e) {
			throw new GenerateAuthenticationValuesException(e);
		}
	}

}
