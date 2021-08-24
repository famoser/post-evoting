/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.config.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptolib.api.stores.StoresServiceAPI;
import ch.post.it.evoting.cryptolib.stores.service.PollingStoresServiceFactory;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalService;

@Configuration
public class SpringConfig {

	@Bean
	public ElGamalService elGamalService() {
		return new ElGamalService();
	}

	@Bean
	public StoresServiceAPI storesService() {
		return new PollingStoresServiceFactory().create();
	}
}
