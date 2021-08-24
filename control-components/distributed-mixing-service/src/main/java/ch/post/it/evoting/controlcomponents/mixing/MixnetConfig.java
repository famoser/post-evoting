/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.mixing;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptoprimitives.mixnet.Mixnet;
import ch.post.it.evoting.cryptoprimitives.mixnet.MixnetService;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;

@Configuration
public class MixnetConfig {

	@Bean
	public static PropertySourcesPlaceholderConfigurer propertyConfigInDev() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	@Bean
	public GroupElementsCompressor<ZpGroupElement> groupElementsCompressor() {
		return new GroupElementsCompressor<>();
	}

	@Bean
	ObjectMapper objectMapper() {
		return ObjectMapperMixnetConfig.getNewInstance();
	}

	@Bean
	Mixnet mixnet() {
		return new MixnetService();
	}

}
