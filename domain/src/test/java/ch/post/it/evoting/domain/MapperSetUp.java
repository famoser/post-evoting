/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain;

import org.junit.jupiter.api.BeforeAll;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;

public class MapperSetUp {

	public static ObjectMapper mapper;

	@BeforeAll
	static void setUpMapper() {
		mapper = ObjectMapperMixnetConfig.getNewInstance();
	}
}
