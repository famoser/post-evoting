/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.election;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.domain.election.exceptions.CombinedCorrectnessInformationException;

@DisplayName("A combined correctness information")
class CombinedCorrectnessInformationTest {

	private static final String BALLOT_JSON = "ballot.json";
	private static final String BALLOT_2_JSON = "ballot2.json";
	private static final String BALLOT_3_JSON = "ballot3.json";

	private static Ballot ballot;
	private static Ballot ballot3;

	private static CombinedCorrectnessInformation combinedCorrectnessInformation;
	private static CombinedCorrectnessInformation combinedCorrectnessInformation2;
	private static CombinedCorrectnessInformation combinedCorrectnessInformation3;

	@BeforeAll
	static void setUpAll() throws IOException {
		ballot = getBallotFromResourceName(BALLOT_JSON);
		ballot3 = getBallotFromResourceName(BALLOT_3_JSON);

		final Ballot ballot2 = getBallotFromResourceName(BALLOT_2_JSON);

		combinedCorrectnessInformation = new CombinedCorrectnessInformation(ballot);
		combinedCorrectnessInformation2 = new CombinedCorrectnessInformation(ballot2);
		combinedCorrectnessInformation3 = new CombinedCorrectnessInformation(ballot3);
	}

	private static Stream<Arguments> getTotalNumberOfSelectionsTestSource() {

		return Stream
				.of(Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 13), Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 3),
						Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 11));
	}

	private static Stream<Arguments> getTotalNumberOfVotingOptionsTestSource() {
		return Stream
				.of(Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 432), Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 10),
						Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 124));
	}

	private static Stream<Arguments> getCorrectnessIdForSelectionIndexTestSource() {
		return Stream.of(Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 0, "e7c8b3ac09f64d95b08a6f451e0608fe"),
				Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 1, "acc6fffa007e413d8c2c51f80039f810"),
				Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 12, "acc6fffa007e413d8c2c51f80039f810"),

				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 0, "2d22a5bfa0f0406a9812576f925e1cea"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 1, "8522fc66faf2452e8062abe247ef5a24"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 2, "4c432e954efa4d8dbc5c5a8416d2e054"),

				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 0, "e65ce1ebeb9840a79c952a6ceae4c681"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 6, "e65ce1ebeb9840a79c952a6ceae4c681"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 7, "e727a2d916774758bd0c6f256c5ae241"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 8, "afaf857e8612471aa019f4b4ae0c4cd5"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 9, "afaf857e8612471aa019f4b4ae0c4cd5"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 10, "117bad3080214e94afe1abc811ebb2fb"));
	}

	private static Stream<Arguments> getCorrectnessIdForSelectionIndexOutOfBoundInputTestSource() {
		return Stream
				.of(Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 13), Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 3),
						Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 11));
	}

	private static Stream<Arguments> getCorrectnessIdForVotingOptionIndexTestSource() {
		return Stream.of(Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 0, "e7c8b3ac09f64d95b08a6f451e0608fe"),
				Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 1, "e7c8b3ac09f64d95b08a6f451e0608fe"),
				Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 23, "e7c8b3ac09f64d95b08a6f451e0608fe"),
				Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 24, "acc6fffa007e413d8c2c51f80039f810"),
				Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 55, "acc6fffa007e413d8c2c51f80039f810"),
				Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 282, "acc6fffa007e413d8c2c51f80039f810"),
				Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 430, "acc6fffa007e413d8c2c51f80039f810"),
				Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 431, "acc6fffa007e413d8c2c51f80039f810"),

				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 0, "2d22a5bfa0f0406a9812576f925e1cea"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 1, "2d22a5bfa0f0406a9812576f925e1cea"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 2, "2d22a5bfa0f0406a9812576f925e1cea"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 3, "8522fc66faf2452e8062abe247ef5a24"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 4, "8522fc66faf2452e8062abe247ef5a24"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 5, "8522fc66faf2452e8062abe247ef5a24"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 6, "4c432e954efa4d8dbc5c5a8416d2e054"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 7, "4c432e954efa4d8dbc5c5a8416d2e054"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 8, "4c432e954efa4d8dbc5c5a8416d2e054"),
				Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 9, "4c432e954efa4d8dbc5c5a8416d2e054"),

				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 0, "e65ce1ebeb9840a79c952a6ceae4c681"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 1, "e65ce1ebeb9840a79c952a6ceae4c681"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 97, "e65ce1ebeb9840a79c952a6ceae4c681"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 98, "e65ce1ebeb9840a79c952a6ceae4c681"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 99, "e727a2d916774758bd0c6f256c5ae241"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 100, "e727a2d916774758bd0c6f256c5ae241"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 105, "e727a2d916774758bd0c6f256c5ae241"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 106, "e727a2d916774758bd0c6f256c5ae241"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 107, "afaf857e8612471aa019f4b4ae0c4cd5"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 108, "afaf857e8612471aa019f4b4ae0c4cd5"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 115, "afaf857e8612471aa019f4b4ae0c4cd5"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 116, "afaf857e8612471aa019f4b4ae0c4cd5"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 117, "117bad3080214e94afe1abc811ebb2fb"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 118, "117bad3080214e94afe1abc811ebb2fb"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 122, "117bad3080214e94afe1abc811ebb2fb"),
				Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 123, "117bad3080214e94afe1abc811ebb2fb"));
	}

	private static Stream<Arguments> getCorrectnessIdForVotingOptionIndexOutOfBoundInputTestSource() {
		return Stream
				.of(Arguments.of(BALLOT_JSON, combinedCorrectnessInformation, 432), Arguments.of(BALLOT_2_JSON, combinedCorrectnessInformation2, 10),
						Arguments.of(BALLOT_3_JSON, combinedCorrectnessInformation3, 124));
	}

	private static Stream<Arguments> combinedCorrectnessInformationFromJsonTestSource() {
		return Stream.of(Arguments
						.of("combinedCorrectnessInformation.json", 0, "e7c8b3ac09f64d95b08a6f451e0608fe", 1, "e7c8b3ac09f64d95b08a6f451e0608fe", 13, 432),
				Arguments.of("combinedCorrectnessInformation2.json", 0, "2d22a5bfa0f0406a9812576f925e1cea", 1, "2d22a5bfa0f0406a9812576f925e1cea", 3,
						10), Arguments
						.of("combinedCorrectnessInformation3.json", 0, "e65ce1ebeb9840a79c952a6ceae4c681", 8, "e65ce1ebeb9840a79c952a6ceae4c681", 11,
								124));
	}

	private static Ballot getBallotFromResourceName(final String resourceName) throws IOException {
		return new ObjectMapper().readValue(CombinedCorrectnessInformationTest.class.getClassLoader().getResource(resourceName), Ballot.class);
	}

	private static CombinedCorrectnessInformation getCombinedCorrectnessInformationFromJson(final String jsonFilename) throws IOException {
		return new ObjectMapper()
				.readValue(CombinedCorrectnessInformationTest.class.getClassLoader().getResource(jsonFilename), CombinedCorrectnessInformation.class);
	}

	@ParameterizedTest(name = "built from {0} expected value {2}.")
	@MethodSource("getTotalNumberOfSelectionsTestSource")
	@DisplayName("built from a valid ballot, calling getTotalNumberOfSelections returns the expected result.")
	void getTotalNumberOfSelectionsTest(final String ignoredBallotName, final CombinedCorrectnessInformation combinedCorrectnessInformation,
			final int expectedTotalNumberOfSelections) {

		assertEquals(expectedTotalNumberOfSelections, combinedCorrectnessInformation.getTotalNumberOfSelections());
	}

	@ParameterizedTest(name = "built from {0} expected value {2}.")
	@MethodSource("getTotalNumberOfVotingOptionsTestSource")
	@DisplayName("built from a valid ballot, calling getTotalNumberOfVotingOptions returns the expected result.")
	void getTotalNumberOfVotingOptionsTest(final String ignoredBallotName, final CombinedCorrectnessInformation combinedCorrectnessInformation,
			final int expectedTotalNumberOfVotingOptions) {

		assertEquals(expectedTotalNumberOfVotingOptions, combinedCorrectnessInformation.getTotalNumberOfVotingOptions());
	}

	@ParameterizedTest(name = "built from {0} and given index {2}, expected value {3}.")
	@MethodSource("getCorrectnessIdForSelectionIndexTestSource")
	@DisplayName("built from a valid ballot, calling getCorrectnessIdForSelectionIndex with a valid index returns the expected result.")
	void getCorrectnessIdForSelectionIndexTest(final String ignoredBallotName, final CombinedCorrectnessInformation combinedCorrectnessInformation,
			final int index, final String expectedCorrectnessId) {

		assertEquals(expectedCorrectnessId, combinedCorrectnessInformation.getCorrectnessIdForSelectionIndex(index));
	}

	@Test
	@DisplayName("built from a valid ballot, calling getCorrectnessIdForSelectionIndex with a negative index throws an IllegalArgumentException.")
	void getCorrectnessIdForSelectionIndexNegativeInputTest() {
		final int index = -1;
		final CombinedCorrectnessInformation combinedCorrectnessInformation = new CombinedCorrectnessInformation(ballot);

		final IllegalArgumentException negativeIndexException = assertThrows(IllegalArgumentException.class,
				() -> combinedCorrectnessInformation.getCorrectnessIdForSelectionIndex(index));

		assertEquals("The provided index " + index + " is negative.", negativeIndexException.getMessage());
	}

	@ParameterizedTest(name = "built from {0} and given index {2}.")
	@MethodSource("getCorrectnessIdForSelectionIndexOutOfBoundInputTestSource")
	@DisplayName("built from a valid ballot, calling getCorrectnessIdForSelectionIndex with an out of bound index throws an IllegalArgumentException.")
	void getCorrectnessIdForSelectionIndexOutOfBoundInputTest(final String ignoredBallotName,
			final CombinedCorrectnessInformation combinedCorrectnessInformation, final int index) {

		final IllegalArgumentException outOfBoundIndexException = assertThrows(IllegalArgumentException.class,
				() -> combinedCorrectnessInformation.getCorrectnessIdForSelectionIndex(index));

		assertEquals("There are less correctnessIds than the provided index " + index + ".", outOfBoundIndexException.getMessage());
	}

	@ParameterizedTest(name = "built from {0} and given index {2}, expected value {3}.")
	@MethodSource("getCorrectnessIdForVotingOptionIndexTestSource")
	@DisplayName("built from a valid ballot, calling getCorrectnessIdForVotingOptionIndex with a valid index returns the expected result.")
	void getCorrectnessIdForVotingOptionIndexTest(final String ignoredBallotName, final CombinedCorrectnessInformation combinedCorrectnessInformation,
			final int index, final String expectedCorrectnessId) {
		assertEquals(expectedCorrectnessId, combinedCorrectnessInformation.getCorrectnessIdForVotingOptionIndex(index));
	}

	@Test
	@DisplayName("built from a valid ballot, calling getCorrectnessIdForVotingOptionIndex with a negative index throws an IllegalArgumentException.")
	void getCorrectnessIdForVotingOptionIndexNegativeInputTest() {
		final int index = -1;
		final CombinedCorrectnessInformation combinedCorrectnessInformation = new CombinedCorrectnessInformation(ballot);

		final IllegalArgumentException negativeIndexException = assertThrows(IllegalArgumentException.class,
				() -> combinedCorrectnessInformation.getCorrectnessIdForVotingOptionIndex(index));

		assertEquals("The provided index " + index + " is negative.", negativeIndexException.getMessage());
	}

	@ParameterizedTest(name = "built from {0} and given index {2}.")
	@MethodSource("getCorrectnessIdForVotingOptionIndexOutOfBoundInputTestSource")
	@DisplayName("built from a valid ballot, calling getCorrectnessIdForVotingOptionIndex with an out of bound index throws an IllegalArgumentException.")
	void getCorrectnessIdForVotingOptionIndexOutOfBoundInputTest(final String ignoredBallotName,
			final CombinedCorrectnessInformation combinedCorrectnessInformation, final int index) {

		final IllegalArgumentException outOfBoundIndexException = assertThrows(IllegalArgumentException.class,
				() -> combinedCorrectnessInformation.getCorrectnessIdForVotingOptionIndex(index));

		assertEquals("There are less voting options than the provided index " + index + ".", outOfBoundIndexException.getMessage());
	}

	@Test
	@DisplayName("built from a malformed ballot with missing questions, throws a CombinedCorrectnessInformationException.")
	void combinedCorrectnessInformationExceptionTest() throws IOException {
		final Ballot ballotNoCorrespondingQuestionFound = getBallotFromResourceName("ballotNoCorrespondingQuestionFound.json");

		final CombinedCorrectnessInformationException combinedCorrectnessInformationException = assertThrows(
				CombinedCorrectnessInformationException.class, () -> new CombinedCorrectnessInformation(ballotNoCorrespondingQuestionFound));

		assertEquals(
				"No corresponding question found in contest with id 17966dc82c0841db996b0c718a3255e3 for attribute with id acc7fffa007e413d8c2c51f80039f810.",
				combinedCorrectnessInformationException.getMessage());
	}

	@Test
	@DisplayName("built from a valid ballot and mapped to a JSON string, does not throw an exception.")
	void combinedCorrectnessInformationToJsonTest() {

		final CombinedCorrectnessInformation combinedCorrectnessInformation = new CombinedCorrectnessInformation(ballot);

		assertDoesNotThrow(() -> new ObjectMapper().writeValueAsString(combinedCorrectnessInformation));
	}

	@Test
	@DisplayName("built from a valid ballot and mapped to a JSON string, matches the expected JSON string.")
	void combinedCorrectnessInformationJsonMatchingTest() throws IOException {

		final CombinedCorrectnessInformation combinedCorrectnessInformation = new CombinedCorrectnessInformation(ballot3);

		final CombinedCorrectnessInformation combinedCorrectnessInformationRebuiltFromJSON = getCombinedCorrectnessInformationFromJson(
				"combinedCorrectnessInformation3.json");

		assertEquals(new ObjectMapper().writeValueAsString(combinedCorrectnessInformationRebuiltFromJSON),
				new ObjectMapper().writeValueAsString(combinedCorrectnessInformation));
	}

	@ParameterizedTest(name = "built from {0}.")
	@MethodSource("combinedCorrectnessInformationFromJsonTestSource")
	@DisplayName("built from a JSON string representation, returns the expected results upon methods calls.")
	void combinedCorrectnessInformationFromJsonTest(final String jsonFileName, final int selectionIndex,
			final String expectedCorrectnessIdForSelectionIndex, final int votingOptionIndex, final String expectedCorrectnessIdForVotingOptionIndex,
			final int expectedTotalNumberOfSelections, final int expectedTotalNumberOfVotingOptions) throws IOException {

		final CombinedCorrectnessInformation combinedCorrectnessInformationRebuiltFromJSON = getCombinedCorrectnessInformationFromJson(jsonFileName);

		assertAll(() -> assertNotNull(combinedCorrectnessInformationRebuiltFromJSON),
				() -> assertNotNull(combinedCorrectnessInformationRebuiltFromJSON.getCorrectnessInformationList()),
				() -> assertEquals(expectedCorrectnessIdForSelectionIndex,
						combinedCorrectnessInformationRebuiltFromJSON.getCorrectnessIdForSelectionIndex(selectionIndex)),
				() -> assertEquals(expectedCorrectnessIdForVotingOptionIndex,
						combinedCorrectnessInformationRebuiltFromJSON.getCorrectnessIdForVotingOptionIndex(votingOptionIndex)),
				() -> assertEquals(expectedTotalNumberOfSelections, combinedCorrectnessInformationRebuiltFromJSON.getTotalNumberOfSelections()),
				() -> assertEquals(expectedTotalNumberOfVotingOptions,
						combinedCorrectnessInformationRebuiltFromJSON.getTotalNumberOfVotingOptions()));
	}

}
