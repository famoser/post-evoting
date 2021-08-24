/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.SpringConfigTest;
import ch.post.it.evoting.sdm.config.model.authentication.service.StartVotingKeyService;

/**
 * Test class for the StartVotingKeyService
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringConfigTest.class, loader = AnnotationConfigContextLoader.class)
class StartVotingKeyServiceTest {

	@Autowired
	private StartVotingKeyService startVotingKeyServiceWithLongSecrets;

	@Test
	void getSVKLengthWithSecretsSpec() {

		final int startVotingKeyLength = assertDoesNotThrow(() -> startVotingKeyServiceWithLongSecrets.getStartVotingKeyLength());
		assertTrue(startVotingKeyLength > Constants.SVK_LENGTH);
	}

}
