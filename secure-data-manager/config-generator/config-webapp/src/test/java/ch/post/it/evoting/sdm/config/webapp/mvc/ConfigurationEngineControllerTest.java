/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.webapp.mvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;

import ch.post.it.evoting.sdm.config.commands.api.ConfigurationService;

/**
 * Test the controller with a mock verification card ID generator.
 */
class ConfigurationEngineControllerTest extends ConfigurationEngineControllerTestBase {

	@Autowired
	ConfigurationService configurationService;

	@Test
	void precomputeManyItems() throws Exception {
		final int itemCount = 1_000;
		when(configurationService.createVerificationCardIdStream(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, itemCount))
				.thenReturn(createMockVerificationCardStream(itemCount));

		final MvcResult result = runPrecomputeTest(itemCount);

		// Ensure that the right amount of items is generated.
		final String expectedContent =
				createMockVerificationCardStream(itemCount).collect(Collectors.joining(ConfigurationEngineController.SEPARATOR))
						+ ConfigurationEngineController.SEPARATOR;

		assertEquals(expectedContent, result.getResponse().getContentAsString());
	}

	/**
	 * Create a stream of predictable values to simulate the output of the ConfigurationService stream of verification card IDs.
	 *
	 * @param itemCount how many verification card IDs to produce
	 * @return a stream with fake, predictable verification card IDs
	 */
	private Stream<String> createMockVerificationCardStream(final int itemCount) {
		return IntStream.range(0, itemCount).mapToObj(String::valueOf);
	}

	@Configuration
	@Import(ConfigurationEngineControllerTestBase.TestConfig.class)
	static class TestConfig {
		@Bean
		ConfigurationService configurationService() {
			return mock(ConfigurationService.class);
		}
	}
}
