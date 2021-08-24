/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import java.util.Optional;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.sdm.config.model.authentication.ExtendedAuthChallenge;

public interface ChallengeServiceAPI {

	/**
	 * Generates the necessary info to construct the extended authentication information.
	 *
	 * @return
	 */
	Optional<ExtendedAuthChallenge> createExtendedAuthChallenge() throws GeneralCryptoLibException;

}
