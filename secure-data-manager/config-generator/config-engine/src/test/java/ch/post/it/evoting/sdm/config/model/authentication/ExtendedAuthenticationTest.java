/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.model.authentication;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ch.post.it.evoting.sdm.config.SpringConfigTest;
import ch.post.it.evoting.sdm.config.actions.ExtendedAuthenticationService;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SpringConfigTest.class, loader = AnnotationConfigContextLoader.class)
class ExtendedAuthenticationTest {

	private static final StartVotingKey startVotingKey = StartVotingKey.ofValue("zpfrcmn28mcct4pf682w");

	@Autowired
	private ExtendedAuthenticationService extendedAuthenticationService;

	@Test
	void generateExtendedAuthentication() {

		String eeID = "d710a4df654a4d7480df52f0ae9de610";
		ExtendedAuthInformation extendedAuthentication = extendedAuthenticationService.create(startVotingKey, eeID);
		assertNotNull(extendedAuthentication.getAuthenticationId());
		AuthenticationKey authenticationKey = extendedAuthentication.getAuthenticationKey();
		assertNotNull(authenticationKey);

		assertNotNull(authenticationKey);

		assertNotNull(extendedAuthentication.getAuthenticationPin());

		Optional<ExtendedAuthChallenge> extendedAuthChallengeOptional = extendedAuthentication.getExtendedAuthChallenge();

		assertNotNull(extendedAuthChallengeOptional);

		assertNotNull(extendedAuthentication.getEncryptedSVK());
	}

}
