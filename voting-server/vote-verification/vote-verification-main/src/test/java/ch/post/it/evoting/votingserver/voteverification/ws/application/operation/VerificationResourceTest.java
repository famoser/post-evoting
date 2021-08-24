/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;

@RunWith(Arquillian.class)
public class VerificationResourceTest extends VoteVerificationArquillianTest {

	private final String VERIFICATION_CARD_ID = "4953a0f1fd4448c0a9b9494ce3348056";
	private final String VERIFICATION_CARD_SET_ID = "4953a0f1fd4448c0a9b9494ce3348056";

	// Paths
	private final String GET_VOTER_INFORMATION_PATH = VerificationResource.RESOURCE_PATH + VerificationResource.GET_VOTER_INFORMATION_PATH;

	@Test
	public void testGetVoterInformation_Successful(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws JsonProcessingException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SystemException, NotSupportedException {
		saveMockVoterInformationToDatabase();

		String url = addPathValues(GET_VOTER_INFORMATION_PATH, VerificationResource.PARAMETER_VALUE_TENANT_ID, TENANT_ID,
				VerificationResource.PARAMETER_VALUE_ELECTION_EVENT_ID, ELECTION_EVENT_ID, VerificationResource.PARAMETER_VALUE_VERIFICATION_CARD_ID,
				VERIFICATION_CARD_ID);

		Response response = webTarget.path(url).request(MediaType.APPLICATION_JSON).get();
		assertThat(response.getStatus(), is(STATUS_OK));
	}

	private void saveMockVoterInformationToDatabase()
			throws JsonProcessingException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SystemException, NotSupportedException {
		userTransaction.begin();

		Verification verification = new Verification();
		verification.setElectionEventId(ELECTION_EVENT_ID);
		verification.setSignedVerificationPublicKey("");
		verification.setTenantId(TENANT_ID);
		verification.setVerificationCardId(VERIFICATION_CARD_ID);
		verification.setVerificationCardKeystore("");
		verification.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);

		entityManager.persist(verification);

		userTransaction.commit();
	}
}
