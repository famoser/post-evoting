/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static java.nio.file.Files.newInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.Contest;
import ch.post.it.evoting.domain.election.ElectionOption;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;

/**
 * The Class ConsistencyCheckService. It is used to verify trusted objects from a signed file against objects which are untrusted.
 */
@Service
public class ConsistencyCheckService {

	/**
	 * Checks that the JsonObject received from the ElectionEventJson has the same values of P, Q and G than the verified ones.
	 *
	 * @param encryptionParameters
	 * @param verifiedGroup
	 * @return
	 */
	public boolean encryptionParamsConsistent(JsonObject encryptionParameters, MathematicalGroup verifiedGroup) {
		String pAttribute = encryptionParameters.getString(JsonConstants.P);
		String qAttribute = encryptionParameters.getString(JsonConstants.Q);
		String gAttribute = encryptionParameters.getString(JsonConstants.G);

		return verifiedGroup.getP().equals(new BigInteger(pAttribute)) && verifiedGroup.getQ().equals(new BigInteger(qAttribute)) && verifiedGroup
				.getG().equals(new BigInteger(gAttribute));
	}

	/**
	 * Check consistency between the representations used on the ballot and the assigned to the election event on the verified representations file.
	 *
	 * @param verifiedRepresentationsFilePath
	 * @param json
	 * @return
	 * @throws IOException
	 */
	public boolean representationsConsistent(String json, Path verifiedRepresentationsFilePath) throws IOException {

		boolean consistent = true;
		Stream<String> representations;
		try (InputStream streamRepresentations = newInputStream(verifiedRepresentationsFilePath)) {

			// Get the set of representations
			representations = new BufferedReader(new InputStreamReader(streamRepresentations)).lines();
			Set<String> representationsSet = representations.collect(Collectors.toSet());

			// Extract the representations used on the Ballot
			Ballot ballotObject = new ObjectMapper().readValue(json, Ballot.class);

			// Validate the representations used are assigned to the electionEvent
			if (ballotObject.getContests() != null && !ballotObject.getContests().isEmpty()) {
				Set<String> representationsUsedForElectionEvent = ballotObject.getContests().stream().map(Contest::getOptions)
						.flatMap(Collection::stream).map(ElectionOption::getRepresentation).collect(Collectors.toSet());

				consistent = representationsSet.containsAll(representationsUsedForElectionEvent);
			}

			return consistent;
		}
	}
}
