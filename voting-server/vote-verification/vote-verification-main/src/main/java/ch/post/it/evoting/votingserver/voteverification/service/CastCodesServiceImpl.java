/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesService;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.model.confirmation.TraceableConfirmationMessage;
import ch.post.it.evoting.domain.returncodes.CastCodeAndComputeResults;
import ch.post.it.evoting.domain.returncodes.ConfirmationKeyVerificationInput;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.util.JsonUtils;
import ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode.CodesComputeResults;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

/**
 * Generate the short vote cast return code based on the confirmation message - in interaction with the control components.
 */
@Stateless
public class CastCodesServiceImpl implements CastCodeService {

	static final String PUBLIC_KEY = "choicesCodesEncryptionPublicKey";

	private static final Logger LOGGER = LoggerFactory.getLogger(CastCodesServiceImpl.class);
	// The verification repository that will give us the verification card set
	// id for the voter verification card id
	@Inject
	private VerificationRepository verificationRepository;

	// The service that will be used to verify the signature
	@Inject
	private AsymmetricServiceAPI asymmetricService;

	// The service used to retrieve the short vote cast return code
	@Inject
	private ShortCodesService shortCodesService;

	// The verification set repository that will give us the verification card
	// set for the voter verification card
	@Inject
	private VerificationSetRepository verificationSetRepository;

	@Inject
	private CodesComputeService codesComputeService;

	@Inject
	private VoterCodesService voterCodesService;

	/**
	 * @see CastCodeService#retrieveCastCode(String, String, String, TraceableConfirmationMessage)
	 */
	@Override
	public CastCodeAndComputeResults retrieveCastCode(final String tenantId, final String eeid, final String verificationCardId,
			TraceableConfirmationMessage confirmationMessage) throws ResourceNotFoundException, CryptographicOperationException {

		LOGGER.info("Generating the vote cast code for tenant: {}, election event: {} and verification card: {}.", tenantId, eeid,
				verificationCardId);

		Verification verification = verificationRepository.findByTenantIdElectionEventIdVerificationCardId(tenantId, eeid, verificationCardId);

		String verificationCardSetId = verification.getVerificationCardSetId();

		String confirmationCodeString = new String(Base64.getDecoder().decode(confirmationMessage.getConfirmationKey()), StandardCharsets.UTF_8);
		BigInteger confirmationCodeValue = new BigInteger(confirmationCodeString);

		VerificationSetEntity verificationSet = verificationSetRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(tenantId, eeid, verificationCardSetId);
		JsonObject object = JsonUtils.getJsonObject(verificationSet.getJson());
		String elgamalPublicKeyBase64 = object.getString(PUBLIC_KEY);
		byte[] elgamalPublicKeyBytes = Base64.getDecoder().decode(elgamalPublicKeyBase64.getBytes(StandardCharsets.UTF_8));
		String publicKeyString = new String(elgamalPublicKeyBytes, StandardCharsets.UTF_8);

		try {
			ElGamalPublicKey publicKey = ElGamalPublicKey.fromJson(publicKeyString);
			ZpSubgroup group = publicKey.getGroup();

			CodesComputeResults computedPartialCodes = codesComputeService
					.computePartialCodes(tenantId, eeid, verificationCardSetId, verificationCardId, group,
							createVerificationPayload(Collections.singletonList(confirmationCodeValue), confirmationMessage));

			final ZpGroupElement confirmationCodes = computedPartialCodes.getCombinedPartialChoiceCodes().get(confirmationCodeValue);

			// Partial Vote Cast Code consolidation -- Partial Vote Cast
			// Return Code correctly consolidated

			byte[] longCodes = voterCodesService.generateLongReturnCode(eeid, verificationCardId, confirmationCodes, Collections.emptyList());

			List<String> shortCodes = shortCodesService.retrieveShortCodes(tenantId, eeid, verificationCardId, Collections.singletonList(longCodes));
			String[] voteCastReturnCode = shortCodes.get(0).split(";");

			CastCodeAndComputeResults castCodeMessage = new CastCodeAndComputeResults();
			castCodeMessage.setVoteCastCode(voteCastReturnCode[0]);
			castCodeMessage.setComputationResults(ObjectMappers.toJson(computedPartialCodes.getComputationResults()));

			return castCodeMessage;

		} catch (GeneralCryptoLibException | JsonProcessingException e) {
			throw new CryptographicOperationException("Error retrieving the cast code message:", e);
		}

	}

	private ReturnCodesInput createVerificationPayload(List<BigInteger> confirmationCodeToCompute, TraceableConfirmationMessage confirmationMessage)
			throws JsonProcessingException {
		ConfirmationKeyVerificationInput confirmationKeyVerificationInput = new ConfirmationKeyVerificationInput();
		confirmationKeyVerificationInput.setConfirmationMessage(ObjectMappers.toJson(confirmationMessage));
		confirmationKeyVerificationInput.setVotingCardId(confirmationMessage.getVotingCardId());
		ReturnCodesInput returnCodesInput = new ReturnCodesInput();
		returnCodesInput.setReturnCodesInputElements(confirmationCodeToCompute);
		returnCodesInput.setConfirmationKeyVerificationInput(confirmationKeyVerificationInput);
		return returnCodesInput;
	}

}
