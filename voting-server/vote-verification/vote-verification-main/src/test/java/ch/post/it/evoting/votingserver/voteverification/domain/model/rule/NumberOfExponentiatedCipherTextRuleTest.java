/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.rule;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.crypto.Constants;

@RunWith(MockitoJUnitRunner.class)
public class NumberOfExponentiatedCipherTextRuleTest {

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

	private static final String CIPHER_TEXT_EXPONENTIATIONS = "3" + Constants.SEPARATOR_ENCRYPTED_OPTIONS + "4";

	private static final String ENCRYPTED_PARTIAL_CHOICE_CODES = "2" + Constants.SEPARATOR_ENCRYPTED_OPTIONS + "6";

	@InjectMocks
	private final NumberOfExponentiatedCipherTextRule rule = new NumberOfExponentiatedCipherTextRule();

	@InjectMocks
	private final NumberOfExponentiatedCipherTextRuleDecorator cut = new NumberOfExponentiatedCipherTextRuleDecorator();

	@Before
	public void setUp() {
		cut.numberOfExponentiatedCipherTextRule = rule;
	}

	@Test
	public void test_wrong_value() throws GeneralCryptoLibException {
		Vote invalidVote = prepareMockVote();
		invalidVote.setCipherTextExponentiations("3");
		invalidVote.setEncryptedOptions("3" + Constants.SEPARATOR_ENCRYPTED_OPTIONS + "4");

		ValidationError result = cut.execute(invalidVote);

		assertEquals(ValidationErrorType.INVALID_NUMER_COMPONENTS_EXPONENTIATED_CIPHER_TEXT, result.getValidationErrorType());
	}

	@Test
	public void test_null_value() throws GeneralCryptoLibException {
		Vote invalidVote = prepareMockVote();
		invalidVote.setCipherTextExponentiations(null);
		invalidVote.setEncryptedOptions(null);

		ValidationError result = cut.execute(invalidVote);

		assertEquals(ValidationErrorType.FAILED, result.getValidationErrorType());
	}

	@Test
	public void test_empty_value() throws GeneralCryptoLibException {
		Vote invalidVote = prepareMockVote();
		invalidVote.setCipherTextExponentiations("");
		invalidVote.setEncryptedOptions("");

		ValidationError result = cut.execute(invalidVote);

		assertEquals(ValidationErrorType.FAILED, result.getValidationErrorType());
	}

	@Test
	public void test_valid() throws GeneralCryptoLibException {
		Vote vote = prepareMockVote();
		vote.setCipherTextExponentiations("3");
		vote.setEncryptedOptions("3");

		ValidationError result = cut.execute(vote);

		assertEquals(ValidationErrorType.SUCCESS, result.getValidationErrorType());
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
