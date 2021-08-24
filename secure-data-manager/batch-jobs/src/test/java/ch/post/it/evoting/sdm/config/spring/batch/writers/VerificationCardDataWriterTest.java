/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.batch.item.ExecutionContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.domain.election.VerificationCardPublicKeyAndSignature;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardCredentialDataPack;
import ch.post.it.evoting.sdm.config.spring.batch.GeneratedVotingCardOutput;

public class VerificationCardDataWriterTest {

	private final String verificationCardId = "verificationCardId";
	private final String verificationCardSerializedKeyStoreB64 = "{}";
	private final String electionEventId = "electionEventId";
	private final String verificationCardSetId = "verificationCardSetId";
	private final byte[] randomByteArray = new byte[16];

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private ElGamalKeyPair elGamalKeyPair;

	private String tempBasePathPrefix;

	private Path tempBasePath;

	@Before
	public void setup() throws Exception {
		tempBasePathPrefix = tempFolder.newFile().toString();
		tempBasePath = Paths.get(tempBasePathPrefix + Constants.CSV);

		final BigInteger p = new BigInteger(
				"25515082852221325227734875679796454760326467690112538918409444238866830264288928368643860210692030230970372642053699673880830938513755311613746769767735066124931265104230246714327140720231537205767076779634365989939295710998787785801877310580401262530818848712843191597770750843711630250668056368624192328749556025449493888902777252341817892959006585132698115406972938429732386814317498812002229915393331703423250137659204137625584559844531972832055617091033311878843608854983169553055109029654797488332746885443611918764277292979134833642098989040604523427961162591459163821790507259475762650859921432844527734894939");
		final BigInteger q = new BigInteger(
				"12757541426110662613867437839898227380163233845056269459204722119433415132144464184321930105346015115485186321026849836940415469256877655806873384883867533062465632552115123357163570360115768602883538389817182994969647855499393892900938655290200631265409424356421595798885375421855815125334028184312096164374778012724746944451388626170908946479503292566349057703486469214866193407158749406001114957696665851711625068829602068812792279922265986416027808545516655939421804427491584776527554514827398744166373442721805959382138646489567416821049494520302261713980581295729581910895253629737881325429960716422263867447469");
		final BigInteger g = new BigInteger("3");

		final ElGamalEncryptionParameters elGamalEncryptionParameters = new ElGamalEncryptionParameters(p, q, g);
		elGamalKeyPair = new ElGamalService().generateKeyPair(elGamalEncryptionParameters, 1);

		new SecureRandom().nextBytes(randomByteArray);
	}

	@Test
	public void generateOutputFilesWithCorrectDataFormat() throws Exception {

		final int maxNumCredentialsPerFile = 3;
		final int numberCredentials = 10;
		final int numFiles = (numberCredentials + maxNumCredentialsPerFile - 1) / maxNumCredentialsPerFile;

		final VerificationCardDataWriter verificationCardDataWriter = new VerificationCardDataWriter(tempBasePath, maxNumCredentialsPerFile);

		verificationCardDataWriter.open(new ExecutionContext(Collections.emptyMap()));

		// for later verification
		final String signatureBase64 = Base64.getEncoder().encodeToString(randomByteArray);
		final VerificationCardPublicKeyAndSignature original = new VerificationCardPublicKeyAndSignature();
		final String verificationCardPublicKeyB64 = Base64.getEncoder()
				.encodeToString(elGamalKeyPair.getPublicKeys().toJson().getBytes(StandardCharsets.UTF_8));
		original.setPublicKey(verificationCardPublicKeyB64);
		original.setSignature(signatureBase64);

		final List<GeneratedVotingCardOutput> items = new ArrayList<>();
		items.add(createOutput());
		for (int i = 0; i < numberCredentials; i++) {
			verificationCardDataWriter.write(items);
		}

		final List<String> strings = new ArrayList<>();
		for (int i = 0; i < numFiles; i++) {
			final List<String> elemsFile = Files.readAllLines(Paths.get(tempBasePathPrefix + "." + i + Constants.CSV));
			strings.addAll(elemsFile);
			if (i == numFiles - 1) {
				assertEquals(numberCredentials - (numFiles - 1) * maxNumCredentialsPerFile, elemsFile.size());
			} else {
				assertEquals(maxNumCredentialsPerFile, elemsFile.size());
			}
		}

		final int expectedFormatLength = 5; // There are 5 parameters in the expected format.
		final ObjectMapper jsonMapper = new ObjectMapper();

		strings.forEach((String l) -> {
			final String[] columns = l.split(",");
			assertEquals(expectedFormatLength, columns.length);
			assertEquals(verificationCardId, columns[0]);
			assertEquals(verificationCardSerializedKeyStoreB64, columns[1]);

			try {
				final String json = new String(Base64.getDecoder().decode(columns[2]), StandardCharsets.UTF_8);
				final VerificationCardPublicKeyAndSignature deserialized = jsonMapper.readValue(json, VerificationCardPublicKeyAndSignature.class);

				assertEquals(original.getPublicKey(), deserialized.getPublicKey());
				assertEquals(original.getSignature(), deserialized.getSignature());

			} catch (IOException e) {
				fail("invalid format");
			}

			assertEquals(electionEventId, columns[3]);
			assertEquals(verificationCardSetId, columns[4]);
		});
	}

	private GeneratedVotingCardOutput createOutput() {

		final VerificationCardCredentialDataPack verificationCardCredentialDataPack = mock(VerificationCardCredentialDataPack.class);
		when(verificationCardCredentialDataPack.getSerializedKeyStore()).thenReturn("{}");
		when(verificationCardCredentialDataPack.getSignatureVCardPubKeyEEIDVCID()).thenReturn(randomByteArray);
		when(verificationCardCredentialDataPack.getVerificationCardKeyPair()).thenReturn(elGamalKeyPair);

		return GeneratedVotingCardOutput.success(null, null, null, null, null, electionEventId, verificationCardId, verificationCardSetId, null, null,
				verificationCardCredentialDataPack, null, null);
	}

}
