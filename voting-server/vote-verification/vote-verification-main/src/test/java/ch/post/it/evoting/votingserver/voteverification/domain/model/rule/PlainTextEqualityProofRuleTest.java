/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKey;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKeyRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContent;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContentRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence.ElectionPublicKeyRepositoryDecorator;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence.VerificationContentRepositoryDecorator;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence.VerificationRepositoryDecorator;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.persistence.VerificationSetRepositoryDecorator;

@RunWith(MockitoJUnitRunner.class)
public class PlainTextEqualityProofRuleTest {

	private static final String VERIFICATION_CHOICE_CODES_PUBLIC_KEY = "eyJwdWJsaWNLZXkiOnsienBTdWJncm91cCI6eyJwIjoiQVBPZU5GbmI3SFNoYlNDUnEyVmt6cFVzY3RBdmE2ODBGYkIzU2hlSFFjWWIiLCJxIjoiZWM4YUxPMzJPbEMya0VqVnNySm5TcFk1YUJlMTE1b0syRHVsQzhPZzR3MD0iLCJnIjoiQXc9PSJ9LCJlbGVtZW50cyI6WyJBT3h0TXZqVjFpb2U5ZDRndVBoTXdibjdPUkVXQ3JhNVRoMlZvQ1plVm9ZcyIsIkFLUnVZT2N0bFE1VHFjLzdaNlZZaGdXZFdCM0FCVWI0cjJvVnpLQmJwcHN5IiwiTC8vcGFnM09PSEpyZE1VUTc3RjhUSm1lTnVpell0M3VNcGw0L0RBRkwwND0iLCJBTHl6YU44SFAwU3JDRksraTdhN1dvTVVtRnFFRE5MQ1ZKYmJzQkZrKytReSIsIkFMU3ZjZlRpWFhmVDFpbC9iUnNJMHJadlU2RUd5TWlpZGpQWlNnVE5ZYUxpIiwiQU1VNmxNbmlqS243SnhqVnAraXIvZFFUbk1LQjBVVllhYU9xRElVOE1KUVIiLCJBTnlQYlIvNnNzYmRTaXNYOWVveE42RHNLdTRiNk43Q0dCQmFIalhOamFybCIsIkFwQXRoc2ZpdXRZMXNGWHNEd3k3LzllU2trbU1WNmJLaThJRE9KL2xsT1k9IiwiWHp1dEU0SWJxdVFnY1pORm9POUtpLzhubmNaQjI5aFVGUjk4M29mT0JXYz0iLCJVWlNHbTNDdGVYSjJWNkFzdEt6bW12c25ESy9qRE1LTSt3QVE1dGFISWVJPSIsIkJ6TFRxdFNwL1MzUHFkc20vOGJkRTlTOUF1eGlTdkNLdFFERGhuMTJndUE9IiwiQUpmQzVRZnBhelVCdCtXNFF5VUJIV3Y2Vlg5S2cvaXY3Qm81N1RnRTVmRlgiLCJWci8wbysyNVhianNTTjgwUTgxd2dLYnI0ZmVuVWgyU21uUit6Mk9kQmZjPSIsIkFLYTJkeXlxODI4b2dVTTdxL0hlWm5VLzVxSFdITVVPZ0JRRlR2cFRORnJiIiwiQUxiZ2NXYnZVUzJDb216SnpwTEdjNzNqcE9GbFZDUlR4ZXVnLytITGpERFMiXX19";

	private static final String ELECTION_PUBLIC_KEY_BASE64_1_ELEMENT = "eyJwdWJsaWNLZXkiOnsienBTdWJncm91cCI6eyJxIjoiZWM4YUxPMzJPbEMya0VqVnNySm5TcFk1YUJlMTE1b0syRHVsQzhPZzR3MD0iLCJnIjoiQXc9PSIsInAiOiJBUE9lTkZuYjdIU2hiU0NScTJWa3pwVXNjdEF2YTY4MEZiQjNTaGVIUWNZYiJ9LCJlbGVtZW50cyI6WyJBTDdzQlg0bmtVbnJ5Y3dBcXlRTHhFRkhmeWVlMDRYTnJQMWo1ZzZWYkhZVSJdfX0=";

	private static final String ELECTION_PUBLIC_KEY_BASE64_2_ELEMENTS = "eyJwdWJsaWNLZXkiOnsienBTdWJncm91cCI6eyJxIjoiZWM4YUxPMzJPbEMya0VqVnNySm5TcFk1YUJlMTE1b0syRHVsQzhPZzR3MD0iLCJnIjoiQXc9PSIsInAiOiJBUE9lTkZuYjdIU2hiU0NScTJWa3pwVXNjdEF2YTY4MEZiQjNTaGVIUWNZYiJ9LCJlbGVtZW50cyI6WyJBTDdzQlg0bmtVbnJ5Y3dBcXlRTHhFRkhmeWVlMDRYTnJQMWo1ZzZWYkhZVSIsIkFMN3NCWDRua1Vucnljd0FxeVFMeEVGSGZ5ZWUwNFhOclAxajVnNlZiSFlVIl19fQ==";

	private static final String ELECTION_PUBLIC_KEY_BASE64_3_ELEMENTS = "eyJwdWJsaWNLZXkiOnsienBTdWJncm91cCI6eyJxIjoiZWM4YUxPMzJPbEMya0VqVnNySm5TcFk1YUJlMTE1b0syRHVsQzhPZzR3MD0iLCJnIjoiQXc9PSIsInAiOiJBUE9lTkZuYjdIU2hiU0NScTJWa3pwVXNjdEF2YTY4MEZiQjNTaGVIUWNZYiJ9LCJlbGVtZW50cyI6WyJBTDdzQlg0bmtVbnJ5Y3dBcXlRTHhFRkhmeWVlMDRYTnJQMWo1ZzZWYkhZVSIsIkFMN3NCWDRua1Vucnljd0FxeVFMeEVGSGZ5ZWUwNFhOclAxajVnNlZiSFlVIiwiQUw3c0JYNG5rVW5yeWN3QXF5UUx4RUZIZnllZTA0WE5yUDFqNWc2VmJIWVUiXX19";

	private static final String ELECTORAL_AUTHORITY_ID = "ac20b274bf48423d818c811c1da0713e";

	private static final String VERIFICATION_SET_SIGNATURE = "20";

	private static final String SIGNED_VERIFICATION_PUBLIC_KEY = "16";

	private static final String VERIFICATION_CARD_KEYSTORE = "17";

	private static final String CORRECTNESS_IDS = "1";

	private static final String ELECTION_EVENT_ID = "2";

	private static final String ENCRYPTED_OPTIONS = "3";

	private static final String EXPONENTIATION_PROOF = "5";

	private static final String TENANT_ID = "8";

	private static final String VERIFICATION_CARD_ID = "9";

	private static final String VERIFICATION_CARD_PK_SIGNATURE = "10";

	private static final String VERIFICATION_CARD_PUBLIC_KEY = "11";

	private static final String VERIFICATION_CARD_SET_ID = "12";

	private static final String VOTING_CARD_ID = "13";

	private static final String AUTHENTICATION_TOKEN = "14";

	private static final String CIPHER_TEXT_EXPONENTIATIONS = "3;4";

	private static final String ENCRYPTED_PARTIAL_CHOICE_CODES = "2;6";

	private static ResourceNotFoundException resourceNotFoundException;

	@InjectMocks
	private final VerificationSetRepository verificationSetRepositoryDecorator = new VerificationSetRepositoryDecorator() {

		@Override
		public VerificationSetEntity update(VerificationSetEntity entity) {
			return null;
		}

		@Override
		public VerificationSetEntity find(Integer id) {
			return null;
		}
	};

	@InjectMocks
	private final VerificationRepository verificationRepositoryDecorator = new VerificationRepositoryDecorator() {

		@Override
		public Verification update(Verification entity) {
			return null;
		}

		@Override
		public Verification find(Integer id) {
			return null;
		}
	};

	@InjectMocks
	private final VerificationContentRepository verificationContentRepositoryDecorator = new VerificationContentRepositoryDecorator() {

		@Override
		public VerificationContent update(VerificationContent entity) {
			return null;
		}
	};

	@InjectMocks
	private final ElectionPublicKeyRepository electionPublicKeyRepositoryDecorator = new ElectionPublicKeyRepositoryDecorator() {

		@Override
		public ElectionPublicKey update(ElectionPublicKey entity) {
			return null;
		}

		@Override
		public ElectionPublicKey find(Integer id) {
			return null;
		}
	};

	@InjectMocks
	private final PlainTextEqualityProofRule plainTextEqualityProofRule = new PlainTextEqualityProofRule();

	@InjectMocks
	private final PlainTextEqualityProofRuleDecorator plainTextEqualityProofRuleDecorator = new PlainTextEqualityProofRuleDecorator();

	@Mock
	private VerificationSetRepository verificationSetRepository;

	@Mock
	private VerificationRepository verificationRepository;

	@Mock
	private VerificationContentRepository verificationContentRepository;

	@Mock
	private ElectionPublicKeyRepository electionPublicKeyRepository;

	@Mock
	private ProofsServiceAPI proofsService;

	@BeforeClass
	public static void init() {
		resourceNotFoundException = Mockito.mock(ResourceNotFoundException.class);
	}

	@Before
	public void setUp() {
		plainTextEqualityProofRuleDecorator.plainTextEqualityProofRule = plainTextEqualityProofRule;
		plainTextEqualityProofRuleDecorator.plainTextEqualityProofRule.electionPublicKeyRepository = electionPublicKeyRepositoryDecorator;
		plainTextEqualityProofRuleDecorator.plainTextEqualityProofRule.verificationContentRepository = verificationContentRepositoryDecorator;
		plainTextEqualityProofRuleDecorator.plainTextEqualityProofRule.verificationRepository = verificationRepositoryDecorator;
		plainTextEqualityProofRuleDecorator.plainTextEqualityProofRule.verificationSetRepository = verificationSetRepositoryDecorator;
	}

	@Test
	public void testVerification_1_KeyElement_Successful() throws ResourceNotFoundException, GeneralCryptoLibException {
		Vote vote = prepareMockVote();
		Verification verification = prepareMockVerification();
		VerificationContent verificationContent = prepareVerificationContentMock();
		VerificationSetEntity verificationSetEntity = prepareVerificationSetMock();
		ElectionPublicKey electionPublicKey = prepareElectionPublicKeyMock(ELECTION_PUBLIC_KEY_BASE64_1_ELEMENT);

		ProofVerifierAPI proofVerifier = Mockito.mock(ProofVerifierAPI.class);

		Mockito.when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenReturn(verification);
		Mockito.when(verificationContentRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationContent);
		Mockito.when(
				verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationSetEntity);
		Mockito.when(
				electionPublicKeyRepository.findByTenantIdElectionEventIdElectoralAuthorityId(TENANT_ID, ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID))
				.thenReturn(electionPublicKey);
		Mockito.when(proofsService.createProofVerifierAPI(any())).thenReturn(proofVerifier);
		Mockito.when(proofVerifier
				.verifyPlaintextEqualityProof(any(Ciphertext.class), any(ElGamalPublicKey.class), any(Ciphertext.class), any(ElGamalPublicKey.class),
						any(Proof.class))).thenReturn(true);

		ValidationError result = plainTextEqualityProofRuleDecorator.execute(vote);

		assertEquals(ValidationErrorType.SUCCESS, result.getValidationErrorType());
	}

	@Test
	public void testVerification_2_KeyElement_Successful() throws ResourceNotFoundException, GeneralCryptoLibException {
		Vote vote = prepareMockVote();
		Verification verification = prepareMockVerification();
		VerificationContent verificationContent = prepareVerificationContentMock();
		VerificationSetEntity verificationSetEntity = prepareVerificationSetMock();
		ElectionPublicKey electionPublicKey = prepareElectionPublicKeyMock(ELECTION_PUBLIC_KEY_BASE64_2_ELEMENTS);

		ProofVerifierAPI proofVerifier = Mockito.mock(ProofVerifierAPI.class);

		Mockito.when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenReturn(verification);
		Mockito.when(verificationContentRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationContent);
		Mockito.when(
				verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationSetEntity);
		Mockito.when(
				electionPublicKeyRepository.findByTenantIdElectionEventIdElectoralAuthorityId(TENANT_ID, ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID))
				.thenReturn(electionPublicKey);
		Mockito.when(proofsService.createProofVerifierAPI(any())).thenReturn(proofVerifier);
		Mockito.when(proofVerifier
				.verifyPlaintextEqualityProof(any(Ciphertext.class), any(ElGamalPublicKey.class), any(Ciphertext.class), any(ElGamalPublicKey.class),
						any(Proof.class))).thenReturn(true);

		ValidationError result = plainTextEqualityProofRuleDecorator.execute(vote);

		assertEquals(ValidationErrorType.SUCCESS, result.getValidationErrorType());
	}

	@Test
	public void testVerification_3_KeyElement_Success() throws ResourceNotFoundException, GeneralCryptoLibException {
		Vote vote = prepareMockVote();
		Verification verification = prepareMockVerification();
		VerificationContent verificationContent = prepareVerificationContentMock();
		VerificationSetEntity verificationSetEntity = prepareVerificationSetMock();
		ElectionPublicKey electionPublicKey = prepareElectionPublicKeyMock(ELECTION_PUBLIC_KEY_BASE64_3_ELEMENTS);

		ProofVerifierAPI proofVerifier = Mockito.mock(ProofVerifierAPI.class);

		Mockito.when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenReturn(verification);
		Mockito.when(verificationContentRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationContent);
		Mockito.when(
				verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationSetEntity);
		Mockito.when(
				electionPublicKeyRepository.findByTenantIdElectionEventIdElectoralAuthorityId(TENANT_ID, ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID))
				.thenReturn(electionPublicKey);
		Mockito.when(proofsService.createProofVerifierAPI(any())).thenReturn(proofVerifier);
		Mockito.when(proofVerifier
				.verifyPlaintextEqualityProof(any(Ciphertext.class), any(ElGamalPublicKey.class), any(Ciphertext.class), any(ElGamalPublicKey.class),
						any(Proof.class))).thenReturn(true);

		ValidationError result = plainTextEqualityProofRuleDecorator.execute(vote);

		assertEquals(ValidationErrorType.SUCCESS, result.getValidationErrorType());
	}

	@Test
	public void testVerification_Failed() throws ResourceNotFoundException, GeneralCryptoLibException {
		Vote vote = prepareMockVote();
		Verification verification = prepareMockVerification();
		VerificationContent verificationContent = prepareVerificationContentMock();
		VerificationSetEntity verificationSetEntity = prepareVerificationSetMock();
		ElectionPublicKey electionPublicKey = prepareElectionPublicKeyMock(ELECTION_PUBLIC_KEY_BASE64_1_ELEMENT);

		ProofVerifierAPI proofVerifier = Mockito.mock(ProofVerifierAPI.class);

		Mockito.when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenReturn(verification);
		Mockito.when(verificationContentRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationContent);
		Mockito.when(
				verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationSetEntity);
		Mockito.when(
				electionPublicKeyRepository.findByTenantIdElectionEventIdElectoralAuthorityId(TENANT_ID, ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID))
				.thenReturn(electionPublicKey);
		Mockito.when(proofsService.createProofVerifierAPI(any())).thenReturn(proofVerifier);
		Mockito.when(proofVerifier
				.verifyPlaintextEqualityProof(any(Ciphertext.class), any(ElGamalPublicKey.class), any(Ciphertext.class), any(ElGamalPublicKey.class),
						any(Proof.class))).thenReturn(false);

		ValidationError result = plainTextEqualityProofRuleDecorator.execute(vote);

		assertEquals(ValidationErrorType.FAILED, result.getValidationErrorType());
	}

	@Test
	public void testVerification_VerificationNotFound() throws GeneralCryptoLibException, ResourceNotFoundException {
		Vote vote = prepareMockVote();

		Mockito.when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenThrow(resourceNotFoundException);

		ValidationError result = plainTextEqualityProofRuleDecorator.execute(vote);

		assertEquals(ValidationErrorType.FAILED, result.getValidationErrorType());
	}

	@Test
	public void testVerification_VerificationContentNotFound() throws ResourceNotFoundException, GeneralCryptoLibException {
		Vote vote = prepareMockVote();
		Verification verification = prepareMockVerification();

		Mockito.when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenReturn(verification);
		Mockito.when(verificationContentRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenThrow(resourceNotFoundException);

		ValidationError result = plainTextEqualityProofRuleDecorator.execute(vote);

		assertEquals(ValidationErrorType.FAILED, result.getValidationErrorType());
	}

	@Test
	public void testVerification_VerificationSetNotFound() throws ResourceNotFoundException, GeneralCryptoLibException {
		Vote vote = prepareMockVote();
		Verification verification = prepareMockVerification();
		VerificationContent verificationContent = prepareVerificationContentMock();

		Mockito.when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenReturn(verification);
		Mockito.when(verificationContentRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationContent);
		Mockito.when(
				verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenThrow(resourceNotFoundException);

		ValidationError result = plainTextEqualityProofRuleDecorator.execute(vote);

		assertEquals(ValidationErrorType.FAILED, result.getValidationErrorType());
	}

	@Test
	public void testVerification_ElectoralAuthorityNotFound() throws ResourceNotFoundException, GeneralCryptoLibException {
		Vote vote = prepareMockVote();
		Verification verification = prepareMockVerification();
		VerificationContent verificationContent = prepareVerificationContentMock();
		VerificationSetEntity verificationSetEntity = prepareVerificationSetMock();

		Mockito.when(verificationRepository.findByTenantIdElectionEventIdVerificationCardId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_ID))
				.thenReturn(verification);
		Mockito.when(verificationContentRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationContent);
		Mockito.when(
				verificationSetRepository.findByTenantIdElectionEventIdVerificationCardSetId(TENANT_ID, ELECTION_EVENT_ID, VERIFICATION_CARD_SET_ID))
				.thenReturn(verificationSetEntity);
		Mockito.when(
				electionPublicKeyRepository.findByTenantIdElectionEventIdElectoralAuthorityId(TENANT_ID, ELECTION_EVENT_ID, ELECTORAL_AUTHORITY_ID))
				.thenThrow(resourceNotFoundException);

		ValidationError result = plainTextEqualityProofRuleDecorator.execute(vote);

		assertEquals(ValidationErrorType.FAILED, result.getValidationErrorType());
	}

	private ElectionPublicKey prepareElectionPublicKeyMock(String electoralAuthKey) {
		JsonObjectBuilder json = Json.createObjectBuilder();
		json.add(PlainTextEqualityProofRule.JSON_PUBLIC_KEY, electoralAuthKey);

		JsonObjectBuilder signedObj = Json.createObjectBuilder();
		signedObj.add(JsonSignatureService.SIGNED_OBJECT_FIELD_NAME, json);

		ElectionPublicKey electionPublicKey = new ElectionPublicKey();

		electionPublicKey.setElectionEventId(ELECTION_EVENT_ID);
		electionPublicKey.setElectoralAuthorityId(ELECTORAL_AUTHORITY_ID);
		electionPublicKey.setId(1);
		electionPublicKey.setTenantId(TENANT_ID);
		String b64Json = Base64.getEncoder().encodeToString(signedObj.build().toString().getBytes(StandardCharsets.UTF_8));
		electionPublicKey.setJwt("." + b64Json + ".");

		return electionPublicKey;
	}

	private VerificationSetEntity prepareVerificationSetMock() {
		JsonObjectBuilder json = Json.createObjectBuilder();
		json.add(PlainTextEqualityProofRule.JSON_CHOICE_CODES_PUBLIC_KEY, VERIFICATION_CHOICE_CODES_PUBLIC_KEY);

		VerificationSetEntity verSet = new VerificationSetEntity();
		verSet.setElectionEventId(ELECTION_EVENT_ID);
		verSet.setJson(json.build().toString());
		verSet.setSignature(VERIFICATION_SET_SIGNATURE);
		verSet.setTenantId(TENANT_ID);
		verSet.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);

		return verSet;
	}

	private VerificationContent prepareVerificationContentMock() {
		JsonObjectBuilder jsonEncryptionParams = Json.createObjectBuilder();
		jsonEncryptionParams.add(PlainTextEqualityProofRule.JSON_PARAMETER_GENERATOR, "2");
		jsonEncryptionParams.add(PlainTextEqualityProofRule.JSON_PARAMETER_P, "7");
		jsonEncryptionParams.add(PlainTextEqualityProofRule.JSON_PARAMETER_Q, "3");

		JsonObjectBuilder json = Json.createObjectBuilder();
		json.add(PlainTextEqualityProofRule.JSON_PARAMETER_ELECTORAL_AUTHORITY_ID, ELECTORAL_AUTHORITY_ID);
		json.add(PlainTextEqualityProofRule.JSON_PARAMETER_ENCRYPTION_PARAMETERS, jsonEncryptionParams);

		VerificationContent verificationContent = new VerificationContent();
		verificationContent.setElectionEventId(ELECTION_EVENT_ID);
		verificationContent.setJson(json.build().toString());
		verificationContent.setTenantId(TENANT_ID);
		verificationContent.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);

		return verificationContent;
	}

	private Verification prepareMockVerification() {
		Verification verification = new Verification();
		verification.setElectionEventId(ELECTION_EVENT_ID);
		verification.setId(1);
		verification.setSignedVerificationPublicKey(SIGNED_VERIFICATION_PUBLIC_KEY);
		verification.setTenantId(TENANT_ID);
		verification.setVerificationCardId(VERIFICATION_CARD_ID);
		verification.setVerificationCardKeystore(VERIFICATION_CARD_KEYSTORE);
		verification.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);

		return verification;
	}

	private Vote prepareMockVote() throws GeneralCryptoLibException {
		List<Exponent> exponentList = new ArrayList<>();
		exponentList.add(new Exponent(new BigInteger("3"), new BigInteger("2")));

		Proof proofMock = new Proof(new Exponent(new BigInteger("3"), new BigInteger("2")), exponentList);

		Vote vote = new Vote();
		vote.setAuthenticationToken(AUTHENTICATION_TOKEN);
		vote.setCipherTextExponentiations(CIPHER_TEXT_EXPONENTIATIONS);
		vote.setCorrectnessIds(CORRECTNESS_IDS);
		vote.setElectionEventId(ELECTION_EVENT_ID);
		vote.setEncryptedOptions(ENCRYPTED_OPTIONS);
		vote.setEncryptedPartialChoiceCodes(ENCRYPTED_PARTIAL_CHOICE_CODES);
		vote.setExponentiationProof(EXPONENTIATION_PROOF);
		vote.setPlaintextEqualityProof(proofMock.toJson());
		vote.setTenantId(TENANT_ID);
		vote.setVerificationCardId(VERIFICATION_CARD_ID);
		vote.setVerificationCardPKSignature(VERIFICATION_CARD_PK_SIGNATURE);
		vote.setVerificationCardPublicKey(VERIFICATION_CARD_PUBLIC_KEY);
		vote.setVerificationCardSetId(VERIFICATION_CARD_SET_ID);
		vote.setVotingCardId(VOTING_CARD_ID);

		return vote;
	}
}
