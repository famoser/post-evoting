/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.mixing.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptoprimitives.hashing.HashService;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;

@Configuration
public class TestConfig {

	@Bean
	ObjectMapper objectMapper() {
		return ObjectMapperMixnetConfig.getNewInstance();
	}

	@Bean
	HashService hashService() {
		final MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Failed to create message digest for the HashService.");
		}

		return new HashService(messageDigest);
	}
}
