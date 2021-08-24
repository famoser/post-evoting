/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ch.post.it.evoting.sdm.application.exception.ChoiceCodesComputationServiceException;
import ch.post.it.evoting.sdm.domain.model.status.Status;
import ch.post.it.evoting.sdm.domain.model.votingcardset.VotingCardSetRepository;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.RestClientService;
import ch.post.it.evoting.sdm.infrastructure.cc.PayloadStorageException;
import ch.post.it.evoting.sdm.infrastructure.cc.ReturnCodeGenerationRequestPayloadRepository;
import ch.post.it.evoting.sdm.infrastructure.clients.OrchestratorClient;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

@Service
public class ChoiceCodesComputationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChoiceCodesComputationService.class);

	@Autowired
	private VotingCardSetRepository votingCardSetRepository;

	@Autowired
	private ReturnCodeGenerationRequestPayloadRepository returnCodeGenerationRequestPayloadRepository;

	@Value("${OR_URL}")
	private String orchestratorUrl;

	@Value("${tenantID}")
	private String tenantId;

	/**
	 * Check if the choice code contributions for generation are ready and update the status of the voting card set they belong to.
	 */
	public void updateChoiceCodesComputationStatus(final String electionEventId) throws IOException {
		Map<String, Object> votingCardSetsParams = new HashMap<>();
		votingCardSetsParams.put(JsonConstants.STATUS, Status.COMPUTING.name());
		votingCardSetsParams.put(JsonConstants.ELECTION_EVENT_DOT_ID, electionEventId);

		String votingCardSetJSON = votingCardSetRepository.list(votingCardSetsParams);
		JsonArray votingCardSets = new JsonParser().parse(votingCardSetJSON).getAsJsonObject().get(JsonConstants.RESULT).getAsJsonArray();

		for (int i = 0; i < votingCardSets.size(); i++) {
			JsonObject votingCardSetInArray = votingCardSets.get(i).getAsJsonObject();
			String verificationCardSetId = votingCardSetInArray.get(JsonConstants.VERIFICATION_CARD_SET_ID).getAsString();

			int chunkCount;
			try {
				chunkCount = returnCodeGenerationRequestPayloadRepository.getCount(electionEventId, verificationCardSetId);
			} catch (PayloadStorageException e) {
				throw new IllegalStateException(e);
			}

			Response<ResponseBody> choiceCodesComputationStatusResponse = executeCall(
					getOrchestratorClient().getChoiceCodesComputationStatus(tenantId, electionEventId, verificationCardSetId, chunkCount));

			if (!choiceCodesComputationStatusResponse.isSuccessful()) {
				final String errorBodyString;
				try {
					errorBodyString = choiceCodesComputationStatusResponse.errorBody().string();
				} catch (IOException e) {
					throw new UncheckedIOException("Failed to convert response body to string.", e);
				}
				throw new ChoiceCodesComputationServiceException(String.format("Request to orchestrator failed with error: %s", errorBodyString));
			}

			JsonObject jsonObject;
			try (ResponseBody body = choiceCodesComputationStatusResponse.body(); Reader reader = body.charStream()) {
				jsonObject = new JsonParser().parse(reader).getAsJsonObject();
			}

			votingCardSetInArray.addProperty(JsonConstants.STATUS, jsonObject.get("status").getAsString());
			votingCardSetRepository.update(votingCardSetInArray.toString());
		}
	}

	/**
	 * Gets the orchestrator Retrofit client
	 */
	private OrchestratorClient getOrchestratorClient() {
		Retrofit restAdapter = RestClientService.getInstance().getRestClientWithJacksonConverter(orchestratorUrl);
		return restAdapter.create(OrchestratorClient.class);
	}

	/**
	 * Testing-gate for static method mocking.
	 */
	<T> Response<T> executeCall(Call<T> call) throws IOException {
		return call.execute();
	}
}
