/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.extendedauthentication.ws.application;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.StringJoiner;
import java.util.UUID;

import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIDerivedKey;
import ch.post.it.evoting.cryptolib.api.derivation.CryptoAPIPBKDFDeriver;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptoprimitives.CryptoPrimitives;
import ch.post.it.evoting.cryptoprimitives.CryptoPrimitivesService;
import ch.post.it.evoting.domain.election.model.Information.VoterInformation;
import ch.post.it.evoting.domain.election.model.authentication.AuthenticationToken;
import ch.post.it.evoting.votingserver.commons.sign.CSVSigner;
import ch.post.it.evoting.votingserver.commons.util.CryptoUtils;
import ch.post.it.evoting.votingserver.commons.util.DateUtils;
import ch.post.it.evoting.votingserver.extendedauthentication.ws.application.operation.ExtendedAuthenticationResource;

class ExtendedAuthenticationBaseITestCase {

	static final String ADMINBOARD_ID = "adminBoardId";
	static final String ELECTION_EVENT = "electionevent";
	static final String TEST_FILES_PATH = "src/test/resources/test_resources/";
	static final String FORBIDDEN = "FORBIDDEN";
	static final String NOT_FOUND = "NOT_FOUND";
	static final String UNAUTHORIZED = "UNAUTHORIZED";
	static final String PARAMETER_AUTHENTICATION_TOKEN = "authenticationToken";
	static final String TENANT_ID = "tenantId";
	static final String TEST_ADMINBOARD_ID = "1";
	static final String AUTHENTICATION_PATH = ExtendedAuthenticationResource.RESOURCE_PATH + ExtendedAuthenticationResource.AUTHENTICATE_PATH;
	static final String TEST_AUTH_ID = "f9949cfa3b1f5785cc48b9f879ebdec5";
	static final String TEST_BALLOT_ID = "thisistheballotid";
	static final String TEST_CREDENTIAL_ID = "thisisthecredentialid";
	static final String TEST_ELECTION_EVENT_ID = "electionEventId";
	static final String TEST_REQUEST_ID = "1";
	static final String TEST_TENANT_ID = "100";
	static final String TEST_VC_SET_ID = "thevotingcardsetid";
	static final String TEST_VERIFICATION_CARD_ID = "thisistheverificationcardid";
	static final String TEST_VERIFICATION_CARD_SET_ID = "thisistheverificationcardsetid";
	static final String TEST_VOTINGCARD_ID = "votingCardId";
	static final String MEDIATYPE_TEXT_CSV = "text/csv";

	private static final int MIN_EXTRA_PARAM_LENGTH = 16;
	private static final int MIN_SALT_LENGTH = 32;
	private static final String TEST_ENCRYPTED_SVK = "p5CA4ZqAWx4D/WCHCFLYLrwt1jS9v5pcApt8pSVEVUgXEsv71Gmu1gTMGHkfuSS/1PAtkoCsj4w=";

	static KeyPair keyPairForSigning;
	static String certificateString;

	private final PrimitivesServiceAPI primitivesService;
	private final CryptoPrimitives cryptoPrimitives;

	public ExtendedAuthenticationBaseITestCase() {
		primitivesService = new PrimitivesService();
		cryptoPrimitives = CryptoPrimitivesService.get();
	}

	static void createCryptoMaterial() throws Exception {

		keyPairForSigning = CryptoUtils.getKeyPairForSigning();
		final CryptoAPIX509Certificate certificate = CryptoUtils
				.createCryptoAPIx509Certificate(TEST_CREDENTIAL_ID, CertificateParameters.Type.SIGN, keyPairForSigning);
		certificateString = new String(certificate.getPemEncoded(), StandardCharsets.UTF_8);
	}

	AuthenticationToken createAndSignAuthenticationToken(final VoterInformation voterInformation, final PrivateKey privateKey) throws Exception {

		final String base64AuthenticationTokenId = cryptoPrimitives.genRandomBase64String(24);
		final String currentTimestamp = DateUtils.getTimestamp();
		final byte[] tokenSignature = CryptoUtils.sign(privateKey, base64AuthenticationTokenId, currentTimestamp, voterInformation.getTenantId(),
				voterInformation.getElectionEventId(), voterInformation.getVotingCardId(), voterInformation.getBallotId(),
				voterInformation.getCredentialId(), voterInformation.getVerificationCardId(), voterInformation.getBallotBoxId(),
				voterInformation.getVerificationCardSetId(), voterInformation.getVotingCardSetId());

		return new AuthenticationToken(voterInformation, base64AuthenticationTokenId, currentTimestamp,
				Base64.getEncoder().encodeToString(tokenSignature));

	}

	String createAndSignTestData(String electionId, String authId) {
		// csv fields: authId, extraParam, encryptedStartVotingKey,
		// electionEvent, salt, credentialId
		final String testData = new StringJoiner(",").add(authId).add("").add(TEST_ENCRYPTED_SVK).add(electionId).add("").add(TEST_CREDENTIAL_ID)
				.toString();

		return writeToFileAndSign(testData);
	}

	String createAndSignTestDataWithExtraParam(String electionId, String extraParam) throws Exception {

		final byte[] salt = primitivesService.genRandomBytes(MIN_SALT_LENGTH);
		final String b64Salt = Base64.getEncoder().encodeToString(salt);
		final byte[] saltedExtraParam = calculateHashFromDataAndSalt(extraParam, salt);
		final String saltedExtraParamString = Base64.getEncoder().encodeToString(saltedExtraParam);
		final String testData = new StringJoiner(",").add(TEST_AUTH_ID).add(saltedExtraParamString).add(TEST_ENCRYPTED_SVK).add(electionId)
				.add(b64Salt).add(TEST_CREDENTIAL_ID).toString();

		return writeToFileAndSign(testData);
	}

	private byte[] calculateHashFromDataAndSalt(String providedExtraParam, final byte[] salt) throws GeneralCryptoLibException {

		if (providedExtraParam == null || providedExtraParam.isEmpty()) {
			return new byte[0];
		}

		providedExtraParam = padExtraParameter(providedExtraParam);
		final CryptoAPIPBKDFDeriver derived = primitivesService.getPBKDFDeriver();
		final CryptoAPIDerivedKey cryptoAPIDerivedKeyPIN = derived.deriveKey(providedExtraParam.toCharArray(), salt);

		return cryptoAPIDerivedKeyPIN.getEncoded();
	}

	private String padExtraParameter(String providedExtraParam) {

		if (providedExtraParam.length() < MIN_EXTRA_PARAM_LENGTH) {
			providedExtraParam = leftPadding(providedExtraParam, MIN_EXTRA_PARAM_LENGTH);
		}

		return providedExtraParam;
	}

	private String leftPadding(final String s, final int n) {

		return String.format("%1$" + n + "s", s);
	}

	private String writeToFileAndSign(String serialized) {

		final String fileName = "target/" + UUID.randomUUID().toString() + ".csv";
		final Path csvFilePath = Paths.get(fileName);

		try (PrintWriter out = new PrintWriter(fileName)) {
			out.println(serialized);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		try {
			new CSVSigner().sign(keyPairForSigning.getPrivate(), csvFilePath);
			final String signedFile = new String(Files.readAllBytes(csvFilePath), StandardCharsets.UTF_8);
			Files.delete(csvFilePath);

			return signedFile;
		} catch (GeneralCryptoLibException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
