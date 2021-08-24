/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.utils.CryptographicOperationException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.returncode.VoterCodesService;
import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.election.VoteVerificationContextData;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.returncodes.ChoiceCodeAndComputeResults;
import ch.post.it.evoting.domain.returncodes.ChoiceCodesVerificationDecryptResPayload;
import ch.post.it.evoting.domain.returncodes.ComputeResults;
import ch.post.it.evoting.domain.returncodes.PartialChoiceReturnCodesVerificationInput;
import ch.post.it.evoting.domain.returncodes.ReturnCodesExponentiationResponsePayload;
import ch.post.it.evoting.domain.returncodes.ReturnCodesInput;
import ch.post.it.evoting.domain.returncodes.VoteAndComputeResults;
import ch.post.it.evoting.votingserver.commons.beans.exceptions.ResourceNotFoundException;
import ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode.CodesComputeResults;
import ch.post.it.evoting.votingserver.voteverification.domain.model.choicecode.CodesDecryptionResults;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKey;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.ElectionPublicKeyRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContent;
import ch.post.it.evoting.votingserver.voteverification.domain.model.content.VerificationContentRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.Verification;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verification.VerificationRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetEntity;
import ch.post.it.evoting.votingserver.voteverification.domain.model.verificationset.VerificationSetRepository;

/**
 * Generate the short choice return codes based on the encrypted partial choice return codes - in interaction with the control components.
 */
@Stateless
public class ChoiceCodesServiceImpl implements ChoiceCodesService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceCodesServiceImpl.class);

	@Inject
	private VerificationSetRepository verificationSetRepository;

	@Inject
	private ElectionPublicKeyRepository electionPublicKeyRepository;

	@Inject
	private VerificationContentRepository verificationContentRepository;

	@Inject
	private VerificationRepository verificationRepository;

	@Inject
	private CodesDecrypterService codesDecrypterService;

	@Inject
	private CodesComputeService codesComputeService;

	@Inject
	private VoterCodesService voterCodesService;

	@Inject
	private ShortCodesService shortCodesService;

	@Inject
	private CorrectnessParser correctnessParser;

	@Override
	public ChoiceCodeAndComputeResults generateChoiceCodes(final String tenantId, final String electionEventId, final String verificationCardId,
			final VoteAndComputeResults voteAndComputeResults)
			throws ResourceNotFoundException, GeneralCryptoLibException, IOException, CryptographicOperationException {
		LOGGER.info("Generating choice codes for tenant: {}, election event: {} and verification card: {}.", tenantId, electionEventId,
				verificationCardId);

		Vote vote = voteAndComputeResults.getVote();
		ZpSubgroup zpSubgroup = getZpSubgroupFromVote(vote);

		String[] encryptedPartialCodesAsString = vote.getEncryptedPartialChoiceCodes().split(";");
		List<BigInteger> encryptedPartialCodes = new ArrayList<>();
		List<ZpGroupElement> encryptedPartialCodesAsElements = new ArrayList<>();
		for (String encryptedPcc : encryptedPartialCodesAsString) {
			BigInteger partialCodeAsBigInteger = new BigInteger(encryptedPcc);
			encryptedPartialCodes.add(partialCodeAsBigInteger);
			encryptedPartialCodesAsElements.add(new ZpGroupElement(partialCodeAsBigInteger, zpSubgroup));
		}

		ReturnCodesInput encryptedReturnCodesInput = createVerificationPayload(Collections.singletonList(encryptedPartialCodes.get(0)), vote);

		encryptedReturnCodesInput.setCertificates(voteAndComputeResults.getCredentialInfoCertificates());

		Verification verification = verificationRepository
				.findByTenantIdElectionEventIdVerificationCardId(tenantId, electionEventId, verificationCardId);
		String verificationCardSetId = verification.getVerificationCardSetId();

		ComputeResults computeAndDecryptResults = voteAndComputeResults.getComputeResults();

		CodesDecryptionResults decryptionResult;

		if (computeAndDecryptResults == null || computeAndDecryptResults.getDecryptionResults() == null || computeAndDecryptResults
				.getDecryptionResults().isEmpty()) {

			decryptionResult = codesDecrypterService
					.decryptPartialCodes(tenantId, electionEventId, verificationCardSetId, verificationCardId, encryptedPartialCodesAsElements,
							encryptedReturnCodesInput);

		} else {
			String decyptionResultsString = computeAndDecryptResults.getDecryptionResults();
			List<ChoiceCodesVerificationDecryptResPayload> ccnContributions = ObjectMappers
					.fromJson(decyptionResultsString, new TypeReference<List<ChoiceCodesVerificationDecryptResPayload>>() {
					});
			decryptionResult = codesDecrypterService.decryptPartialCodesFromDecryptResult(encryptedPartialCodesAsElements, ccnContributions);
		}

		List<BigInteger> toComputePartialCodesList = decryptionResult.getCombinedZpGroupElementLists().stream().map(ZpGroupElement::getValue)
				.collect(Collectors.toList());

		ReturnCodesInput toComputeReturnCodesInput = new ReturnCodesInput();
		toComputeReturnCodesInput.setReturnCodesInputElements(toComputePartialCodesList);

		CodesComputeResults computationResults;

		if (computeAndDecryptResults == null || computeAndDecryptResults.getComputationResults() == null || computeAndDecryptResults
				.getComputationResults().isEmpty()) {

			computationResults = codesComputeService
					.computePartialCodes(tenantId, electionEventId, verificationCardSetId, verificationCardId, zpSubgroup, toComputeReturnCodesInput);

		} else {
			String computationResultsString = computeAndDecryptResults.getComputationResults();
			List<ReturnCodesExponentiationResponsePayload> ccnContributions = ObjectMappers
					.fromJson(computationResultsString, new TypeReference<List<ReturnCodesExponentiationResponsePayload>>() {
					});
			computationResults = codesComputeService.computePartialCodesFromComputeResult(zpSubgroup, toComputePartialCodesList, ccnContributions);
		}

		Map<BigInteger, ZpGroupElement> optionsToComputedPartialCodes = computationResults.getCombinedPartialChoiceCodes();

		List<ZpGroupElement> partialCodesList = toComputePartialCodesList.stream().map(optionsToComputedPartialCodes::get)
				.collect(Collectors.toList());

		// Partial Choice Codes consolidation -- Partial Choice Codes correctly consolidated
		final List<List<String>> orderedListOfCorrectnessIds = correctnessParser.parse(vote.getCorrectnessIds());
		final List<byte[]> longCodes = new ArrayList<>();
		for (int i = 0; i < partialCodesList.size(); i++) {
			final ZpGroupElement partialCode = partialCodesList.get(i);
			final List<String> correctnessIds = orderedListOfCorrectnessIds.get(i);
			longCodes.add(voterCodesService.generateLongReturnCode(electionEventId, verificationCardId, partialCode, correctnessIds));
		}

		List<String> shortCodes = shortCodesService.retrieveShortCodes(tenantId, electionEventId, verificationCardId, longCodes);

		String serializedShortCodes = StringUtils.join(shortCodes, ';');

		ChoiceCodeAndComputeResults choiceCodes = new ChoiceCodeAndComputeResults();
		choiceCodes.setChoiceCodes(serializedShortCodes);
		try {
			choiceCodes.setComputationResults(ObjectMappers.toJson(computationResults.getComputationResults()));
			choiceCodes.setDecryptionResults(ObjectMappers.toJson(decryptionResult.getDecryptResult()));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Could not serialize choice codes computation results", e);
		}

		return choiceCodes;
	}

	private ZpSubgroup getZpSubgroupFromVote(Vote vote) throws ResourceNotFoundException, GeneralCryptoLibException, IOException {

		VerificationContent verificationContent;

		try {
			Verification verification = verificationRepository
					.findByTenantIdElectionEventIdVerificationCardId(vote.getTenantId(), vote.getElectionEventId(), vote.getVerificationCardId());
			verificationContent = verificationContentRepository
					.findByTenantIdElectionEventIdVerificationCardSetId(vote.getTenantId(), vote.getElectionEventId(),
							verification.getVerificationCardSetId());
		} catch (ResourceNotFoundException e) {
			LOGGER.error("Error creating ZpSubgroup from Vote.");
			throw e;
		}

		ZpSubgroup mathematicalGroup;
		try {
			VoteVerificationContextData voteVerificationContextData = ObjectMappers
					.fromJson(verificationContent.getJson(), VoteVerificationContextData.class);
			EncryptionParameters encryptionParameters = voteVerificationContextData.getEncryptionParameters();
			String generatorEncryptParam = encryptionParameters.getG();
			String pEncryptParam = encryptionParameters.getP();
			String qEncryptParam = encryptionParameters.getQ();

			BigInteger generatorEncryptParamBigInteger = new BigInteger(generatorEncryptParam);
			BigInteger pEncryptParamBigInteger = new BigInteger(pEncryptParam);
			BigInteger qEncryptParamBigInteger = new BigInteger(qEncryptParam);
			mathematicalGroup = new ZpSubgroup(generatorEncryptParamBigInteger, pEncryptParamBigInteger, qEncryptParamBigInteger);
		} catch (GeneralCryptoLibException | NumberFormatException | IOException e) {
			LOGGER.error("Error creating ZpSubgroup from Vote.");
			throw e;
		}

		return mathematicalGroup;
	}

	private ReturnCodesInput createVerificationPayload(List<BigInteger> partialCodesElements, Vote vote) throws ResourceNotFoundException {

		try {
			Verification verification = verificationRepository
					.findByTenantIdElectionEventIdVerificationCardId(vote.getTenantId(), vote.getElectionEventId(), vote.getVerificationCardId());

			VerificationContent verificationContent = verificationContentRepository
					.findByTenantIdElectionEventIdVerificationCardSetId(vote.getTenantId(), vote.getElectionEventId(),
							verification.getVerificationCardSetId());

			VerificationSetEntity verificationSetEntity = verificationSetRepository
					.findByTenantIdElectionEventIdVerificationCardSetId(vote.getTenantId(), vote.getElectionEventId(),
							verification.getVerificationCardSetId());

			VoteVerificationContextData voteVerificationContextData = ObjectMappers
					.fromJson(verificationContent.getJson(), VoteVerificationContextData.class);

			String electoralAuthorityId = voteVerificationContextData.getElectoralAuthorityId();

			ElectionPublicKey electionPublicKey = electionPublicKeyRepository
					.findByTenantIdElectionEventIdElectoralAuthorityId(vote.getTenantId(), vote.getElectionEventId(), electoralAuthorityId);

			PartialChoiceReturnCodesVerificationInput partialChoiceReturnCodesVerificationInput = new PartialChoiceReturnCodesVerificationInput();
			partialChoiceReturnCodesVerificationInput.setVote(ObjectMappers.toJson(vote));
			partialChoiceReturnCodesVerificationInput.setElectionPublicKeyJwt(electionPublicKey.getJwt());
			partialChoiceReturnCodesVerificationInput.setVerificationCardSetDataJwt(verificationSetEntity.getSignature());
			ReturnCodesInput returnCodesInput = new ReturnCodesInput();
			returnCodesInput.setPartialChoiceReturnCodesVerificationInput(partialChoiceReturnCodesVerificationInput);
			returnCodesInput.setReturnCodesInputElements(partialCodesElements);
			return returnCodesInput;
		} catch (IOException e) {
			throw new ResourceNotFoundException(e.getMessage(), e);
		}

	}
}
