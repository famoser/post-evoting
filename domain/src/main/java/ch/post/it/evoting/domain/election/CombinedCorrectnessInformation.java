/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import ch.post.it.evoting.cryptoprimitives.hashing.Hashable;
import ch.post.it.evoting.cryptoprimitives.hashing.HashableList;
import ch.post.it.evoting.domain.election.exceptions.CombinedCorrectnessInformationException;

public class CombinedCorrectnessInformation implements HashableList {

	@JsonProperty
	private final List<CorrectnessInformation> correctnessInformationList;

	// Corresponds to the variable 𝜓 - the number of voting options a voter can select.
	private Integer totalNumberOfSelections;

	// Corresponds to the variable n - the number of possible voting options.
	private Integer totalNumberOfVotingOptions;

	private Map<String, List<Integer>> correctnessIdToListOfSelectionsIndexesMap;
	private Map<String, List<Integer>> correctnessIdToListOfVotingOptionsIndexesMap;

	public CombinedCorrectnessInformation(final Ballot ballot) {
		checkNotNull(ballot, "The provided ballot is null.");

		this.correctnessInformationList = getCorrectnessInformationListFromBallot(ballot);

		initCombinedCorrectnessInformation();
	}

	@JsonCreator
	public CombinedCorrectnessInformation(
			@JsonProperty("correctnessInformationList")
			final List<CorrectnessInformation> correctnessInformationList) {
		checkNotNull(correctnessInformationList, "The provided correctnessInformationList is null.");

		this.correctnessInformationList = ImmutableList.copyOf(correctnessInformationList);

		initCombinedCorrectnessInformation();
	}

	/**
	 * Returns a map based on the given {@code correctnessInformationList} and the given {@code getIncrementFunction}.
	 * <p>
	 * For each correctness information entry in the list, we add an entry with :
	 * <ul>
	 *     <li>key: the correctness information's correctness id field.</li>
	 *     <li>value: a list of indexes going from [current index + 1] (inclusive) to [current index + increment] (inclusive).</li>
	 * </ul>
	 * When building the map, it iterates over the given {@code correctnessInformationList}, starting with a current index value of -1 and, after
	 * each map insertion, updating the current index to current index + increment.
	 * <p>
	 * Example for two correctness informations :
	 * <ul>
	 *     <li>Correctness information 1 : (correctnessId=1, increment=3)</li>
	 *     <li>Correctness information 2 : (correctnessId=2, increment=8)</li>
	 * </ul>
	 * Returned map would be :
	 * 	[
	 * 		(1, [0, 1, 2]),
	 * 		(2, [3, 4, 5, 6, 7, 8, 9, 10])
	 * 	].
	 *
	 * @param correctnessInformationList the list of correctness informations to process.
	 * @param getIncrementFunction       the function to get the increment for each correctness information.
	 * @return a new map based on the given {@code correctnessInformationList} and the given {@code getIncrementFunction}.
	 */
	private static Map<String, List<Integer>> getCorrectnessIdToListOfIndexesMap(final List<CorrectnessInformation> correctnessInformationList,
			final ToIntFunction<CorrectnessInformation> getIncrementFunction) {

		final Map<String, List<Integer>> correctnessIdToListOfIndexesMap = new HashMap<>();

		int currentIndex = -1;
		for (final CorrectnessInformation correctnessInformation : correctnessInformationList) {
			final int increment = getIncrementFunction.applyAsInt(correctnessInformation);
			final List<Integer> indexesList = IntStream.rangeClosed(currentIndex + 1, currentIndex + increment).boxed().collect(Collectors.toList());

			correctnessIdToListOfIndexesMap.put(correctnessInformation.getCorrectnessId(), indexesList);

			currentIndex += increment;
		}

		return correctnessIdToListOfIndexesMap;
	}

	private static Integer computeTotalNumberOfSelections(final List<CorrectnessInformation> correctnessInformationList) {
		return correctnessInformationList.stream().map(CorrectnessInformation::getNumberOfSelections).reduce(0, Integer::sum);
	}

	private static Integer computeTotalNumberOfVotingOptions(final List<CorrectnessInformation> correctnessInformationList) {
		return correctnessInformationList.stream().map(CorrectnessInformation::getNumberOfVotingOptions).reduce(0, Integer::sum);
	}

	private static List<CorrectnessInformation> getCorrectnessInformationListFromBallot(final Ballot ballot) {
		checkContestsNotNullAndNotEmpty(ballot.getContests(), ballot.getId());

		final List<CorrectnessInformation> correctnessInformationList = new ArrayList<>();

		for (final Contest contest : ballot.getContests()) {

			final String contestId = contest.getId();
			final List<Question> questions = contest.getQuestions();

			checkNotNullAndNotEmpty(questions, "questions", contestId);
			checkNotNullAndNotEmpty(contest.getAttributes(), "election attributes", contestId);
			checkNotNullAndNotEmpty(contest.getOptions(), "election options", contestId);

			for (final ElectionAttributes electionAttributes : contest.getAttributes()) {
				if (!electionAttributes.isCorrectness()) {
					continue;
				}

				final String correctnessId = electionAttributes.getId();
				final Question question = getCorrespondingQuestionByAttribute(questions, correctnessId, contestId);
				final Integer numberOfVotingOptions = getNumberOfVotingOptions(contest, correctnessId);

				correctnessInformationList.add(new CorrectnessInformation(correctnessId, question.getMax(), numberOfVotingOptions));
			}
		}

		return correctnessInformationList;

	}

	/**
	 * Computes the number of voting options for the given {@code correctnessId} within the given {@code contest}.
	 * <p>
	 * For example, a correctnessId might identify a specific question. This question has three associated voting options (called election options in
	 * the Ballot model): YES, NO, EMPTY. Therefore, the method should return 3 in this case. Another example would be a correctnessId identifying the
	 * selection of a list in an election. Imagine that the voter can choose between 20 different lists. In this case, the method should return 21 (20
	 * lists + the blank list).
	 *
	 * @param contest       the contest.
	 * @param correctnessId the correctness id.
	 * @return the computed number of voting options.
	 */
	private static Integer getNumberOfVotingOptions(final Contest contest, final String correctnessId) {

		// list of the related election attributes ids, ie list of election attributes ids whose field related (an array) contains the value correctnessId.
		final List<String> relatedElectionAttributesIdsList = contest.getAttributes().stream()
				.filter(electionAttributes -> electionAttributes.getRelated() != null && electionAttributes.getRelated().contains(correctnessId))
				.map(ElectionAttributes::getId).collect(Collectors.toList());

		// the number of voting options is the number of election options which are present in the list of the related election attributes ids.
		final long numberOfVotingOptions = contest.getOptions().stream()
				.filter(electionOption -> relatedElectionAttributesIdsList.contains(electionOption.getAttribute())).count();

		return Math.toIntExact(numberOfVotingOptions);
	}

	private static Question getCorrespondingQuestionByAttribute(final List<Question> questions, final String attribute, final String contestId) {
		return questions.stream().filter(question -> question.getAttribute().equals(attribute)).findAny().orElseThrow(
				() -> new CombinedCorrectnessInformationException(
						String.format("No corresponding question found in contest with id %s for attribute with id %s.", contestId, attribute)));
	}

	private static void checkContestsNotNullAndNotEmpty(final List<Contest> contests, final String ballotId) {
		if (contests == null) {
			throw new CombinedCorrectnessInformationException(String.format("The provided contests for the ballot with id %s are null.", ballotId));
		} else if (contests.isEmpty()) {
			throw new CombinedCorrectnessInformationException(String.format("The provided contests for the ballot with id %s are empty.", ballotId));
		}
	}

	private static void checkNotNullAndNotEmpty(final List<?> parameterList, final String parameterListContentDescription, final String contestId) {
		if (parameterList == null) {
			throw new CombinedCorrectnessInformationException(
					String.format("The provided %s for the contest with id %s are null.", parameterListContentDescription, contestId));
		} else if (parameterList.isEmpty()) {
			throw new CombinedCorrectnessInformationException(
					String.format("The provided %s for the contest with id %s are empty.", parameterListContentDescription, contestId));
		}
	}

	public String getCorrectnessIdForSelectionIndex(final int index) {
		checkArgument(index >= 0, String.format("The provided index %s is negative.", index));
		checkArgument(index < this.totalNumberOfSelections, String.format("There are less correctnessIds than the provided index %s.", index));

		return this.correctnessIdToListOfSelectionsIndexesMap.entrySet().stream().filter(entry -> entry.getValue().contains(index))
				.map(Map.Entry::getKey).findAny()
				.orElseThrow(() -> new CombinedCorrectnessInformationException(String.format("The provided index %s could not be found.", index)));
	}

	public String getCorrectnessIdForVotingOptionIndex(final int index) {
		checkArgument(index >= 0, String.format("The provided index %s is negative.", index));
		checkArgument(index < this.totalNumberOfVotingOptions, String.format("There are less voting options than the provided index %s.", index));

		return this.correctnessIdToListOfVotingOptionsIndexesMap.entrySet().stream().filter(entry -> entry.getValue().contains(index))
				.map(Map.Entry::getKey).findAny()
				.orElseThrow(() -> new CombinedCorrectnessInformationException(String.format("The provided index %s could not be found.", index)));
	}

	@JsonIgnore
	public Integer getTotalNumberOfSelections() {
		return this.totalNumberOfSelections;
	}

	@JsonIgnore
	public Integer getTotalNumberOfVotingOptions() {
		return this.totalNumberOfVotingOptions;
	}

	public List<CorrectnessInformation> getCorrectnessInformationList() {
		return this.correctnessInformationList;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		final CombinedCorrectnessInformation that = (CombinedCorrectnessInformation) o;
		return correctnessInformationList.equals(that.correctnessInformationList) && Objects
				.equals(totalNumberOfSelections, that.totalNumberOfSelections) && Objects
				.equals(totalNumberOfVotingOptions, that.totalNumberOfVotingOptions) && Objects
				.equals(correctnessIdToListOfSelectionsIndexesMap, that.correctnessIdToListOfSelectionsIndexesMap) && Objects
				.equals(correctnessIdToListOfVotingOptionsIndexesMap, that.correctnessIdToListOfVotingOptionsIndexesMap);
	}

	@Override
	public int hashCode() {
		return Objects
				.hash(correctnessInformationList, totalNumberOfSelections, totalNumberOfVotingOptions, correctnessIdToListOfSelectionsIndexesMap,
						correctnessIdToListOfVotingOptionsIndexesMap);
	}

	@Override
	public ImmutableList<Hashable> toHashableForm() {
		return ImmutableList.copyOf(correctnessInformationList);
	}

	private void initCombinedCorrectnessInformation() {

		this.totalNumberOfSelections = computeTotalNumberOfSelections(this.correctnessInformationList);
		this.totalNumberOfVotingOptions = computeTotalNumberOfVotingOptions(this.correctnessInformationList);

		this.correctnessIdToListOfSelectionsIndexesMap = getCorrectnessIdToListOfIndexesMap(this.correctnessInformationList,
				CorrectnessInformation::getNumberOfSelections);
		this.correctnessIdToListOfVotingOptionsIndexesMap = getCorrectnessIdToListOfIndexesMap(this.correctnessInformationList,
				CorrectnessInformation::getNumberOfVotingOptions);
	}

}
