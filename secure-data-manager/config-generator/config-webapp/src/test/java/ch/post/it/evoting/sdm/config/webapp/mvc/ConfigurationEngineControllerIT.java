/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.webapp.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import ch.post.it.evoting.sdm.config.commands.api.ConfigurationService;

/**
 * Test the controller with the real verification card ID generator.
 *
 * @see ConfigurationEngineControllerTest
 */
class ConfigurationEngineControllerIT extends ConfigurationEngineControllerTestBase {

	@Test
	void precomputeAThousandItems() throws Exception {
		final int itemCount = 1000;

		final MvcResult result = runPrecomputeTest(itemCount);

		// Ensure that the right amount of items is generated.
		assertEquals(itemCount, result.getResponse().getContentAsString().split("\n").length);
	}

	@Configuration
	@Import(ConfigurationEngineControllerTestBase.TestConfig.class)
	static class TestConfig {
		@Bean
		ConfigurationService configurationService() {
			return new ConfigurationService();
		}
	}
}
