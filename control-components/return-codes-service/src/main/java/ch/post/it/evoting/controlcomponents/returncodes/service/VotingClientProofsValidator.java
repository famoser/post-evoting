/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.controlcomponents.returncodes.domain.VerificationCardPublicKeyExtended;
import ch.post.it.evoting.controlcomponents.returncodes.domain.VerificationCardPublicKeyExtendedRepository;
import ch.post.it.evoting.controlcomponents.returncodes.service.exception.MissingVerificationCardPublicKeyExtendedException;
import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.commons.serialization.JsonSignatureService;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.maurer.factory.ZpSubgroupProofVerifier;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.cryptoadapters.CryptoAdapters;
import ch.post.it.evoting.domain.election.ElectionPublicKey;
import ch.post.it.evoting.domain.election.VerificationCardSetData;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.election.validation.ValidationError;
import ch.post.it.evoting.domain.election.validation.ValidationErrorType;
import ch.post.it.evoting.domain.returncodes.PartialChoiceReturnCodesVerificationInput;

/**
 * Checks the voting client's non-interactive zero-knowledge proofs (Exponentiation Proof, and Plaintext Equality Proof) and implements part of the
 * VerifyBallotCCR algorithm
 */
@Service
public class VotingClientProofsValidator {

	public static final String SEPARATOR_ENCRYPTED_OPTIONS = ";";
	public static final int POSITION_C0 = 0;
	public static final int POSITION_C1 = 1;

	private static final Logger LOGGER = LoggerFactory.getLogger(VotingClientProofsValidator.class);
	private final Base64.Decoder decoder = Base64.getDecoder();
	private final GroupElementsCompressor<ZpGroupElement> compressor = new GroupElementsCompressor<>();
	private final ProofsServiceAPI proofsService;
	private final AsymmetricServiceAPI asymmetricService;
	private final VerificationCardPublicKeyExtendedRepository verificationCardPublicKeyExtendedRepository;

	@Value("${keys.nodeId:defCcxId}")
	private String controlComponentId;

	@Autowired
	public VotingClientProofsValidator(final ProofsServiceAPI proofsService, final AsymmetricServiceAPI asymmetricService,
			final VerificationCardPublicKeyExtendedRepository verificationCardPublicKeyExtendedRepository) {
		this.proofsService = proofsService;
		this.asymmetricService = asymmetricService;
		this.verificationCardPublicKeyExtendedRepository = verificationCardPublicKeyExtendedRepository;
	}

	public static String[] getCiphertextElementsFromEncryptedOptions(String encryptedOptions) {
		return encryptedOptions.split(SEPARATOR_ENCRYPTED_OPTIONS);
	}

	public static String getC0FromEncryptedOptions(String encryptedOptions) {
		return encryptedOptions.split(SEPARATOR_ENCRYPTED_OPTIONS)[POSITION_C0];
	}

	public static String getC1FromEncryptedOptions(String encryptedOptions) {
		return encryptedOptions.split(SEPARATOR_ENCRYPTED_OPTIONS)[POSITION_C1];
	}

	public boolean validateVoteAndProofs(ZpSubgroup mathematicalGroup,
			PartialChoiceReturnCodesVerificationInput partialChoiceReturnCodesVerificationInput, PublicKey adminBoardPublicKey) {
		try {
			Vote voteObject = ObjectMappers.fromJson(partialChoiceReturnCodesVerificationInput.getVote(), Vote.class);

			ElGamalPublicKey electionPublicKey = verifyAndGetElectionPublicKey(partialChoiceReturnCodesVerificationInput.getElectionPublicKeyJwt(),
					adminBoardPublicKey);

			if (electionPublicKey == null) {
				LOGGER.error("The election public key is null.");
				return false;
			}

			VerificationCardSetData verificationCardSetData = verifyAndGetVerificationCardSetData(
					partialChoiceReturnCodesVerificationInput.getVerificationCardSetDataJwt(), adminBoardPublicKey);

			if (verificationCardSetData == null) {
				LOGGER.error("The verification card set data is null.");
				return false;
			}

			if (!verifyVerificationCardPublicKey(verificationCardSetData.getVerificationCardSetIssuerCert(), voteObject)) {
				LOGGER.error("The Verification Card Public Key could not be verified.");
				return false;
			}

			ValidationError exponentiationProofValidation = validateExponentiationProof(new ValidationError(), mathematicalGroup, voteObject);
			boolean exponentiationProofValidationResult = !ValidationErrorType.FAILED.equals(exponentiationProofValidation.getValidationErrorType());
			if (!exponentiationProofValidationResult) {
				String errorArgs = StringUtils.join(exponentiationProofValidation.getErrorArgs());
				LOGGER.error("Exponentiation proof not valid. {}", errorArgs);
			}

			ValidationError plaintextEqualityProofValidation = validatePlaintextEqualityProof(new ValidationError(), mathematicalGroup, voteObject,
					electionPublicKey, verificationCardSetData.getChoicesCodesEncryptionPublicKey());
			boolean plaintextEqualityProofValidationResult = !ValidationErrorType.FAILED
					.equals(plaintextEqualityProofValidation.getValidationErrorType());
			if (!plaintextEqualityProofValidationResult) {
				String errorArgs = StringUtils.join(exponentiationProofValidation.getErrorArgs());
				LOGGER.error("Plaintext equality proof not valid. {}", errorArgs);
			}

			if (exponentiationProofValidationResult && plaintextEqualityProofValidationResult) {
				LOGGER.info("Successful vote validation.");
				return true;
			}
			return false;

		} catch (GeneralCryptoLibException | IOException e) {
			LOGGER.error("Error trying to validate the vote and its corresponding zero-knowledge proofs.", e);
			return false;
		}
	}

	/**
	 * Validates an exponentiation zero-knowledge proof.
	 * <p>
	 * At this point, we already checked the group membership of all elements, via the class {@link ZpSubgroupProofVerifier} of CryptoLib.
	 * </p>
	 */
	private ValidationError validateExponentiationProof(ValidationError result, ZpSubgroup mathematicalGroup, Vote vote) {
		try {
			result.setValidationErrorType(ValidationErrorType.FAILED);

			List<ZpGroupElement> groupElements = new ArrayList<>();
			String[] exponentiatedCiphertext = getCiphertextElementsFromEncryptedOptions(vote.getEncryptedOptions());
			for (String elementValue : exponentiatedCiphertext) {
				ZpGroupElement groupElement = new ZpGroupElement(new BigInteger(elementValue), mathematicalGroup);
				groupElements.add(groupElement);
			}

			// create a list of exponentiated elements consisting of the verification card public key and the exponentiated ciphertexts.

			final String verificationCardId = vote.getVerificationCardId();
			final Optional<VerificationCardPublicKeyExtended> optionalVerificationCardPublicKeyExtended = verificationCardPublicKeyExtendedRepository
					.findById(verificationCardId);

			if (!optionalVerificationCardPublicKeyExtended.isPresent()) {
				throw new MissingVerificationCardPublicKeyExtendedException(verificationCardId);
			}

			final ElGamalPublicKey verificationCardPublicKey = CryptoAdapters
					.convert(optionalVerificationCardPublicKeyExtended.get().getVerificationCardPublicKey());

			// There must be only one exponent in the verification card public key.
			List<ZpGroupElement> verificationCardPublicKeyList = verificationCardPublicKey.getKeys();
			if (verificationCardPublicKeyList.size() != 1) {
				throw new IllegalArgumentException(
						String.format("Unexpected number of keys: found %s but should be 1.", verificationCardPublicKeyList.size()));
			}

			List<ZpGroupElement> exponentiatedElements = new ArrayList<>();
			ZpGroupElement verificationCardPublicKeySingleElement = verificationCardPublicKeyList.get(0);
			exponentiatedElements.add(verificationCardPublicKeySingleElement);
			exponentiatedCiphertext = getCiphertextElementsFromEncryptedOptions(vote.getCipherTextExponentiations());

			for (String elementValue : exponentiatedCiphertext) {
				ZpGroupElement exponentiatedElement = new ZpGroupElement(new BigInteger(elementValue), mathematicalGroup);
				exponentiatedElements.add(exponentiatedElement);
			}

			// create a list of base elements consisting of the encryption group's generator and the multiplied voting options ciphertext (E1).
			List<ZpGroupElement> baseElements = new ArrayList<>();

			ZpGroupElement groupElementGenerator = new ZpGroupElement(mathematicalGroup.getG(), mathematicalGroup);
			baseElements.add(groupElementGenerator);
			baseElements.addAll(groupElements);

			if (proofsService.createProofVerifierAPI(mathematicalGroup)
					.verifyExponentiationProof(exponentiatedElements, baseElements, Proof.fromJson(vote.getExponentiationProof()))) {
				result.setValidationErrorType(ValidationErrorType.SUCCESS);
			}
		} catch (GeneralCryptoLibException | NumberFormatException e) {
			LOGGER.error("The validation of the exponentiation proof failed.", e);
		}
		return result;

	}

	/**
	 * Validates a plaintext equality zero-knowledge proof.
	 * <p>
	 * At this point, we already checked the group membership of all elements, via the class {@link ZpSubgroupProofVerifier} of CryptoLib.
	 * </p>
	 */
	private ValidationError validatePlaintextEqualityProof(ValidationError validationError, ZpSubgroup mathematicalGroup, Vote vote,
			ElGamalPublicKey electionPublicKey, String choiceReturnCodesEncryptionPublicKeyString) {
		try {
			validationError.setValidationErrorType(ValidationErrorType.FAILED);

			String choiceReturnCodesEncryptionPKString = new String(decoder.decode(choiceReturnCodesEncryptionPublicKeyString),
					StandardCharsets.UTF_8);

			ElGamalPublicKey choiceReturnCodesEncryptionPublicKey = ElGamalPublicKey.fromJson(choiceReturnCodesEncryptionPKString);

			// Retrieval of ciphertext.
			Ciphertext primaryCiphertext = getPrimaryCipherText(vote, mathematicalGroup);
			Ciphertext secondaryCiphertext = getSecondaryCipherText(vote, mathematicalGroup);

			// We multiply the Choice Return Codes encryption public keys and designate this operation as "compression".
			ZpGroupElement multipliedChoiceReturnCodesEncryptionPK = compressor.compress(choiceReturnCodesEncryptionPublicKey.getKeys());

			ElGamalPublicKey multipliedChoiceReturnCodesEncryptionPublicKey = new ElGamalPublicKey(
					Collections.singletonList(multipliedChoiceReturnCodesEncryptionPK), mathematicalGroup);

			if (proofsService.createProofVerifierAPI(mathematicalGroup)
					.verifyPlaintextEqualityProof(primaryCiphertext, getKeyComposedOfFirstSubkeyOnly(electionPublicKey), secondaryCiphertext,
							multipliedChoiceReturnCodesEncryptionPublicKey, Proof.fromJson(vote.getPlaintextEqualityProof()))) {
				validationError.setValidationErrorType(ValidationErrorType.SUCCESS);
			}

		} catch (GeneralCryptoLibException | NumberFormatException e) {
			LOGGER.error("The validation of the plaintext equality proof failed.", e);
		}
		return validationError;
	}

	/**
	 * Verifies that the VerificationCardPublicKey was issued to the correct verification card ID by the VerificationCardSetIssuer Key.
	 */
	private boolean verifyVerificationCardPublicKey(String verificationCardSetIssuerCert, Vote vote) throws GeneralCryptoLibException {
		byte[] verificationCardPKSignatureBytes = decoder.decode(vote.getVerificationCardPKSignature());
		PublicKey vcsTrustedPublicKey = PemUtils.certificateFromPem(verificationCardSetIssuerCert).getPublicKey();

		final byte[] verificationCardPublicKeyAsBytes = decoder.decode(vote.getVerificationCardPublicKey());
		final byte[] electionEventIdAsBytes = vote.getElectionEventId().getBytes(StandardCharsets.UTF_8);
		final byte[] verificationCardIdAsBytes = vote.getVerificationCardId().getBytes(StandardCharsets.UTF_8);

		boolean verified = asymmetricService
				.verifySignature(verificationCardPKSignatureBytes, vcsTrustedPublicKey, verificationCardPublicKeyAsBytes, electionEventIdAsBytes,
						verificationCardIdAsBytes);

		if (!verified) {
			LOGGER.error("Invalid digital signature of the Verification Card Public Key.");
		} else {
			LOGGER.info("Successfully verified the Verification Card Public Key.");
		}

		return verified;
	}

	private ElGamalPublicKey verifyAndGetElectionPublicKey(String electionPublicKeyJwt, PublicKey adminBoardPublicKey)
			throws GeneralCryptoLibException {

		// Retrieval and verification of the Election Public Key.
		ElectionPublicKey electionPublicKeyObj;
		try {
			electionPublicKeyObj = JsonSignatureService.verify(adminBoardPublicKey, electionPublicKeyJwt, ElectionPublicKey.class);
		} catch (Exception e) {
			LOGGER.error("Election public key signature could not be verified.", e);
			return null;
		}

		String electionPublicKeyJson = new String(decoder.decode(electionPublicKeyObj.getPublicKey()), StandardCharsets.UTF_8);

		return ElGamalPublicKey.fromJson(electionPublicKeyJson);
	}

	private VerificationCardSetData verifyAndGetVerificationCardSetData(String verificationSetEntityJwt, PublicKey adminBoardPublicKey) {
		try {
			return JsonSignatureService.verify(adminBoardPublicKey, verificationSetEntityJwt, VerificationCardSetData.class);
		} catch (Exception e) {
			LOGGER.error("Verification Card Set data could not be verified.", e);
			return null;
		}
	}

	private Ciphertext getPrimaryCipherText(Vote vote, ZpSubgroup mathematicalGroup) throws GeneralCryptoLibException {
		// Retrieval of the C'0 and C'1 values - corresponding to the exponentiated ciphertext of the product of selected voting options.
		BigInteger exponentiatedCipherTextC0 = new BigInteger(getC0FromEncryptedOptions(vote.getCipherTextExponentiations()));
		BigInteger exponentiatedCipherTextC1 = new BigInteger(getC1FromEncryptedOptions(vote.getCipherTextExponentiations()));

		LOGGER.debug("C'0 value = {}", exponentiatedCipherTextC0);
		LOGGER.debug("C'1 value = {}", exponentiatedCipherTextC1);

		// Creation of the plaintext equality proof's primary ciphertext from C'0 and C'1 values.
		ZpGroupElement exponentiatedGamma = new ZpGroupElement(exponentiatedCipherTextC0, mathematicalGroup);

		List<ZpGroupElement> exponentiatedPhis = new ArrayList<>();
		exponentiatedPhis.add(new ZpGroupElement(exponentiatedCipherTextC1, mathematicalGroup));

		ElGamalCiphertext exponentiatedE1Ciphertext = new ElGamalCiphertext(exponentiatedGamma, exponentiatedPhis);

		return new ElGamalEncrypterValues(new Exponent(mathematicalGroup.getQ(), BigInteger.ONE), exponentiatedE1Ciphertext);
	}

	private Ciphertext getSecondaryCipherText(Vote vote, ZpSubgroup mathematicalGroup) throws GeneralCryptoLibException {
		// Retrieval of D0 and D'1 as result of compressing the D1-Dn elements of the encrypted partial Choice Return Codes.
		String encryptedPartialChoiceReturnCodes = vote.getEncryptedPartialChoiceCodes();
		BigInteger valueD0 = new BigInteger(encryptedPartialChoiceReturnCodes.split(";")[0]);
		ZpGroupElement zpGroupElementD0 = new ZpGroupElement(valueD0, mathematicalGroup);
		ZpGroupElement zpGroupElementD1prima = getCompressedListFromEncryptedPartialChoiceReturnCodes(mathematicalGroup,
				encryptedPartialChoiceReturnCodes);

		LOGGER.debug("D0 value = {}", zpGroupElementD0);
		LOGGER.debug("D'1 value = {}", zpGroupElementD1prima);

		// Creation of secondary ciphertext from D0 and D'1 values.
		ZpGroupElement gamma = new ZpGroupElement(valueD0, mathematicalGroup);
		List<ZpGroupElement> phis = new ArrayList<>();
		phis.add(zpGroupElementD1prima);
		ElGamalCiphertext elGamalCiphertext = new ElGamalCiphertext(gamma, phis);

		return new ElGamalEncrypterValues(new Exponent(mathematicalGroup.getQ(), BigInteger.ONE), elGamalCiphertext);
	}

	private ZpGroupElement getCompressedListFromEncryptedPartialChoiceReturnCodes(ZpSubgroup mathematicalGroup, String encryptedChoiceCodes)
			throws GeneralCryptoLibException {
		String[] partials = encryptedChoiceCodes.split(";");
		List<ZpGroupElement> elements = new ArrayList<>();
		for (int i = 1; i < partials.length; i++) {
			elements.add(new ZpGroupElement(new BigInteger(partials[i]), mathematicalGroup));
		}
		return compressor.compress(elements);
	}

	private ElGamalPublicKey getKeyComposedOfFirstSubkeyOnly(ElGamalPublicKey originalKey) throws GeneralCryptoLibException {
		List<ZpGroupElement> firstSubKey = new ArrayList<>();
		firstSubKey.add(originalKey.getKeys().get(0));

		return new ElGamalPublicKey(firstSubKey, originalKey.getGroup());
	}
}
