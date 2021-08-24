/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.sdm.config.spring.batch.CommonBatchInfrastructure;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {

	private static final String VERIFICATION_CARD_SET_ID = "0d4c64d0187341c28aa9065d86fd0069";

	private static final String ELECTION_EVENT_ID = "34523tdfvasdfasfdadfasfdasdfa";
	@InjectMocks
	private final ConfigurationService configurationService = new ConfigurationService();
	@Mock
	private CommonBatchInfrastructure commonBatchInfrastructureMock;
	@Mock
	private LoggingWriter loggingWriterMock;

	@Test
	void testCreateZeroVerificationCardIds() {
		createStreamAndTest(0);
	}

	@Test
	void testCreateOneVerificationCardId() {
		createStreamAndTest(1);
	}

	@Test
	void testCreateManyVerificationCardIds() {
		createStreamAndTest(10_000);
	}

	private void createStreamAndTest(int numItems) {
		Stream<String> verificationCardIds = configurationService
				.createVerificationCardIdStream(ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID, numItems);

		final AtomicInteger count = new AtomicInteger();
		verificationCardIds.forEach(vcid -> count.incrementAndGet());
		assertEquals(numItems, count.get());
	}
}
