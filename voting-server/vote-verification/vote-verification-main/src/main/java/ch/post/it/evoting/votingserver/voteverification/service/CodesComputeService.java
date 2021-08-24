/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;
import ch.post.it.evoting.domain.returncodes.ReturnCodesExponentiationResponsePayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.platform.PlatformCARepository;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode.CodesComputeResults;
import ch.post.it.evoting.votingserver.voteverification.domain.model.platform.VvPlatformCARepository;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.remote.OrchestratorClient;

import okhttp3.ResponseBody;

/**
 * Computes pre-Choice Return Codes or the pre-Vote Cast Return Code
 */
@Stateless
public class CodesComputeService {

	static final String CC_ORCHESTRATOR_PATH = "choicecodes";

	private static final Logger LOGGER = LoggerFactory.getLogger(CodesComputeService.class);

	@Inject
	private OrchestratorClient ccOrchestratorClient;

	@Inject
	private TrackIdInstance trackId;

	@Inject
	private PayloadVerifier payloadVerifier;

	@Inject
	@VvPlatformCARepository
	private PlatformCARepository platformCARepository;

	private static Map<BigInteger, ZpGroupElement> combineChoiceCodeNodesComputeContributions(List<BigInteger> partialCodesElements,
			List<Map<BigInteger, BigInteger>> ccnContributions, ZpSubgroup zpSubgroup) {

		if (ccnContributions.isEmpty()) {
			LOGGER.error("Empty Choice Code Nodes compute contributions");
			throw new IllegalStateException("Choice Code Nodes did not return their compute contributions");
		}

		Map<BigInteger, ZpGroupElement> result = new HashMap<>();

		for (BigInteger partialCodeElement : partialCodesElements) {
			ZpGroupElement combinedContribution = zpSubgroup.getIdentity();
			for (Map<BigInteger, BigInteger> partialCodeElementToNodeContributionMap : ccnContributions) {
				try {
					ZpGroupElement nodeContribution = new ZpGroupElement(partialCodeElementToNodeContributionMap.get(partialCodeElement), zpSubgroup);
					combinedContribution = combinedContribution.multiply(nodeContribution);
				} catch (GeneralCryptoLibException e) {
					LOGGER.error("Error combining the Choice Code Nodes compute contributions");
					throw new IllegalStateException("Choice Code Nodes contributions could not be combined", e);
				}
			}
			result.put(partialCodeElement, combinedContribution);
		}

		return result;
	}

	/**
	 * Computes in collaboration with the control components the pre-Choice Return Codes or pre-Vote Cast Return Code.
	 *
	 * @param tenantId              - tenant identifier.
	 * @param eeId                  - election event identifier.
	 * @param verificationCardSetId - verification card set id
	 * @param verificationCardId    - verification card id
	 * @param zpSubgroup            - the mathematical group of the input codes
	 * @param returnCodesInput      - encrypted partial Choice Return Codes (for calculating the pre-Choice Return Codes) or the confirmation message
	 *                              (for calculating the pre-Vote Cast Return Code)
	 * @return a map with the pre-Choice Return Codes or the pre-Vote Cast Return Code
	 * @throws ResourceNotFoundException
	 * @throws CryptographicOperationException
	 */
	public CodesComputeResults computePartialCodes(String tenantId, String eeId, String verificationCardSetId, String verificationCardId,
			ZpSubgroup zpSubgroup, ReturnCodesInput returnCodesInput) throws ResourceNotFoundException, CryptographicOperationException {

		try {
			LOGGER.info("Requesting the choice code nodes compute contributions");

			List<ReturnCodesExponentiationResponsePayload> ccnContributions = collectChoiceCodeNodesComputeContributions(tenantId, eeId,
					verificationCardSetId, verificationCardId, returnCodesInput);

			return computePartialCodesFromComputeResult(zpSubgroup, returnCodesInput.getReturnCodesInputElements(), ccnContributions);
		} catch (GeneralCryptoLibException e) {
			// Partial Choice Codes consolidation -- Partial Choice Codes cannot be consolidated
			throw new CryptographicOperationException("Error computing partial choice codes:", e);
		} catch (IOException | ClassNotFoundException e) {
			throw new IllegalStateException("Error computing partial choice codes:", e);
		} catch (PayloadVerificationException e) {
			throw new IllegalStateException("Error verifying partial choice codes signatures:", e);
		}
	}

	public CodesComputeResults computePartialCodesFromComputeResult(ZpSubgroup zpSubgroup, List<BigInteger> partialCodesElements,
			List<ReturnCodesExponentiationResponsePayload> ccnContributions) {
		LOGGER.info("Combining the choice code nodes compute contributions");

		List<Map<BigInteger, BigInteger>> contributedPartialChoiceCodes = ccnContributions.stream()
				.map(ReturnCodesExponentiationResponsePayload::getPccOrCkToLongReturnCodeShare).collect(Collectors.toList());

		Map<BigInteger, ZpGroupElement> combinedPartialChoiceCodes = combineChoiceCodeNodesComputeContributions(partialCodesElements,
				contributedPartialChoiceCodes, zpSubgroup);

		return new CodesComputeResults(combinedPartialChoiceCodes, ccnContributions);
	}

	private List<ReturnCodesExponentiationResponsePayload> collectChoiceCodeNodesComputeContributions(String tenantId, String eeId,
			String verificationCardSetId, String verificationCardId, ReturnCodesInput returnCodesInput)
			throws GeneralCryptoLibException, ResourceNotFoundException, ClassNotFoundException, IOException, PayloadVerificationException {

		try (ResponseBody response = RetrofitConsumer.processResponse(ccOrchestratorClient
				.getChoiceCodeNodesComputeContributions(trackId.getTrackId(), CC_ORCHESTRATOR_PATH, tenantId, eeId, verificationCardSetId,
						verificationCardId, returnCodesInput))) {
			// Partial Choice Codes computation request -- Partial Choice Code computation correctly received

			@SuppressWarnings("unchecked")
			List<ReturnCodesExponentiationResponsePayload> choiceCodesVerificationResults = (List<ReturnCodesExponentiationResponsePayload>) new ObjectInputStream(
					response.byteStream()).readObject();

			verifySignatures(tenantId, eeId, verificationCardId, choiceCodesVerificationResults);

			return choiceCodesVerificationResults;
		}
	}

	private void verifySignatures(String tenantId, String eeId, String verificationCardId,
			List<ReturnCodesExponentiationResponsePayload> choiceCodesVerificationResults)
			throws GeneralCryptoLibException, ResourceNotFoundException, PayloadVerificationException {

		X509Certificate rootCertificate = (X509Certificate) PemUtils
				.certificateFromPem(platformCARepository.getRootCACertificate().getCertificateContent());

		for (ReturnCodesExponentiationResponsePayload choiceCodesComputationResult : choiceCodesVerificationResults) {
			if (!payloadVerifier.isValid(choiceCodesComputationResult, rootCertificate)) {
				throw new IllegalStateException(
						"Signature invalid for tenantId " + tenantId + " eeId " + eeId + " verificationCardId " + verificationCardId);
			}
		}
	}

}
