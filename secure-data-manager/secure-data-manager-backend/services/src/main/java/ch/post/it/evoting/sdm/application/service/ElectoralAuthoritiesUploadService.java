/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.application.exception.DatabaseException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityRepository;
import ch.post.it.evoting.sdm.domain.model.electoralauthority.ElectoralAuthorityUploadRepository;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.utils.JsonUtils;

/**
 * Service which uploads files to voter portal after creating the electoral authorities
 */
@Service
public class ElectoralAuthoritiesUploadService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElectoralAuthoritiesUploadService.class);

	private static final String CONSTANT_AUTHENTICATION_CONTEXT_DATA = "authenticationContextData";

	private static final String CONSTANT_ELECTION_PUBLIC_KEY = "electionPublicKey";

	private static final String CONSTANT_AUTHENTICATION_VOTER_DATA = "authenticationVoterData";

	private static final String NULL_ELECTION_EVENT_ID = "";

	@Autowired
	private ElectoralAuthorityRepository electoralAuthorityRepository;

	@Autowired
	private ElectoralAuthorityUploadRepository electoralAuthorityUploadRepository;

	@Autowired
	private PathResolver pathResolver;

	@Autowired
	private ElectionEventRepository electionEventRepository;

	/**
	 * Uploads the available electoral authorities texts to the voter portal.
	 */
	public void uploadSynchronizableElectoralAuthorities(String electionEvent) throws IOException {

		Map<String, Object> params = new HashMap<>();
		addSigned(params);
		addPendingToSynchronize(params);

		if (thereIsAnElectionEventIdAsAParameter(electionEvent)) {
			addElectionEventId(electionEvent, params);
		}

		String documents = electoralAuthorityRepository.list(params);
		JsonArray electoralAuthorities = JsonUtils.getJsonObject(documents).getJsonArray(JsonConstants.RESULT);

		for (int i = 0; i < electoralAuthorities.size(); i++) {

			JsonObject electoralAuthoritiesInArray = electoralAuthorities.getJsonObject(i);
			String electoralAuthorityId = electoralAuthoritiesInArray.getString(JsonConstants.ID);
			String electionEventId = electoralAuthoritiesInArray.getJsonObject(JsonConstants.ELECTION_EVENT).getString(JsonConstants.ID);

			JsonObject eEvent = JsonUtils.getJsonObject(electionEventRepository.find(electionEventId));
			JsonObject adminBoard = eEvent.getJsonObject(JsonConstants.ADMINISTRATION_AUTHORITY);

			String adminBoardId = adminBoard.getString(JsonConstants.ID);

			LOGGER.info("Uploading the signed authentication context configuration");
			boolean authResult = uploadAuthenticationContextData(electionEventId, adminBoardId);

			LOGGER.info("Uploading the signed election information context configuration");
			boolean eiResult = uploadElectionInformationData(electionEventId, adminBoardId);

			LOGGER.info("Uploading the signed election public key");
			boolean electionPublicKeyUploadResult = uploadElectionPublicKey(electionEventId, electoralAuthorityId, adminBoardId);

			JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
			if (authResult && eiResult && electionPublicKeyUploadResult) {

				LOGGER.info("Changing the status of the electoral authority");
				jsonObjectBuilder.add(JsonConstants.ID, electoralAuthorityId);
				jsonObjectBuilder.add(JsonConstants.SYNCHRONIZED, SynchronizeStatus.SYNCHRONIZED.getIsSynchronized().toString());
				jsonObjectBuilder.add(JsonConstants.DETAILS, SynchronizeStatus.SYNCHRONIZED.getStatus());
				LOGGER.info("The electoral authority was uploaded successfully");
			} else {
				LOGGER.error("An error occurred while uploading the signed electoral authority");
				jsonObjectBuilder.add(JsonConstants.ID, electoralAuthorityId);
				jsonObjectBuilder.add(JsonConstants.DETAILS, SynchronizeStatus.FAILED.getStatus());

			}

			try {
				electoralAuthorityRepository.update(jsonObjectBuilder.build().toString());
			} catch (DatabaseException ex) {
				LOGGER.error("An error occurred while updating the signed electoral authority", ex);
			}
		}

	}

	private void addElectionEventId(final String electionEvent, final Map<String, Object> params) {
		params.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEvent);
	}

	private boolean thereIsAnElectionEventIdAsAParameter(final String electionEvent) {
		return !NULL_ELECTION_EVENT_ID.equals(electionEvent);
	}

	private void addPendingToSynchronize(final Map<String, Object> params) {
		params.put(JsonConstants.SYNCHRONIZED, SynchronizeStatus.PENDING.getIsSynchronized().toString());
	}

	private void addSigned(final Map<String, Object> params) {
		params.put(JsonConstants.STATUS, Status.SIGNED.name());
	}

	private boolean uploadAuthenticationContextData(String electionEventId, final String adminBoardId) throws IOException {

		if (electoralAuthorityUploadRepository.checkEmptyElectionEventDataInAU(electionEventId)) {
			Path authenticationPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
					Constants.CONFIG_DIR_NAME_AUTHENTICATION);

			Path authenticationContextPath = pathResolver.resolve(authenticationPath.toString(), Constants.CONFIG_FILE_NAME_SIGNED_AUTH_CONTEXT_DATA);
			Path authenticationVoterDataPath = pathResolver.resolve(authenticationPath.toString(), Constants.CONFIG_FILE_NAME_SIGNED_AUTH_VOTER_DATA);
			JsonObject authenticationContexData = JsonUtils
					.getJsonObject(new String(Files.readAllBytes(authenticationContextPath), StandardCharsets.UTF_8));
			JsonObject authenticationVoterData = JsonUtils
					.getJsonObject(new String(Files.readAllBytes(authenticationVoterDataPath), StandardCharsets.UTF_8));
			JsonObject jsonInput = Json.createObjectBuilder().add(CONSTANT_AUTHENTICATION_CONTEXT_DATA, authenticationContexData.toString())
					.add(CONSTANT_AUTHENTICATION_VOTER_DATA, authenticationVoterData.toString()).build();

			return electoralAuthorityUploadRepository.uploadAuthenticationContextData(electionEventId, adminBoardId, jsonInput);
		}

		return true;
	}

	private boolean uploadElectionInformationData(String electionEventId, final String adminBoardId) throws IOException {

		if (electoralAuthorityUploadRepository.checkEmptyElectionEventDataInEI(electionEventId)) {
			Path electionInformationPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
					Constants.CONFIG_DIR_NAME_ELECTIONINFORMATION);

			Path electionInformationContextPath = pathResolver
					.resolve(electionInformationPath.toString(), Constants.CONFIG_FILE_NAME_SIGNED_ELECTION_INFORMATION_CONTENTS);

			byte[] electionInformationContextDataBytes = Files.readAllBytes(electionInformationContextPath);

			JsonObject electionInformationContextData = JsonUtils
					.getJsonObject(new String(electionInformationContextDataBytes, StandardCharsets.UTF_8));

			return electoralAuthorityUploadRepository
					.uploadElectionInformationContextData(electionEventId, adminBoardId, electionInformationContextData);

		}
		return true;
	}

	private boolean uploadElectionPublicKey(String electionEventId, String electoralAuthorityId, final String adminBoardId) throws IOException {

		JsonObject electionPublicKeyJSON = getSignedElectionPublicKeyJSON(electionEventId, electoralAuthorityId);
		JsonObject jsonInput = prepareSignedElectionPublicKeyToBeUploaded(electionPublicKeyJSON);

		boolean uploadEAtoVVResult = uploadElectionPublicKeyToVoteVerificationContext(electoralAuthorityId, electionEventId, adminBoardId, jsonInput);
		boolean uploadEAtoEIResult = uploadElectionPublicKeyToElectionInformationContext(electoralAuthorityId, electionEventId, adminBoardId,
				jsonInput);

		return uploadEAtoEIResult && uploadEAtoVVResult;
	}

	private JsonObject prepareSignedElectionPublicKeyToBeUploaded(final JsonObject electionPublicKey) {
		return Json.createObjectBuilder().add(CONSTANT_ELECTION_PUBLIC_KEY, electionPublicKey).build();
	}

	private JsonObject getSignedElectionPublicKeyJSON(final String electionEventId, final String electoralAuthorityId) throws IOException {

		Path signedElectionPublicKeyDataPath = pathResolver
				.resolve(Constants.CONFIG_FILES_BASE_DIR, electionEventId, Constants.CONFIG_DIR_NAME_ONLINE,
						Constants.CONFIG_DIR_NAME_ELECTORAL_AUTHORITY, electoralAuthorityId,
						Constants.CONFIG_FILE_NAME_SIGNED_ELECTION_PUBLIC_KEY_JSON);

		return JsonUtils.getJsonObject(new String(Files.readAllBytes(signedElectionPublicKeyDataPath), StandardCharsets.UTF_8));
	}

	private boolean uploadElectionPublicKeyToVoteVerificationContext(String electoralAuthorityId, String electionEventId, final String adminBoardId,
			JsonObject jsonInput) {

		return electoralAuthorityUploadRepository
				.uploadElectoralDataInVerificationContext(electionEventId, electoralAuthorityId, adminBoardId, jsonInput);
	}

	private boolean uploadElectionPublicKeyToElectionInformationContext(String electoralAuthorityId, String electionEventId,
			final String adminBoardId, JsonObject jsonInput) {

		return electoralAuthorityUploadRepository
				.uploadElectoralDataInElectionInformationContext(electionEventId, electoralAuthorityId, adminBoardId, jsonInput);
	}

}
