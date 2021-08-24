/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.FileUtils;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.votingserver.commons.sign.CSVSigner;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.CodesMapping;

@RunWith(Arquillian.class)
public class CodesMappingDataResourceTest extends VoteVerificationArquillianTest {

	private final String VERIFICATION_CARD_SET_ID = "34567890";
	private final String VERIFICATION_CARD_ID = "111111";
	private final String ADMIN_BOARD_ID = "5b4517223d0a40aa94badf6f1ad32152";
	private final String CODES_MAPPING_JSON_DUPLICATE = "duplicate-saved-json";
	private final String CODES_MAPPING_JSON = "mock-json";

	private final String SAVE_CODES_MAPPING_DATA_PATH =
			CodesMappingDataResource.RESOURCE_PATH + CodesMappingDataResource.SAVE_CODES_MAPPING_DATA_PATH;

	@Test
	public void testSaveCodesMappingData_Successful(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException,
			NotSupportedException, IOException, GeneralCryptoLibException {
		String url = addPathValues(SAVE_CODES_MAPPING_DATA_PATH, CodesMappingDataResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				CodesMappingDataResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				CodesMappingDataResource.QUERY_PARAMETER_VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_SET_ID,
				CodesMappingDataResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		String codesMappingCsv = getMockCodesMappingDataCSV();

		MediaType csvMediaType = new MediaType("text", "csv");
		Response response = webTarget.path(url).request(csvMediaType).post(Entity.entity(codesMappingCsv, csvMediaType));

		Assert.assertThat(response.getStatus(), is(STATUS_OK));

		CodesMapping savedCodesMapping = getSavedObject(CodesMapping.class, "electionEventId", ELECTION_EVENT_ID);
		Assert.assertThat(savedCodesMapping.getVerificationCardId(), is(VERIFICATION_CARD_ID));
		Assert.assertThat(savedCodesMapping.getElectionEventId(), is(ELECTION_EVENT_ID));
		Assert.assertThat(savedCodesMapping.getJson(), is(CODES_MAPPING_JSON));
	}

	@Test
	public void testSaveCodesMappingData_Duplicate(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException,
			NotSupportedException, IOException, GeneralCryptoLibException {
		saveCodesMappingDuplicate();

		String url = addPathValues(SAVE_CODES_MAPPING_DATA_PATH, CodesMappingDataResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				CodesMappingDataResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				CodesMappingDataResource.QUERY_PARAMETER_VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_SET_ID,
				CodesMappingDataResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		String codesMappingCsv = getMockCodesMappingDataCSV();

		MediaType csvMediaType = new MediaType("text", "csv");
		Response response = webTarget.path(url).request(csvMediaType).post(Entity.entity(codesMappingCsv, csvMediaType));

		Assert.assertThat(response.getStatus(), is(STATUS_OK));

		CodesMapping savedCodesMapping = getSavedObject(CodesMapping.class, "electionEventId", ELECTION_EVENT_ID);
		Assert.assertThat(savedCodesMapping.getVerificationCardId(), is(VERIFICATION_CARD_ID));
		Assert.assertThat(savedCodesMapping.getElectionEventId(), is(ELECTION_EVENT_ID));
		Assert.assertThat(savedCodesMapping.getJson(), is(CODES_MAPPING_JSON_DUPLICATE));
	}

	@Test
	public void testSaveCodesMappingData_InvalidParams(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException,
			NotSupportedException, IOException, GeneralCryptoLibException {
		String url = addPathValues(SAVE_CODES_MAPPING_DATA_PATH, CodesMappingDataResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				CodesMappingDataResource.QUERY_PARAMETER_ELECTION_EVENT_ID, "5", CodesMappingDataResource.QUERY_PARAMETER_VERIFICATION_CARD_SET_ID,
				VERIFICATION_CARD_SET_ID, CodesMappingDataResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		MediaType csvMediaType = new MediaType("text", "csv");
		Response response = webTarget.path(url).request(csvMediaType).post(Entity.entity("", csvMediaType));

		Assert.assertThat(response.getStatus(), is(500));
	}

	private void saveCodesMappingDuplicate()
			throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		userTransaction.begin();

		CodesMapping codeMapping = new CodesMapping();
		codeMapping.setElectionEventId(ELECTION_EVENT_ID);
		codeMapping.setTenantId(TENANT_ID);
		codeMapping.setVerificationCardId(VERIFICATION_CARD_ID);
		codeMapping.setJson(CODES_MAPPING_JSON_DUPLICATE);
		entityManager.persist(codeMapping);

		userTransaction.commit();
	}

	private String getMockCodesMappingDataCSV() throws IOException, GeneralCryptoLibException {
		String testCsvFileName = "codesMappingTest.csv";

		String codeMapping = VERIFICATION_CARD_ID + "," + CODES_MAPPING_JSON;
		File testCsvFile = Files.createTempFile(testCsvFileName, null).toFile();
		FileUtils.writeStringToFile(testCsvFile, codeMapping);

		new CSVSigner().sign(VoteVerificationArquillianDeployment.keyPair.getPrivate(), testCsvFile.toPath());
		String csvFileContent = FileUtils.readFileToString(testCsvFile);

		return csvFileContent;
	}

}


