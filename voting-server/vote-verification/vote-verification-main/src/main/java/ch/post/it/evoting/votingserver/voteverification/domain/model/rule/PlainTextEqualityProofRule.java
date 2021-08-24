/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.rule;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.crypto.Utils;
import ch.post.it.evoting.votingserver.commons.domain.model.rule.AbstractRule;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.commons.util.JwtUtils;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKey;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKeyRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContent;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContentRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

/**
 * This is an implementation of a validator of the Plaintext equality Proof.
 */
public class PlainTextEqualityProofRule implements AbstractRule<Vote> {

	// The name of the json parameter p (group).
	static final String JSON_PARAMETER_P = "p";

	// The name of the json parameter q (order).
	static final String JSON_PARAMETER_Q = "q";

	// The name of the json parameter generator.
	static final String JSON_PARAMETER_GENERATOR = "g";

	// The name of the json parameter encryptionParameters.
	static final String JSON_PARAMETER_ENCRYPTION_PARAMETERS = "encryptionParameters";

	static final String JSON_PUBLIC_KEY = "publicKey";

	static final String JSON_PARAMETER_ELECTORAL_AUTHORITY_ID = "electoralAuthorityId";

	static final String JSON_CHOICE_CODES_PUBLIC_KEY = "choicesCodesEncryptionPublicKey";
	private static final Logger LOGGER = LoggerFactory.getLogger(PlainTextEqualityProofRule.class);
	@Inject
	VerificationSetRepository verificationSetRepository;
	@Inject
	VerificationRepository verificationRepository;
	@Inject
	VerificationContentRepository verificationContentRepository;
	@Inject
	ElectionPublicKeyRepository electionPublicKeyRepository;
	@Inject
	private ProofsServiceAPI proofsService;

	/**
	 * Executes a plain text equality proof validation.
	 *
	 * @param vote - vote to be validated
	 * @return boolean value with the result of the validation
	 */
	@Override
	public ValidationError execute(Vote vote) {
		ValidationError result = new ValidationError();

		VerificationContent verificationContent;
		VerificationSetEntity verificationSetEntity;
		ElectionPublicKey electionPublicKey;
		VoteVerificationContextData voteVerificationContextData;
		JsonObject verificationSetJSON;
		try {
			Verification verification;
			verification = verificationRepository
					.findByTenantIdElectionEventIdVerificationCardId(vote.getTenantId(), vote.getElectionEventId(), vote.getVerificationCardId());

			verificationContent = verificationContentRepository
					.findByTenantIdElectionEventIdVerificationCardSetId(vote.getTenantId(), vote.getElectionEventId(),
							verification.getVerificationCardSetId());

			verificationSetEntity = verificationSetRepository
					.findByTenantIdElectionEventIdVerificationCardSetId(vote.getTenantId(), vote.getElectionEventId(),
							verification.getVerificationCardSetId());

			voteVerificationContextData = ObjectMappers.fromJson(verificationContent.getJson(), VoteVerificationContextData.class);

			verificationSetJSON = JsonUtils.getJsonObject(verificationSetEntity.getJson());

			String electoralAuthorityId = voteVerificationContextData.getElectoralAuthorityId();

			electionPublicKey = electionPublicKeyRepository
					.findByTenantIdElectionEventIdElectoralAuthorityId(vote.getTenantId(), vote.getElectionEventId(), electoralAuthorityId);

		} catch (ResourceNotFoundException | IOException e) {
			LOGGER.error("The validation of the plaintext equality proof failed, verification data not found", e);
			return result;
		}
		try {
			// Retrieval of the encryption parameters and electoral public key
			EncryptionParameters encryptionParameters = voteVerificationContextData.getEncryptionParameters();
			String generatorEncryptParam = encryptionParameters.getG();
			String pEncryptParam = encryptionParameters.getP();
			String qEncryptParam = encryptionParameters.getQ();

			LOGGER.debug("Parameters to be used for PlaintText Equality proof are:");
			LOGGER.debug("g = {}", generatorEncryptParam);
			LOGGER.debug("p = {}", pEncryptParam);
			LOGGER.debug("q = {}", qEncryptParam);

			String electoralPublicKeyString = JwtUtils.getJsonObject(electionPublicKey.getJwt()).getString(JSON_PUBLIC_KEY);

			// Retrieval of the Election Public Key
			ElGamalPublicKey elGamalElectoralPublicKey = ElGamalPublicKey
					.fromJson(new String(Base64.getDecoder().decode(electoralPublicKeyString), StandardCharsets.UTF_8));

			// Retrieval of the Choice Return Codes Encryption Public Key
			String choiceCodesEncryptionPKString = new String(Base64.getDecoder().decode(verificationSetJSON.getString(JSON_CHOICE_CODES_PUBLIC_KEY)),
					StandardCharsets.UTF_8);
			ElGamalPublicKey elGamalChoiceCodesPublicKey = ElGamalPublicKey.fromJson(choiceCodesEncryptionPKString);

			ZpSubgroup mathematicalGroup = new ZpSubgroup(new BigInteger(generatorEncryptParam), new BigInteger(pEncryptParam),
					new BigInteger(qEncryptParam));

			// Retrieval of ciphertext.
			Ciphertext primaryCiphertext = getPrimaryCipherText(vote, mathematicalGroup);
			Ciphertext secondaryCiphertext = getSecondaryCipherText(vote, mathematicalGroup);

			// The choice Codes encryption PK is compressed applying the same
			// technique.Its keys are used as input
			ZpGroupElement compressedChoiceCodesPKGroupElement = getCompressedList(elGamalChoiceCodesPublicKey.getKeys());
			ElGamalPublicKey compressedChoiceCodesEncryptionPK = new ElGamalPublicKey(Collections.singletonList(compressedChoiceCodesPKGroupElement),
					mathematicalGroup);

			if (proofsService.createProofVerifierAPI(mathematicalGroup)
					.verifyPlaintextEqualityProof(primaryCiphertext, getKeyComposedOfFirstSubkeyOnly(elGamalElectoralPublicKey), secondaryCiphertext,
							compressedChoiceCodesEncryptionPK, Proof.fromJson(vote.getPlaintextEqualityProof()))) {
				result.setValidationErrorType(ValidationErrorType.SUCCESS);
			}

		} catch (GeneralCryptoLibException | NumberFormatException e) {
			LOGGER.error("The validation of the plaintext equality proof failed", e);
		}
		return result;
	}

	@Override
	public String getName() {
		return RuleNames.VOTE_PLAINTEXT_EQUALITY_PROOF.getText();
	}

	private Ciphertext getPrimaryCipherText(Vote vote, ZpSubgroup mathematicalGroup) throws GeneralCryptoLibException {
		// Retrieval of the C'0 and C'1 values.
		BigInteger c0CipherText = new BigInteger(Utils.getC0FromEncryptedOptions(vote.getCipherTextExponentiations()));
		BigInteger c1CipherText = new BigInteger(Utils.getC1FromEncryptedOptions(vote.getCipherTextExponentiations()));

		LOGGER.debug("C'0 value = {}", c0CipherText);
		LOGGER.debug("C'1 value = {}", c1CipherText);

		// Creation of primary ciphertext from C'0 and C'1 values.
		ZpGroupElement gamma = new ZpGroupElement(c0CipherText, mathematicalGroup);
		List<ZpGroupElement> phis = new ArrayList<>();
		phis.add(new ZpGroupElement(c1CipherText, mathematicalGroup));
		ElGamalCiphertext elGamalCiphertext = new ElGamalCiphertext(gamma, phis);

		return new ElGamalEncrypterValues(new Exponent(mathematicalGroup.getQ(), BigInteger.ONE), elGamalCiphertext);
	}

	private Ciphertext getSecondaryCipherText(Vote vote, ZpSubgroup mathematicalGroup) throws GeneralCryptoLibException {
		// Retrieval of D0 and D'1 as result of compressing the D1-Dn elements
		// of the partial choice codes.
		String encryptedChoiceCodes = vote.getEncryptedPartialChoiceCodes();
		BigInteger valueD0 = new BigInteger(encryptedChoiceCodes.split(";")[0]);
		ZpGroupElement d0 = new ZpGroupElement(valueD0, mathematicalGroup);
		ZpGroupElement d1prima = getCompressedListFromChoiceCodes(mathematicalGroup, encryptedChoiceCodes);

		LOGGER.debug("D0 value = {}", d0);
		LOGGER.debug("D'1 value = {}", d1prima);

		// Creation of secondary ciphertext from D0 and D'1 values.
		ZpGroupElement gamma = new ZpGroupElement(valueD0, mathematicalGroup);
		List<ZpGroupElement> phis = new ArrayList<>();
		phis.add(d1prima);
		ElGamalCiphertext elGamalCiphertext = new ElGamalCiphertext(gamma, phis);

		return new ElGamalEncrypterValues(new Exponent(mathematicalGroup.getQ(), BigInteger.ONE), elGamalCiphertext);
	}

	private ZpGroupElement getCompressedListFromChoiceCodes(ZpSubgroup mathematicalGroup, String encryptedChoiceCodes)
			throws GeneralCryptoLibException {
		String[] partials = encryptedChoiceCodes.split(";");
		List<ZpGroupElement> elements = new ArrayList<>();
		for (int i = 1; i < partials.length; i++) {
			elements.add(new ZpGroupElement(new BigInteger(partials[i]), mathematicalGroup));
		}
		return getCompressedList(elements);
	}

	private ZpGroupElement getCompressedList(List<ZpGroupElement> elements) throws GeneralCryptoLibException {
		GroupElementsCompressor<ZpGroupElement> compressor = new GroupElementsCompressor<>();
		return compressor.compress(elements);
	}

	private ElGamalPublicKey getKeyComposedOfFirstSubkeyOnly(ElGamalPublicKey originalKey) throws GeneralCryptoLibException {

		List<ZpGroupElement> firstSubKey = new ArrayList<>();
		firstSubKey.add(originalKey.getKeys().get(0));
		return new ElGamalPublicKey(firstSubKey, originalKey.getGroup());
	}
}
