/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication.service;

import java.util.Optional;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIDerivedKey;
import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIPBKDFDeriver;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.model.authentication.AuthenticationDerivedElement;
import ch.post.it.evoting.sdm.config.model.authentication.ChallengeGenerator;
import ch.post.it.evoting.sdm.config.model.authentication.ExtendedAuthChallenge;
import ch.post.it.evoting.sdm.config.model.authentication.ExtraParams;

/**
 * Class that creates the challenge to be used in extended auth
 */
public class ChallengeService implements ChallengeServiceAPI {

	private final PrimitivesServiceAPI primitivesService;

	private final ChallengeGenerator challengeGenerator;

	public ChallengeService(final PrimitivesServiceAPI primitivesService, final ChallengeGenerator challengeGenerator) {

		this.primitivesService = primitivesService;
		this.challengeGenerator = challengeGenerator;

	}

	/**
	 * Creates the challenge used for extended authentication
	 *
	 * @return
	 * @throws GeneralCryptoLibException
	 */
	@Override
	public Optional<ExtendedAuthChallenge> createExtendedAuthChallenge() throws GeneralCryptoLibException {

		ExtraParams extraParams = challengeGenerator.generateExtraParams();
		Optional<String> extraParamsValue = extraParams.getValue();
		if (!extraParamsValue.isPresent()) {
			return Optional.empty();
		}

		String extraAuthParam = extraParamsValue.get();
		Optional<String> alias = extraParams.getAlias();

		byte[] salt = primitivesService.genRandomBytes(Constants.RANDOM_SALT_LENGTH);

		if (extraAuthParam.length() < Constants.PBKDF2_MIN_EXTRA_PARAM_LENGTH) {
			extraAuthParam = StringUtils.leftPad(extraAuthParam, Constants.PBKDF2_MIN_EXTRA_PARAM_LENGTH);

		}
		final CryptoAPIPBKDFDeriver pbkdfDeriver = primitivesService.getPBKDFDeriver();
		final CryptoAPIDerivedKey cryptoAPIDerivedKey = pbkdfDeriver.deriveKey(extraAuthParam.toCharArray(), salt);
		final byte[] keyIdBytes = cryptoAPIDerivedKey.getEncoded();
		final AuthenticationDerivedElement authenticationDerivedElement = AuthenticationDerivedElement
				.of(cryptoAPIDerivedKey, new String(Hex.encodeHex(keyIdBytes)));

		return Optional.of(ExtendedAuthChallenge.of(authenticationDerivedElement, alias, salt));
	}
}
