/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import static org.hamcrest.core.Is.is;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.returncodes.ChoiceCodeAndComputeResults;
import ch.post.it.evoting.domain.returncodes.VoteAndComputeResults;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;

@RunWith(Arquillian.class)
public class ChoiceCodesResourceTest extends VoteVerificationArquillianTest {

	private static final String VERIFICATION_CARD_ID = "123";

	private static final String GENERATE_CHOICE_CODES_PATH = ChoiceCodeResource.RESOURCE_PATH + ChoiceCodeResource.GENERATE_CHOICE_CODES_PATH;

	@Test
	public void testGenerateChoiceCodes_Successful(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws JsonProcessingException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException, SystemException, NotSupportedException, ResourceNotFoundException, CryptographicOperationException {
		VoteAndComputeResults voteAndComputeResults = prepareBodyEntity();

		String url = addPathValues(GENERATE_CHOICE_CODES_PATH, ChoiceCodeResource.PARAMETER_VALUE_TENANT_ID, TENANT_ID,
				ChoiceCodeResource.PARAMETER_VALUE_ELECTION_EVENT_ID, ELECTION_EVENT_ID, ChoiceCodeResource.PARAMETER_VALUE_VERIFICATION_CARD_ID,
				VERIFICATION_CARD_ID);

		Response response = webTarget.path(url).request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(voteAndComputeResults, MediaType.APPLICATION_JSON_TYPE));

		ChoiceCodeAndComputeResults choiceCodeResult = response.readEntity(ChoiceCodeAndComputeResults.class);

		Assert.assertThat(response.getStatus(), is(STATUS_OK));
		Assert.assertThat(choiceCodeResult.getChoiceCodes(), is(CHOICE_CODE_VALUE));
	}

	private VoteAndComputeResults prepareBodyEntity() {
		Vote vote = new Vote();
		vote.setTenantId(ELECTION_EVENT_ID);
		vote.setElectionEventId(ELECTION_EVENT_ID);
		vote.setBallotId(ELECTION_EVENT_ID);
		vote.setBallotBoxId(ELECTION_EVENT_ID);
		vote.setVotingCardId(ELECTION_EVENT_ID);
		vote.setEncryptedOptions(ELECTION_EVENT_ID);
		vote.setCorrectnessIds(ELECTION_EVENT_ID);
		vote.setVerificationCardPublicKey(ELECTION_EVENT_ID);
		vote.setCredentialId(ELECTION_EVENT_ID);

		VoteAndComputeResults voteAndComputeResults = new VoteAndComputeResults();
		voteAndComputeResults.setVote(vote);
		return voteAndComputeResults;
	}
}
