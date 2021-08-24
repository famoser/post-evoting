/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.domain.model.rule;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
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
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContent;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContentRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationRepository;

/**
 * This is the implementation of a validator of the exponentiation proof.
 */
public class ExponentiationProofRule implements AbstractRule<Vote> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExponentiationProofRule.class);
	@Inject
	private VerificationRepository verificationRepository;
	@Inject
	private VerificationContentRepository verificationContentRepository;
	@Inject
	private ProofsServiceAPI proofsService;

	/**
	 * This method validates from the given vote the Exponentiation proof. The steps are: generating the base elements, exponentiated elements and
	 * needed as input for the validator in the cryptolib.
	 *
	 * @see AbstractRule#execute(Object)
	 */
	@Override
	public ValidationError execute(Vote vote) {
		ValidationError result = new ValidationError();

		VoteVerificationContextData voteVerificationContextData;
		try {

			Verification verification = verificationRepository
					.findByTenantIdElectionEventIdVerificationCardId(vote.getTenantId(), vote.getElectionEventId(), vote.getVerificationCardId());
			VerificationContent verificationContent = verificationContentRepository
					.findByTenantIdElectionEventIdVerificationCardSetId(vote.getTenantId(), vote.getElectionEventId(),
							verification.getVerificationCardSetId());
			voteVerificationContextData = ObjectMappers.fromJson(verificationContent.getJson(), VoteVerificationContextData.class);
		} catch (ResourceNotFoundException | IOException e) {
			LOGGER.error("The validation of the exponentation proof failed, verification data not found", e);
			return result;
		}

		EncryptionParameters encryptionParameters = voteVerificationContextData.getEncryptionParameters();
		String generatorEncryptParam = encryptionParameters.getG();
		String pEncryptParam = encryptionParameters.getP();
		String qEncryptParam = encryptionParameters.getQ();

		ZpSubgroup mathematicalGroup;
		try {
			BigInteger generatorEncryptParamBigInteger = new BigInteger(generatorEncryptParam);
			BigInteger pEncryptParamBigInteger = new BigInteger(pEncryptParam);
			BigInteger qEncryptParamBigInteger = new BigInteger(qEncryptParam);
			mathematicalGroup = new ZpSubgroup(generatorEncryptParamBigInteger, pEncryptParamBigInteger, qEncryptParamBigInteger);

			List<ZpGroupElement> groupElements = new ArrayList<>();
			String[] ciphertextElements = Utils.getCiphertextElementsFromEncryptedOptions(vote.getEncryptedOptions());
			for (String elementValue : ciphertextElements) {
				ZpGroupElement groupElement = new ZpGroupElement(new BigInteger(elementValue), mathematicalGroup);
				groupElements.add(groupElement);
			}

			// create the input parameter for the cryptolib validator of
			// exponentiation proof
			// create a list of exponentiated elements as group elements of
			// verification card public key and ciphertext
			// elements
			List<ZpGroupElement> exponentiatedElements = new ArrayList<>();
			String verificationCardPublicKeyString = new String(Base64.getDecoder().decode(vote.getVerificationCardPublicKey()),
					StandardCharsets.UTF_8);

			ZpGroupElement verificationCardPublicKey = ElGamalPublicKey.fromJson(verificationCardPublicKeyString).getKeys().get(0);
			exponentiatedElements.add(verificationCardPublicKey);
			ciphertextElements = Utils.getCiphertextElementsFromEncryptedOptions(vote.getCipherTextExponentiations());
			for (String elementValue : ciphertextElements) {
				ZpGroupElement exponentiatedElement = new ZpGroupElement(new BigInteger(elementValue), mathematicalGroup);
				exponentiatedElements.add(exponentiatedElement);
			}

			// create a list of base elements for generator and ciphertext
			// elements
			List<ZpGroupElement> baseElements = new ArrayList<>();
			ZpGroupElement groupElementGenerator = new ZpGroupElement(generatorEncryptParamBigInteger, mathematicalGroup);
			baseElements.add(groupElementGenerator);
			for (ZpGroupElement groupElement : groupElements) {
				baseElements.add(groupElement);
			}

			if (proofsService.createProofVerifierAPI(mathematicalGroup)
					.verifyExponentiationProof(exponentiatedElements, baseElements, Proof.fromJson(vote.getExponentiationProof()))) {
				result.setValidationErrorType(ValidationErrorType.SUCCESS);
			}

		} catch (GeneralCryptoLibException | NumberFormatException e) {
			LOGGER.error("The validation of the exponentation proof failed", e);
		}
		return result;
	}

	/**
	 * @see AbstractRule#getName()
	 */
	@Override
	public String getName() {
		return RuleNames.VOTE_EXPONENTIATION_PROOF.getText();
	}
}
