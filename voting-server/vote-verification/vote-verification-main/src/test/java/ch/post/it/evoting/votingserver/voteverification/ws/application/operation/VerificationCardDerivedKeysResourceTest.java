/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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

@RunWith(Arquillian.class)
public class VerificationCardDerivedKeysResourceTest extends VoteVerificationArquillianTest {

	private static final String TEST_DERIVED_KEYS_DATA_FILE_PATH = "derivedKeys.csv";
	private final String VERIFICATION_CARD_SET_ID = "bf2dbc45f7324320a5e0dc7f70a6aad4";
	private final String ADMIN_BOARD_ID = "5b4517223d0a40aa94badf6f1ad32152";
	private final String VERIFICATION_CARD_ID = "1234";
	private final String CCODE_DERIVED_KEY_COMMITMENT = "CCodeTest";
	private final String BCK_DERIVED_KEY_COMMITMENT = "BCK_Test";

	private final String SAVE_VERIFICATION_CARD_DERIVED_KEYS_PATH =
			VerificationCardDerivedKeysResource.RESOURCE_PATH + "/" + VerificationCardDerivedKeysResource.SAVE_VERIFICATION_DERIVED_KEYS_PATH;

	@Test
	public void testSaveVerificationCardDerivedKeys_Successful(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget) throws SecurityException, IllegalStateException, IOException, GeneralCryptoLibException {
		String url = addPathValues(SAVE_VERIFICATION_CARD_DERIVED_KEYS_PATH, VerificationCardDerivedKeysResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				VerificationCardDerivedKeysResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				VerificationCardDerivedKeysResource.QUERY_PARAMETER_VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_SET_ID,
				VerificationCardDerivedKeysResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		String verificationDataCsv = getMockVerificationDataCSV(1);

		MediaType mediaType = new MediaType("text", "csv");
		Response response = webTarget.path(url).request(mediaType).post(Entity.entity(verificationDataCsv, mediaType));

		assertThat(response.getStatus(), is(STATUS_OK));
	}

	@Test
	public void testSaveVerificationCardDerivedKeys_With_File_Successful(
			@ArquillianResteasyResource("")
			final ResteasyWebTarget webTarget) throws SecurityException, IllegalStateException, IOException, GeneralCryptoLibException {
		String url = addPathValues(SAVE_VERIFICATION_CARD_DERIVED_KEYS_PATH, VerificationCardDerivedKeysResource.QUERY_PARAMETER_TENANT_ID, TENANT_ID,
				VerificationCardDerivedKeysResource.QUERY_PARAMETER_ELECTION_EVENT_ID, ELECTION_EVENT_ID,
				VerificationCardDerivedKeysResource.QUERY_PARAMETER_VERIFICATION_CARD_SET_ID, VERIFICATION_CARD_SET_ID,
				VerificationCardDerivedKeysResource.QUERY_PARAMETER_ADMIN_BOARD_ID, ADMIN_BOARD_ID);

		String verificationDataCsv = getFileDataCSVContent();
		MediaType mediaType = new MediaType("text", "csv");
		Response response = webTarget.path(url).request(mediaType).post(Entity.entity(verificationDataCsv, mediaType));
		assertThat(response.getStatus(), is(STATUS_OK));
	}

	private String getMockVerificationDataCSV(int numberOfLines) throws IOException, GeneralCryptoLibException {
		String testCsvFileName = "verificationCardDerivedKeysTest.csv";

		String verificationCardDerivedKeysCsv = VERIFICATION_CARD_ID + ";" + CCODE_DERIVED_KEY_COMMITMENT + ";" + BCK_DERIVED_KEY_COMMITMENT;
		File testCsvFile = Files.createTempFile(testCsvFileName, null).toFile();

		List<String> lines = new ArrayList<>();
		for (int i = 0; i < numberOfLines; i++) {
			lines.add(verificationCardDerivedKeysCsv);
		}
		FileUtils.writeLines(testCsvFile, lines);

		new CSVSigner().sign(VoteVerificationArquillianDeployment.keyPair.getPrivate(), testCsvFile.toPath());
		String csvFileContent = FileUtils.readFileToString(testCsvFile);

		return csvFileContent;
	}

	private String getFileDataCSVContent() throws IOException, GeneralCryptoLibException {
		File testCsvFile = new File(URLDecoder
				.decode(this.getClass().getClassLoader().getResource(TEST_DERIVED_KEYS_DATA_FILE_PATH).getPath(), StandardCharsets.UTF_8.toString()));
		new CSVSigner().sign(VoteVerificationArquillianDeployment.keyPair.getPrivate(), testCsvFile.toPath());
		return FileUtils.readFileToString(testCsvFile);
	}
}
