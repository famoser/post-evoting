/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.annotation.Resource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.model.confirmation.ConfirmationMessage;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;

@RunWith(Arquillian.class)
public class CastCodeResourceTest extends VoteVerificationArquillianTest {

	private final String CONFIRMATION_CODE = "123";

	private final String CONFIRMATION_SIGNATURE = "4567";

	private final String VERIFICATION_CARD_ID = "8315e25841ea458ea257f0cd1dfdc2d3";

	private final String VERIFICATION_CARD_SET_ID = "4953a0f1fd4448c0a9b9494ce3348056";

	private final String SIGNED_VERIFICATION_PUBLIC_KEY = "1";

	private final String VERIFICATION_CARD_KEYSTORE = "1";

	// Paths
	private final String RETRIEVE_CAST_CODES_PATH = CastCodeResource.RESOURCE_PATH + CastCodeResource.RETRIEVE_CAST_CODES_PATH;

	@Resource
	private UserTransaction userTransaction;

	@Test
	public void testRetrieveCastCodes_Successful(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws JsonProcessingException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SystemException, NotSupportedException {
		saveMockVerification();

		ConfirmationMessage confirmationMessage = new ConfirmationMessage();
		confirmationMessage.setConfirmationKey(CONFIRMATION_CODE);
		confirmationMessage.setSignature(CONFIRMATION_SIGNATURE);

		Response response = webTarget.path(RETRIEVE_CAST_CODES_PATH).resolveTemplate(CastCodeResource.PARAMETER_VALUE_TENANT_ID, TENANT_ID)
				.resolveTemplate(CastCodeResource.PARAMETER_VALUE_ELECTION_EVENT_ID, ELECTION_EVENT_ID)
				.resolveTemplate(CastCodeResource.PARAMETER_VALUE_VERIFICATION_CARD_ID, VERIFICATION_CARD_ID).request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(ObjectMappers.toJson(confirmationMessage), MediaType.APPLICATION_JSON_TYPE));

		assertThat(response.getStatus(), is(STATUS_OK));
	}

	private void saveMockVerification()
			throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		userTransaction.begin();

		Verification verification = new Verification();
		verification.setElectionEventId(ELECTION_EVENT_ID);
		verification.setSignedVerificationPublicKey(SIGNED_VERIFICATION_PUBLIC_KEY);
		verification.setTenantId(TENANT_ID);
		verification.setVerificationCardId(VERIFICATION_CARD_ID);
		verification.setVerificationCardKeystore(VERIFICATION_CARD_KEYSTORE);
		verification.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);
		entityManager.persist(verification);

		userTransaction.commit();
	}

}
