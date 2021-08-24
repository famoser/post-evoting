/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.ws.application;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.sdm.CryptoTestData;

/**
 * RSA and ElGamal keys required for the mix-dec-val integration test.
 */
@Configuration
public class KeyTestConfig {

	@Bean
	ElGamalKeyPair electoralAuthorityKeyPair() throws GeneralCryptoLibException {
		return CryptoTestData.generateElectoralAuthorityKeyPair(1);
	}

	@Bean
	KeyPair signingKeyPair(AsymmetricServiceAPI asymmetricService) throws NoSuchAlgorithmException {
		return asymmetricService.getKeyPairForSigning();
	}

}
