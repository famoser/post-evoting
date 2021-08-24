/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.function;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

class PhiFunctionTest {

	private static final int numInputs = 1;
	private static final int numOutputs = 1;

	private static int[][][] computationRules;
	private static List<ZpGroupElement> baseElements;
	private static ZpSubgroup group;
	private static PhiFunction phiFunction;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		computationRules = new int[][][] { { { 1, 1 } } };

		BigInteger ZpSubgroupP = new BigInteger("23");
		BigInteger ZpSubgroupQ = new BigInteger("11");
		BigInteger ZpSubgroupG = new BigInteger("2");

		group = new ZpSubgroup(ZpSubgroupG, ZpSubgroupP, ZpSubgroupQ);

		baseElements = new ArrayList<>();
		baseElements.add(group.getGenerator());

		phiFunction = new PhiFunction(group, numInputs, numOutputs, baseElements, computationRules);
	}

	@Test
	void givenNullGroupWhenCreatePhiFunctionThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(null, numInputs, numOutputs, baseElements, computationRules));
	}

	@Test
	void givenNegativeNumInputsWhenCreatePhiFunctionThenException() {
		int negativeNumInputs = -1;

		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, negativeNumInputs, numOutputs, baseElements, computationRules));
	}

	@Test
	void givenNegativeNumOutputsWhenCreatePhiFunctionThenException() {
		int negativeNumOutputs = -1;

		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, negativeNumOutputs, baseElements, computationRules));
	}

	@Test
	void givenNullBaseElementsWhenCreatePhiFunctionThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, numOutputs, null, computationRules));
	}

	@Test
	void givenEmptyBaseElementsWhenCreatePhiFunctionThenException() {
		List<ZpGroupElement> emptyBaseElements = new ArrayList<>();

		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, numOutputs, emptyBaseElements, computationRules));
	}

	@Test
	void givenNullComputationalRulesWhenCreatePhiFunctionThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, numOutputs, baseElements, null));
	}

	@Test
	void givenZeroLengthComputationalRulesWhenCreatePhiFunctionThenException() {
		int[][][] zeroLengthComputationRules = new int[0][0][0];

		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, numOutputs, baseElements, zeroLengthComputationRules));
	}

	@Test
	void givenRulesWithListOfWrongSizeWhenCreatePhiFunctionThenException() {
		int[][][] wrongLengthComputationRules = new int[][][] { { { 1, 1 } }, { { 1, 1 } }, { { 1, 1 } }, { { 1, 1 } } };

		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, numOutputs, baseElements, wrongLengthComputationRules));
	}

	@Test
	void givenRulesFirstIndexValueNegativeWhenCreatePhiFunctionThenException() {
		int[][][] badRules = computationRules = new int[][][] { { { -1, 1 } } };

		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, numOutputs, baseElements, badRules));
	}

	@Test
	void givenRulesFirstIndexValueLargerThenNumBasesWhenCreatePhiFunctionThenException() {
		int[][][] badRules = computationRules = new int[][][] { { { 2, 1 } } };

		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, numOutputs, baseElements, badRules));
	}

	@Test
	void givenRulesSecondIndexValueNegativeWhenCreatePhiFunctionThenException() {
		int[][][] badRules = computationRules = new int[][][] { { { 1, -1 } } };

		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, numOutputs, baseElements, badRules));
	}

	@Test
	void givenRulesSecondIndexValueLargerThenNumBasesWhenCreatePhiFunctionThenException() {
		int[][][] badRules = computationRules = new int[][][] { { { 1, 2 } } };

		assertThrows(GeneralCryptoLibException.class, () -> new PhiFunction(group, numInputs, numOutputs, baseElements, badRules));
	}

	@Test
	void givenNullInputWhenCalculatePhiThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> phiFunction.calculatePhi(null));
	}

	@Test
	void givenInputWithSizeNotExpectedNumOfInputsWhenCalculatePhiThenException() {
		List<Exponent> notRSizeInput = new ArrayList<>();

		assertThrows(GeneralCryptoLibException.class, () -> phiFunction.calculatePhi(notRSizeInput));
	}
}
