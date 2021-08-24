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

import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKey;

@RunWith(Arquillian.class)
public class ElectoralDataResourceTest extends VoteVerificationArquillianTest {

	private final String ADMIN_BOARD_ID = "5b4517223d0a40aa94badf6f1ad32152";
	private final String ELECTORAL_AUTHORITY_ID = "123456789";

	private final String ELECTION_PUBLIC_KEY_JWT = "first-saved-jwt";

	// Paths
	private final String SAVE_ELECTORAL_DATA_PATH = ElectoralDataResource.RESOURCE_PATH + ElectoralDataResource.SAVE_ELECTORAL_DATA_PATH;

	@Test
	public void testSaveElectoralData_Successful(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget) throws SecurityException, IllegalStateException {
		String url = addPathValues(SAVE_ELECTORAL_DATA_PATH, ElectoralDataResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				ElectoralDataResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				ElectoralDataResource.QUERY_PARAMETER_ELECTORAL_AUTHORITY_ID, ELECTORAL_AUTHORITY_ID,
				ElectoralDataResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		String entity = "{\"electionPublicKey\": { \"signature\":\"" + getElectoralAuthoritySignature() + "\" } }";

		Response response = webTarget.path(url).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));

		assertThat(response.getStatus(), is(STATUS_OK));

		ElectionPublicKey savedObject = getSavedObject(ElectionPublicKey.class, "electoralAuthorityId", ELECTORAL_AUTHORITY_ID);
		assertThat(savedObject.getElectionEventId(), is(ELECTION_EVENT_ID));
	}

	private String getElectoralAuthoritySignature() {
		ch.post.it.evoting.domain.election.ElectionPublicKey electionPublicKey = new ch.post.it.evoting.domain.election.ElectionPublicKey();
		electionPublicKey.setId("electoralAuthorityId");
		electionPublicKey.setPublicKey("electoralAuthorityPublicKey");
		return JsonSignatureService.sign(VoteVerificationArquillianDeployment.keyPair.getPrivate(), electionPublicKey);
	}

	@Test
	public void testSaveElectoralData_Duplicate(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws SecurityException, IllegalStateException, NotSupportedException, SystemException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		createDuplicateElectionPublicKeyEntity();

		String url = addPathValues(SAVE_ELECTORAL_DATA_PATH, ElectoralDataResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				ElectoralDataResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				ElectoralDataResource.QUERY_PARAMETER_ELECTORAL_AUTHORITY_ID, ELECTORAL_AUTHORITY_ID,
				ElectoralDataResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		String entity = "{\"electionPublicKey\": { \"signature\":\"" + getElectoralAuthoritySignature() + "\" } }";

		Response response = webTarget.path(url).request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));

		assertThat(response.getStatus(), is(STATUS_OK));

		ElectionPublicKey savedObject = getSavedObject(ElectionPublicKey.class, "electoralAuthorityId", ELECTORAL_AUTHORITY_ID);
		assertThat(savedObject.getElectionEventId(), is(ELECTION_EVENT_ID));
		assertThat(savedObject.getJwt(), is(ELECTION_PUBLIC_KEY_JWT));
	}

	private void createDuplicateElectionPublicKeyEntity()
			throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		userTransaction.begin();

		ElectionPublicKey entity = new ElectionPublicKey();
		entity.setElectionEventId(ELECTION_EVENT_ID);
		entity.setElectoralAuthorityId(ELECTORAL_AUTHORITY_ID);
		entity.setTenantId(TENANT_ID);
		entity.setJwt(ELECTION_PUBLIC_KEY_JWT);
		entityManager.persist(entity);

		userTransaction.commit();
	}

}
