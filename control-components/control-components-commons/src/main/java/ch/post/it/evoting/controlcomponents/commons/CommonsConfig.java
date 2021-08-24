/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.certificates.CertificatesServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.api.stores.StoresServiceAPI;
import ch.post.it.evoting.cryptolib.asymmetric.service.PollingAsymmetricServiceFactory;
import ch.post.it.evoting.cryptolib.certificates.service.PollingCertificatesServiceFactory;
import ch.post.it.evoting.cryptolib.elgamal.service.PollingElGamalServiceFactory;
import ch.post.it.evoting.cryptolib.primitives.service.PollingPrimitivesServiceFactory;
import ch.post.it.evoting.cryptolib.proofs.service.PollingProofsServiceFactory;
import ch.post.it.evoting.cryptolib.stores.service.PollingStoresServiceFactory;
import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProof;
import ch.post.it.evoting.cryptoprimitives.zeroknowledgeproofs.ZeroKnowledgeProofService;

@Configuration
@ComponentScan
public class CommonsConfig {

	/**
	 * Defines the asymmetric service. The returned service is thread-safe,
	 *
	 * @return the asymmetric service.
	 */
	@Bean
	public AsymmetricServiceAPI asymmetricService() {
		return new PollingAsymmetricServiceFactory().create();
	}

	/**
	 * Defines the certificates service. The returned service is thread-safe,
	 *
	 * @return the certificates service.
	 */
	@Bean
	public CertificatesServiceAPI certificatesService() {
		return new PollingCertificatesServiceFactory().create();
	}

	/**
	 * Defines the ElGamal service. The returned service is thread-safe,
	 *
	 * @return the ElGamal service.
	 */
	@Bean
	public ElGamalServiceAPI elGamalService() {
		return new PollingElGamalServiceFactory().create();
	}

	/**
	 * Defines the primitives service. The returned service is thread-safe,
	 *
	 * @return the primitives service.
	 */
	@Bean
	public PrimitivesServiceAPI primitivesService() {
		return new PollingPrimitivesServiceFactory().create();
	}

	/**
	 * Defines the proof service. The returned service is thread-safe,
	 *
	 * @return the proof service.
	 */
	@Bean
	public ProofsServiceAPI proofsService() {
		return new PollingProofsServiceFactory().create();
	}

	/**
	 * Defines the stores service. The returned service is thread-safe,
	 *
	 * @return the stores service.
	 */
	@Bean
	public StoresServiceAPI storesService() {
		return new PollingStoresServiceFactory().create();
	}

	@Bean
	public HashService hashService() {
		return new HashService();
	}

	@Bean
	public ZeroKnowledgeProof zeroKnowledgeProof() {
		return new ZeroKnowledgeProofService();
	}

}
