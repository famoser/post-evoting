/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.application.exception.ResourceNotFoundException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * The Class ControlComponentKeysAccessorService. It is used to download the control component keys from the secure data manager database and write
 * them to the secure data manager configuration area, to facilitate subsequent verification processes.
 */
@Service
public class ControlComponentKeysAccessorService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ControlComponentKeysAccessorService.class);

	@Autowired
	private ElectoralAuthorityRepository electoralAuthorityRepository;

	@Autowired
	private VotingCardSetRepository votingCardSetRepository;

	@Autowired
	private PathResolver pathResolver;

	/**
	 * Downloads the control component mixing keys, for the provided electoral authority, from the secure data manager database.
	 *
	 * @param electoralAuthorityId the electoral authority ID.
	 * @return the JSON array containing the mixing keys.
	 * @throws ResourceNotFoundException if electoral authority cannot be found.
	 */
	public JsonArray downloadMixingKeys(String electoralAuthorityId) throws ResourceNotFoundException {
		LOGGER.info("Downloading control component mixing keys for electoral authority {} from SDM database.", electoralAuthorityId);

		String electoralAuthorityJsonStr = electoralAuthorityRepository.find(electoralAuthorityId);

		if (electoralAuthorityJsonStr.isEmpty() || JsonConstants.EMPTY_OBJECT.equals(electoralAuthorityJsonStr)) {
			throw new ResourceNotFoundException("Electoral Authority with ID " + electoralAuthorityId + " could not be found in SDM database.");
		}

		JsonObject electoralAuthorityJsonObj = JsonUtils.getJsonObject(electoralAuthorityJsonStr);

		String mixingKeysJsonArrayStr = electoralAuthorityJsonObj.getString(Constants.MIX_DEC_KEY_LABEL);

		return JsonUtils.getJsonArray(mixingKeysJsonArrayStr);
	}

	/**
	 * Writes the control component mixing keys, for the provided election event and electoral authority, to the secure data manager configuration
	 * area.
	 *
	 * @param electionEventId      the electoral event ID.
	 * @param electoralAuthorityId the electoral authority ID.
	 * @param mixingKeysJsonArray  the JSON array containing the mixing keys.
	 * @throws IOException if a file write operation fails.
	 */
	public void writeMixingKeys(String electionEventId, String electoralAuthorityId, JsonArray mixingKeysJsonArray) throws IOException {
		LOGGER.info("Writing control component mixing keys for election event {} and electoral authority {} to SDM configuration area.",
				electionEventId, electoralAuthorityId);

		Path electoralAuthorityPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_ELECTORAL_AUTHORITY, electoralAuthorityId);
		Files.createDirectories(electoralAuthorityPath);

		Path mixingKeysJsonArrayPath = pathResolver.resolve(electoralAuthorityPath.toString(), Constants.MIX_DEC_KEYS_JSON);

		String mixingKeysJsonArrayStr = mixingKeysJsonArray.toString();

		Files.write(mixingKeysJsonArrayPath, mixingKeysJsonArrayStr.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Downloads the control component choice code keys, for the provided voting card set, from the secure data manager database.
	 *
	 * @param votingCardSetId the voting card set ID.
	 * @return the JSON array containing the choice code keys.
	 * @throws ResourceNotFoundException if voting card set cannot be found.
	 */
	public JsonArray downloadChoiceCodeKeys(String votingCardSetId) throws ResourceNotFoundException {
		LOGGER.info("Downloading control component choice code keys for voting card set {} from SDM database.", votingCardSetId);

		String votingCardSetJsonStr = votingCardSetRepository.find(votingCardSetId);

		if (votingCardSetJsonStr.isEmpty() || JsonConstants.EMPTY_OBJECT.equals(votingCardSetJsonStr)) {
			throw new ResourceNotFoundException("Voting card set with ID " + votingCardSetId + " could not be found in SDM database.");
		}

		JsonObject votingCardSetJsonObj = JsonUtils.getJsonObject(votingCardSetJsonStr);

		String choiceCodeKeysJsonArrayStr = votingCardSetJsonObj.getString(JsonConstants.CHOICE_CODES_ENCRYPTION_KEY);

		return JsonUtils.getJsonArray(choiceCodeKeysJsonArrayStr);
	}

	/**
	 * Writes the control component choice code keys, for the provided election event and verification card set, to the secure data manager
	 * configuration area.
	 *
	 * @param electionEventId         the electoral event ID.
	 * @param verificationCardSetId   the verification card set ID.
	 * @param choiceCodeKeysJsonArray the JSON array containing the choice code keys.
	 * @throws IOException if a file write operation fails.
	 */
	public void writeChoiceCodeKeys(String electionEventId, String verificationCardSetId, JsonArray choiceCodeKeysJsonArray) throws IOException {
		LOGGER.info("Writing control component choice code keys for election event " + electionEventId + " and verification card set "
				+ verificationCardSetId + " to SDM configuration area.");

		Path verificationCardSetPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
				Constants.CONFIG_DIR_NAME_VOTERVERIFICATION, verificationCardSetId);
		Files.createDirectories(verificationCardSetPath);

		Path choiceCodeKeysJsonArrayPath = pathResolver.resolve(verificationCardSetPath.toString(), Constants.CHOICE_CODES_ENCRYPTION_KEYS_JSON);

		String choiceCodeKeysJsonArrayStr = choiceCodeKeysJsonArray.toString();

		Files.write(choiceCodeKeysJsonArrayPath, choiceCodeKeysJsonArrayStr.getBytes(StandardCharsets.UTF_8));
	}
}
