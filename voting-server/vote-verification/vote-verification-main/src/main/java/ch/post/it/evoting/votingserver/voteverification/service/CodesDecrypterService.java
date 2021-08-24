/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.proofs.ProofsServiceAPI;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.certificates.utils.PemUtils;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.proofs.cryptoapi.ProofVerifierAPI;
import ch.post.it.evoting.cryptolib.proofs.proof.Proof;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.domain.election.model.messaging.PayloadVerificationException;
import ch.post.it.evoting.domain.election.payload.verify.PayloadVerifier;
import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.commons.domain.model.platform.PlatformCARepository;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitConsumer;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode.CodesDecryptionResults;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContent;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContentRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.platform.VvPlatformCARepository;
import ch.post.it.evoting.votingserver.voteverification.infrastructure.remote.OrchestratorClient;

import okhttp3.ResponseBody;

/**
 * Decrypt encrypted partial choice codes
 */
@Stateless
public class CodesDecrypterService {

	static final String CC_ORCHESTRATOR_PATH = "choicecodes";

	private static final Logger LOGGER = LoggerFactory.getLogger(CodesDecrypterService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();
	@Inject
	private OrchestratorClient ccOrchestratorClient;
	@Inject
	private TrackIdInstance trackId;
	@Inject
	private ProofsServiceAPI proofsService;
	@Inject
	private VerificationContentRepository verificationContentRepository;
	@Inject
	private GroupElementsCompressor<ZpGroupElement> groupElementsCompressor;
	@Inject
	private PayloadVerifier payloadVerifier;
	@Inject
	@VvPlatformCARepository
	private PlatformCARepository platformCARepository;

	/**
	 * Removes encryption from the encrypted choice codes received.
	 *
	 * @param tenantId              - tenant identifier.
	 * @param eeId                  - election event identifier.
	 * @param verificationCardSetId - verification card set id
	 * @param verificationCardId    - verification card id
	 * @param encryptedPartialCodes - encryptionPartial codes of the vote
	 * @return combined decrypted partial codes and all decryption results
	 * @throws ResourceNotFoundException
	 * @throws CryptographicOperationException
	 */
	public CodesDecryptionResults decryptPartialCodes(String tenantId, String eeId, String verificationCardSetId, String verificationCardId,
			List<ZpGroupElement> encryptedPartialCodes, ReturnCodesInput encryptedReturnCodesInput)
			throws ResourceNotFoundException, CryptographicOperationException {

		try {

			LOGGER.info("Obtaining the necessary data to decrypt the choice codes for tenant: {}, election event: {} " + "and verification card: {}.",
					tenantId, eeId, verificationCardId);

			LOGGER.info("Requesting the choice code nodes decryption contributions");

			List<ChoiceCodesVerificationDecryptResPayload> decryptResult = collectChoiceCodeNodesDecryptionContributions(tenantId, eeId,
					verificationCardSetId, verificationCardId, encryptedReturnCodesInput);

			return decryptPartialCodesFromDecryptResult(encryptedPartialCodes, decryptResult);
		} catch (GeneralCryptoLibException e) {
			throw new CryptographicOperationException("Error decrypting partial choice codes:", e);
		} catch (PayloadVerificationException e) {
			throw new IllegalStateException("Error verifying partial choice codes signatures:", e);
		} catch (IOException e) {
			throw new IllegalStateException("Error retrieving decrypted partial choice codes:", e);
		}
	}

	/**
	 * Removes encryption from the encrypted choice codes received.
	 *
	 * @param encryptedPartialCodes - encryptionPartial codes of the vote
	 * @param decryptResult         - the result of the partial choice codes nodes decryption
	 * @return combined decrypted partial codes and all decryption results
	 * @throws CryptographicOperationException
	 */
	public CodesDecryptionResults decryptPartialCodesFromDecryptResult(List<ZpGroupElement> encryptedPartialCodes,
			List<ChoiceCodesVerificationDecryptResPayload> decryptResult) throws CryptographicOperationException {
		try {
			List<ZpGroupElement> phis = encryptedPartialCodes.subList(1, encryptedPartialCodes.size());

			List<List<ZpGroupElement>> ccnContributions = getCcnContributions(decryptResult);

			List<ZpGroupElement> gammasWithCCNContributions = combineChoiceCodeNodesDecryptionContributions(ccnContributions, phis.size());

			// Partial Decryption -- Partial Decryptions correctly consolidated
			LOGGER.info("Decrypting partial choice codes");

			List<ZpGroupElement> gammasforDecryption = gammasWithCCNContributions.stream().map(ZpGroupElement::invert).collect(Collectors.toList());

			List<ZpGroupElement> combinedZpGroupElementLists = combineZpGroupElementLists(gammasforDecryption, phis);
			return new CodesDecryptionResults(combinedZpGroupElementLists, decryptResult);
		} catch (GeneralCryptoLibException e) {
			throw new CryptographicOperationException("Error decrypting partial choice codes using the decrypt contributions:", e);
		}
	}

	private List<List<ZpGroupElement>> getCcnContributions(List<ChoiceCodesVerificationDecryptResPayload> decryptResult) {
		List<List<ZpGroupElement>> ccnContributions = new ArrayList<>();
		decryptResult.forEach(decryptResPayload -> {
			List<ZpGroupElement> castedGammas = decryptResPayload.getDecryptContributionResult().stream().map(gammaStr -> {
				try {
					return ZpGroupElement.fromJson(gammaStr);
				} catch (GeneralCryptoLibException e) {
					throw new IllegalStateException("Gamma in json format could not be casted to ZpGroupElement: " + e.getMessage());
				}
			}).collect(Collectors.toList());
			ccnContributions.add(castedGammas);
		});

		return ccnContributions;
	}

	private List<ChoiceCodesVerificationDecryptResPayload> collectChoiceCodeNodesDecryptionContributions(String tenantId, String eeId,
			String verificationCardSetId, String verificationCardId, ReturnCodesInput returnCodesInput)
			throws GeneralCryptoLibException, ResourceNotFoundException, PayloadVerificationException, IOException {

		VoteVerificationContextData voteVerificationContextData = getVoteVerificationContextData(tenantId, eeId, verificationCardSetId);

		try (ResponseBody response = RetrofitConsumer.processResponse(ccOrchestratorClient
				.getChoiceCodeNodesDecryptContributions(trackId.getTrackId(), CC_ORCHESTRATOR_PATH, tenantId, eeId, verificationCardSetId,
						verificationCardId, returnCodesInput))) {

			List<ChoiceCodesVerificationDecryptResPayload> decryptResult = objectMapper
					.readValue(response.byteStream(), new TypeReference<List<ChoiceCodesVerificationDecryptResPayload>>() {
					});

			// Get gamma from partial codes input
			ZpSubgroup group = getEncryptionParametersGroup(voteVerificationContextData);
			ZpGroupElement gamma = new ZpGroupElement(returnCodesInput.getReturnCodesInputElements().get(0), group);

			// Partial Decryption request -- Partial decryption correctly received

			verifySignatures(tenantId, eeId, verificationCardId, decryptResult);

			verifyProofs(tenantId, eeId, verificationCardSetId, verificationCardId, gamma, decryptResult);

			return decryptResult;
		}
	}

	private void verifySignatures(String tenantId, String eeId, String verificationCardId, List<ChoiceCodesVerificationDecryptResPayload> response)
			throws GeneralCryptoLibException, ResourceNotFoundException, PayloadVerificationException {

		X509Certificate rootCertificate = (X509Certificate) PemUtils
				.certificateFromPem(platformCARepository.getRootCACertificate().getCertificateContent());

		for (ChoiceCodesVerificationDecryptResPayload choiceCodesComputationResult : response) {
			if (!payloadVerifier.isValid(choiceCodesComputationResult, rootCertificate)) {
				throw new IllegalStateException(
						String.format("Signature invalid for tenantId %s eeId %s verificationCardId %s", tenantId, eeId, verificationCardId));
			}
		}
	}

	private void verifyProofs(String tenantId, String eeId, String verificationCardSetId, String verificationCardId, ZpGroupElement gamma,
			List<ChoiceCodesVerificationDecryptResPayload> decryptResponses) throws GeneralCryptoLibException, ResourceNotFoundException {

		VoteVerificationContextData voteVerificationContextData = getVoteVerificationContextData(tenantId, eeId, verificationCardSetId);

		List<ElGamalPublicKey> commitmentPublicKeys = getCommitmentPublicKeys(voteVerificationContextData);

		for (ChoiceCodesVerificationDecryptResPayload choiceCodesDecryptResult : decryptResponses) {

			ElGamalPublicKey resultPublicKey = ElGamalPublicKey.fromJson(choiceCodesDecryptResult.getPublicKeyJson());

			if (!commitmentPublicKeys.remove(resultPublicKey)) {
				throw new IllegalStateException(String.format(
						"Resulting proof public key was not valid for tenantId %s eeId %s verificationCardSetId %s verificationCardId %s", tenantId,
						eeId, verificationCardSetId, verificationCardId));
			}

			List<String> exponentiatedGammaStrings = choiceCodesDecryptResult.getDecryptContributionResult();
			List<ZpGroupElement> exponentiatedGammas = new ArrayList<>(exponentiatedGammaStrings.size());
			for (String exponentiatedGammaString : exponentiatedGammaStrings) {
				ZpGroupElement exponentiatedGamma = ZpGroupElement.fromJson(exponentiatedGammaString);
				exponentiatedGammas.add(exponentiatedGamma);
			}

			ZpGroupElement compressedPublicKeys = groupElementsCompressor.compress(resultPublicKey.getKeys());
			ZpGroupElement compressedExponentiatedGamma = groupElementsCompressor.compress(exponentiatedGammas);

			ZpSubgroup encryptionParametersGroup = getEncryptionParametersGroup(voteVerificationContextData);

			List<ZpGroupElement> exponentiatedElements = Arrays.asList(compressedPublicKeys, compressedExponentiatedGamma);
			List<ZpGroupElement> baseElements = Arrays.asList(encryptionParametersGroup.getGenerator(), gamma);

			Proof proof = Proof.fromJson(choiceCodesDecryptResult.getExponentiationProofJson());

			ProofVerifierAPI proofsVerifier = proofsService.createProofVerifierAPI(encryptionParametersGroup);

			if (!proofsVerifier.verifyExponentiationProof(exponentiatedElements, baseElements, proof)) {
				// Partial Decryption consolidation -- Proof not valid
				throw new IllegalStateException(
						String.format("Proof invalid for tenantId %s eeId %s verificationCardSetId %s verificationCardId %s", tenantId, eeId,
								verificationCardSetId, verificationCardId));
			}
		}

		if (!commitmentPublicKeys.isEmpty()) {
			throw new IllegalStateException("Resulting proof set did not contain all public keys expected for tenantId " + tenantId + " eeId " + eeId
					+ " verificationCardSetId " + verificationCardSetId + " verificationCardId " + verificationCardId);
		}
	}

	private List<ElGamalPublicKey> getCommitmentPublicKeys(VoteVerificationContextData voteVerificationContextData) throws GeneralCryptoLibException {
		String[] commitmentPublicKeyBase64Strings = voteVerificationContextData.getNonCombinedChoiceCodesEncryptionPublicKeys().split(";");
		List<ElGamalPublicKey> commitmentPublicKeys = new ArrayList<>(commitmentPublicKeyBase64Strings.length);
		for (String commitmentPublicKeyBase64String : commitmentPublicKeyBase64Strings) {
			String commitmentPublicKeyString = new String(
					Base64.getDecoder().decode(commitmentPublicKeyBase64String.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
			ElGamalPublicKey commitmentPublicKey = ElGamalPublicKey.fromJson(commitmentPublicKeyString);
			commitmentPublicKeys.add(commitmentPublicKey);
		}
		return commitmentPublicKeys;
	}

	private VoteVerificationContextData getVoteVerificationContextData(String tenantId, String eeId, String verificationCardSetId)
			throws ResourceNotFoundException {
		VerificationContent verificationContent = verificationContentRepository
				.findByTenantIdElectionEventIdVerificationCardSetId(tenantId, eeId, verificationCardSetId);

		VoteVerificationContextData voteVerificationContextData;
		try {
			voteVerificationContextData = ObjectMappers.fromJson(verificationContent.getJson(), VoteVerificationContextData.class);
		} catch (IOException e) {
			throw new IllegalStateException(
					"Verification context data was not valid for tenantId " + tenantId + " eeId " + eeId + " verificationCardSetId "
							+ verificationCardSetId, e);
		}
		return voteVerificationContextData;
	}

	private ZpSubgroup getEncryptionParametersGroup(VoteVerificationContextData voteVerificationContextData) throws GeneralCryptoLibException {
		EncryptionParameters encryptionParameters = voteVerificationContextData.getEncryptionParameters();
		String generatorEncryptParam = encryptionParameters.getG();
		String pEncryptParam = encryptionParameters.getP();
		String qEncryptParam = encryptionParameters.getQ();

		BigInteger generatorEncryptParamBigInteger = new BigInteger(generatorEncryptParam);
		BigInteger pEncryptParamBigInteger = new BigInteger(pEncryptParam);
		BigInteger qEncryptParamBigInteger = new BigInteger(qEncryptParam);
		return new ZpSubgroup(generatorEncryptParamBigInteger, pEncryptParamBigInteger, qEncryptParamBigInteger);
	}

	private List<ZpGroupElement> combineChoiceCodeNodesDecryptionContributions(List<List<ZpGroupElement>> ccnContributions, int numRequiredElements)
			throws GeneralCryptoLibException {

		LOGGER.info("Combining choice code nodes contributions for the decryption");

		Optional<List<ZpGroupElement>> result = ccnContributions.stream().reduce(this::combineZpGroupElementLists);

		if (!result.isPresent()) {
			LOGGER.error("Error combining the Choice Code Nodes contributions");
			throw new IllegalStateException("Choice Code Nodes did not return their contributions");
		}
		return compressZpGroupElementListIfNecessary(result.get(), numRequiredElements);
	}

	/**
	 * Combines (multiplies its elements) two lists of ZpGroupElement. It is required that both input ZpGroupElement lists have the same length,
	 * otherwise an IllegalStateException is thrown
	 *
	 * @param zpgel1
	 * @param zpgel2
	 * @return the list of combined (multiplied) ZpGroupElements
	 */
	private List<ZpGroupElement> combineZpGroupElementLists(List<ZpGroupElement> zpgel1, List<ZpGroupElement> zpgel2) {

		LOGGER.info("Combining ZpGroupElement lists");

		if (zpgel1.size() != zpgel2.size()) {
			LOGGER.error("ZpGroupElement lists have different number of elements. " + "Both lists need to have same size to combine them.");
			throw new IllegalStateException(
					"ZpGroupElement lists have different number of elements. " + "Both lists need to have same size to combine them.");
		}

		List<ZpGroupElement> result = new ArrayList<>(zpgel1.size());
		try {
			for (int i = 0; i < zpgel1.size(); i++) {
				result.add(zpgel1.get(i).multiply(zpgel2.get(i)));
			}
			return result;
		} catch (GeneralCryptoLibException e) {
			throw new IllegalStateException("ZpGroupElement lists could not be combined. " + e.getMessage(), e);
		}
	}

	/**
	 * Compress a list of ZpGroup elements if the size of the list is longer than the number of required messages. Otherwise return the same list.
	 *
	 * @param inputList           The list of group elements to be compressed
	 * @param numRequiredMessages The number of the required messages
	 * @return The result of the compression process.
	 * @throws GeneralCryptoLibException
	 */
	private List<ZpGroupElement> compressZpGroupElementListIfNecessary(final List<ZpGroupElement> inputList, final int numRequiredMessages)
			throws GeneralCryptoLibException {

		if (inputList.size() <= numRequiredMessages) {
			return inputList;
		}

		List<ZpGroupElement> untouchedElements = inputList.subList(0, numRequiredMessages - 1);

		List<ZpGroupElement> elementsToBeCombined = inputList.subList(numRequiredMessages - 1, inputList.size());

		GroupElementsCompressor<ZpGroupElement> grpElementsCompressor = new GroupElementsCompressor<>();

		ZpGroupElement compressedElements = grpElementsCompressor.compress(elementsToBeCombined);

		untouchedElements.add(compressedElements);

		return untouchedElements;
	}
}
