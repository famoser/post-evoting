/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;

public class VotersSerializationDestProvider {

	private final Path voterMaterialOnlinePath;

	private final Path voteVerificationOnlinePath;

	private final Path printingOnlinePath;

	private final Path extendedAuthenticationPath;

	public VotersSerializationDestProvider(final Path voterMaterialOnlinePath, final Path voteVerificationOnlinePath, final Path printingOnlinePath,
			final Path extendedAuthenticationPath) {
		this.voterMaterialOnlinePath = voterMaterialOnlinePath;
		this.voteVerificationOnlinePath = voteVerificationOnlinePath;
		this.printingOnlinePath = printingOnlinePath;
		this.extendedAuthenticationPath = extendedAuthenticationPath;
	}

	private static boolean isNodeContributions(Path file) {
		String name = file.getFileName().toString();
		return name.startsWith(Constants.CONFIG_FILE_NAME_NODE_CONTRIBUTIONS) && name.endsWith(Constants.JSON);
	}

	public Path getTempVoterInformation(final String version) {
		return Paths.get(voterMaterialOnlinePath.toString(), Constants.CONFIG_FILE_NAME_VOTER_INFORMATION + version + Constants.CSV);
	}

	public Path getTempCredentialsData(final String version) {
		return Paths.get(voterMaterialOnlinePath.toString(), Constants.CONFIG_FILE_NAME_CREDENTIAL_DATA + version + Constants.CSV);
	}

	public Path getTempVerificationCardSetData(final String version) {
		return Paths.get(voteVerificationOnlinePath.toString(), Constants.VERIFICATION_CARD_SET_DATA + version + Constants.JSON);
	}

	public Path getTempVoteVerificationContextData(final String version) {
		return Paths.get(voteVerificationOnlinePath.toString(), Constants.VOTE_VERIFICATION_CONTEXT_DATA + version + Constants.JSON);
	}

	public Path getTempVerificationCardData(final String version) {
		return Paths.get(voteVerificationOnlinePath.toString(), Constants.CONFIG_FILE_VERIFICATION_CARD_DATA + version + Constants.CSV);
	}

	public Path getTempCodesMappingTablesContextData(final String version) {
		return Paths.get(voteVerificationOnlinePath.toString(), Constants.CONFIG_FILE_NAME_CODES_MAPPING + version + Constants.CSV);
	}

	/**
	 * Obtain a map of paths, where each key in the map represents the path of file containing the output data from the compute operation, and each
	 * value in the map represents the path of the file containing the inputs for the compute operation.
	 * <p>
	 * Note: if there are multiple "chunks", then there will be multiple pairs of input and output files for a single verification card set.
	 *
	 * @return a list of input and output paths.
	 */
	public List<NodeContributionsPath> getNodeContributions() {

		List<NodeContributionsPath> returnList = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(voteVerificationOnlinePath)) {

			for (Path path : stream) {

				if (isNodeContributions(path)) {

					String nodeContributionsFileName = path.getFileName().toString();
					String choiceCodeGenerationRequestFileName = constructChoiceCodeGenerationRequestFileName(nodeContributionsFileName);

					Path pathOutputFile = Paths.get(voteVerificationOnlinePath.toAbsolutePath().toString(), nodeContributionsFileName);
					Path pathInputFile = Paths.get(voteVerificationOnlinePath.toAbsolutePath().toString(), choiceCodeGenerationRequestFileName);
					returnList.add(new NodeContributionsPath(pathInputFile, pathOutputFile));
				}
			}

		} catch (IOException e) {
			throw new CreateVotingCardSetException(
					String.format("Exception while trying to read from the directory %s", voteVerificationOnlinePath.toAbsolutePath().toString()), e);
		}

		return returnList;
	}

	/**
	 * Constructs the name of the file that contains the data that was sent from the SDM to the channel when requesting computations from the control
	 * components. Uses the file that contains the returned data to obtain the chunk ID. Assumes that the names of these files match the following
	 * format:
	 * <ul>
	 * <li>nodeContributions.CHUNK_ID.json</li>
	 * <li>choiceCodeGenerationRequestPayload.CHUNK_ID.json</li>
	 * </ul>
	 *
	 * @param nodeContributionsFileName the name of the file that contains the data returned by the channel.
	 * @return the constructed filename.
	 */
	private String constructChoiceCodeGenerationRequestFileName(String nodeContributionsFileName) {
		String chunkId = nodeContributionsFileName.substring(nodeContributionsFileName.indexOf(Constants.SEPARATOR_BEFORE_CHUNK_ID) + 1,
				nodeContributionsFileName.lastIndexOf(Constants.JSON));
		return Constants.CHOICE_CODES_GENERATION_REQUEST_FILENAME + Constants.SEPARATOR_BEFORE_CHUNK_ID + chunkId + Constants.JSON;
	}

	public Path getTempDerivedKeys(final String version) {
		return Paths.get(voteVerificationOnlinePath.toString(), Constants.CONFIG_FILE_NAME_DERIVED_KEYS + version + Constants.CSV);
	}

	public Path getTempPrintingData(final String version) {
		return Paths.get(printingOnlinePath.toString(), Constants.PRINTING_DATA + version + Constants.CSV);
	}

	public Path getTempExtendedAuth(final String version) {
		return Paths.get(extendedAuthenticationPath.toString(), Constants.EXTENDED_AUTHENTICATION + version + Constants.CSV);
	}

	public Path getProvidedChallenge(final String version) {
		return Paths.get(printingOnlinePath.toString(), Constants.PROVIDED_CHALLENGE + version + Constants.CSV);
	}

	public Path getVoterInformation() {
		return getTempVoterInformation(Constants.EMPTY);
	}

	public Path getCredentialsData() {
		return getTempCredentialsData(Constants.EMPTY);
	}

	public Path getVerificationCardSetData() {
		return getTempVerificationCardSetData(Constants.EMPTY);
	}

	public Path getVoteVerificationContextData() {
		return getTempVoteVerificationContextData(Constants.EMPTY);
	}

	public Path getVerificationCardData() {
		return getTempVerificationCardData(Constants.EMPTY);
	}

	public Path getCodesMappingTablesContextData() {
		return getTempCodesMappingTablesContextData(Constants.EMPTY);
	}

	public Path getDerivedKeys() {
		return getTempDerivedKeys(Constants.EMPTY);
	}

	public Path getProvidedChallenge() {
		return getProvidedChallenge(Constants.EMPTY);
	}
}
