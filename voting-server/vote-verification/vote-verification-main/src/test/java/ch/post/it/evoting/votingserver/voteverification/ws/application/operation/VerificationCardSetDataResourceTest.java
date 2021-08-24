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
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.VerificationCardSetData;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.votingserver.voteverification.domain.common.SignedObject;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContent;

@RunWith(Arquillian.class)
public class VerificationCardSetDataResourceTest extends VoteVerificationArquillianTest {

	// Constanst for Save Verification Data
	private final String VERIFICATION_CARD_SET_ID = "4953a0f1fd4448c0a9b9494ce3348056";
	private final String ADMIN_BOARD_ID = "5b4517223d0a40aa94badf6f1ad32152";
	private final String VERIFICATION_CONTENT_DUPLICATE = "duplicate-save";

	// Paths
	private final String SAVE_VERIFICATION_CARD_SET_DATA_PATH =
			VerificationCardSetDataResource.RESOURCE_PATH + VerificationCardSetDataResource.SAVE_VERIFICATION_CARD_SET_DATA_PATH;

	@Test
	public void testSaveVerificationCardSetData_Successful(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget) throws JsonProcessingException, SecurityException, IllegalStateException {
		ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationCardSetData verificationCardSetData = new ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationCardSetData();
		verificationCardSetData.setVerificationCardSetData(generateVerificationCardSetJsonMockSignature());
		verificationCardSetData.setVoteVerificationContextData(generateVoteVerificationContextJsonMockSignature());

		String url = addPathValues(SAVE_VERIFICATION_CARD_SET_DATA_PATH, VerificationCardSetDataResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				VerificationCardSetDataResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				VerificationCardSetDataResource.QUERY_PARAMETER_VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_SET_ID,
				VerificationCardSetDataResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		Response response = webTarget.path(url).request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(verificationCardSetData, MediaType.APPLICATION_JSON_TYPE));
		assertThat(response.getStatus(), is(STATUS_OK));

		VerificationContent savedVerificationContent = getSavedObject(VerificationContent.class, "verificationCardSetId", VERIFICATION_CARD_SET_ID);
		assertThat(savedVerificationContent.getElectionEventId(), is(ELECTION_EVENT_ID));
		assertThat(savedVerificationContent.getTenantId(), is(TENANT_ID));
	}

	@Test
	public void testSaveVerificationCardSetData_Duplicate(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws JsonProcessingException, SecurityException, IllegalStateException, NotSupportedException, SystemException, RollbackException,
			HeuristicMixedException, HeuristicRollbackException {
		saveDuplicateVerificationContent();

		ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationCardSetData verificationCardSetData = new ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationCardSetData();
		verificationCardSetData.setVerificationCardSetData(generateVerificationCardSetJsonMockSignature());
		verificationCardSetData.setVoteVerificationContextData(generateVoteVerificationContextJsonMockSignature());

		String url = addPathValues(SAVE_VERIFICATION_CARD_SET_DATA_PATH, VerificationCardSetDataResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				VerificationCardSetDataResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				VerificationCardSetDataResource.QUERY_PARAMETER_VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_SET_ID,
				VerificationCardSetDataResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		Response response = webTarget.path(url).request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.entity(verificationCardSetData, MediaType.APPLICATION_JSON_TYPE));
		assertThat(response.getStatus(), is(STATUS_OK));

		VerificationContent savedVerificationContent = getSavedObject(VerificationContent.class, "verificationCardSetId", VERIFICATION_CARD_SET_ID);
		assertThat(savedVerificationContent.getElectionEventId(), is(ELECTION_EVENT_ID));
		assertThat(savedVerificationContent.getTenantId(), is(TENANT_ID));
		assertThat(savedVerificationContent.getJson(), is(VERIFICATION_CONTENT_DUPLICATE));
	}

	private void saveDuplicateVerificationContent()
			throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		userTransaction.begin();

		VerificationContent verificationContent = new VerificationContent();
		verificationContent.setElectionEventId(ELECTION_EVENT_ID);
		verificationContent.setTenantId(TENANT_ID);
		verificationContent.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);
		verificationContent.setJson(VERIFICATION_CONTENT_DUPLICATE);
		entityManager.persist(verificationContent);

		userTransaction.commit();
	}

	private String generateVerificationCardSetJsonMockSignature() throws JsonProcessingException {
		VerificationCardSetData verificationCardSetData = new VerificationCardSetData();
		verificationCardSetData.setChoicesCodesEncryptionPublicKey("testChoiceCodesEncryptionPublicKey");
		verificationCardSetData.setElectionEventId(ELECTION_EVENT_ID);
		verificationCardSetData.setVerificationCardSetIssuerCert("testVerificationCardOssieCert");
		verificationCardSetData.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);

		String signedVerificationCardSetDataJson = JsonSignatureService
				.sign(VoteVerificationArquillianDeployment.keyPair.getPrivate(), verificationCardSetData);

		SignedObject result = new SignedObject();
		result.setSignature(signedVerificationCardSetDataJson);

		return ObjectMappers.toJson(result);
	}

	private String generateVoteVerificationContextJsonMockSignature() throws JsonProcessingException {
		VoteVerificationContextData voteVerificationContextData = new VoteVerificationContextData();

		String signedVerificationContextDataJson = JsonSignatureService
				.sign(VoteVerificationArquillianDeployment.keyPair.getPrivate(), voteVerificationContextData);

		SignedObject result = new SignedObject();
		result.setSignature(signedVerificationContextDataJson);

		return ObjectMappers.toJson(result);
	}

}
