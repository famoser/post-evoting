/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import static org.hamcrest.MatcherAssert.assertThat;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.votingserver.commons.sign.CSVSigner;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;

@RunWith(Arquillian.class)
public class VerificationCardDataResourceTest extends VoteVerificationArquillianTest {

	private final String VERIFICATION_CARD_SET_ID = "bf2dbc45f7324320a5e0dc7f70a6aad4";
	private final String ADMIN_BOARD_ID = "5b4517223d0a40aa94badf6f1ad32152";
	private final String VERIFICATION_CARD_ID = "123";
	private final String VERIFICATION_FIRST_SAVED = "first-saved-verification";

	private final String SAVE_VERIFICATION_CARD_DATA_PATH =
			VerificationCardDataResource.RESOURCE_PATH + VerificationCardDataResource.SAVE_VERIFICATION_CARD_DATA_PATH;

	@Test
	public void testSaveVerificationCardData_Successful(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException,
			NotSupportedException, IOException, GeneralCryptoLibException {
		String url = addPathValues(SAVE_VERIFICATION_CARD_DATA_PATH, VerificationCardDataResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				VerificationCardDataResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				VerificationCardDataResource.QUERY_PARAMETER_VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_SET_ID,
				VerificationCardDataResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		String verificationDataCsv = getMockVerificationDataCSV();

		MediaType mediaType = new MediaType("text", "csv");
		Response response = webTarget.path(url).request(mediaType).post(Entity.entity(verificationDataCsv, mediaType));

		assertThat(response.getStatus(), is(STATUS_OK));
	}

	@Test
	public void testSaveVerificationCardData_Duplicate(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget)
			throws SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, SystemException,
			NotSupportedException, IOException, GeneralCryptoLibException {
		saveDuplicateVerificationCardData();

		String url = addPathValues(SAVE_VERIFICATION_CARD_DATA_PATH, VerificationCardDataResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				VerificationCardDataResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				VerificationCardDataResource.QUERY_PARAMETER_VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_SET_ID,
				VerificationCardDataResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		String verificationDataCsv = getMockVerificationDataCSV();

		MediaType mediaType = new MediaType("text", "csv");
		Response response = webTarget.path(url).request(mediaType).post(Entity.entity(verificationDataCsv, mediaType));

		assertThat(response.getStatus(), is(STATUS_OK));
		Verification savedVerification = getSavedObject(Verification.class, "verificationCardId", VERIFICATION_CARD_ID);
		assertThat(savedVerification.getSignedVerificationPublicKey(), is(VERIFICATION_FIRST_SAVED));
	}

	private void saveDuplicateVerificationCardData()
			throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		userTransaction.begin();

		Verification verification = new Verification();
		verification.setElectionEventId(ELECTION_EVENT_ID);
		verification.setSignedVerificationPublicKey(VERIFICATION_FIRST_SAVED);
		verification.setTenantId(TENANT_ID);
		verification.setVerificationCardId(VERIFICATION_CARD_ID);
		verification.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);
		entityManager.persist(verification);

		userTransaction.commit();
	}

	private String getMockVerificationDataCSV() throws IOException, GeneralCryptoLibException {
		String testCsvFileName = "verificationCardDataTest.csv";

		String verificationDataCsv =
				VERIFICATION_CARD_ID + ",verificationCardKeystore_test,signedVerificationPublicKey_test," + ELECTION_EVENT_ID + ","
						+ VERIFICATION_CARD_SET_ID;
		File testCsvFile = Files.createTempFile(testCsvFileName, null).toFile();
		FileUtils.writeStringToFile(testCsvFile, verificationDataCsv);

		new CSVSigner().sign(VoteVerificationArquillianDeployment.keyPair.getPrivate(), testCsvFile.toPath());
		String csvFileContent = FileUtils.readFileToString(testCsvFile);

		return csvFileContent;
	}
}
