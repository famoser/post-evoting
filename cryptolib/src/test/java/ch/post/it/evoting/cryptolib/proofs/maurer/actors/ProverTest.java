/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.maurer.actors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.system.OperatingSystem;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponents;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.primitives.messagedigest.configuration.ConfigMessageDigestAlgorithmAndProvider;
import ch.post.it.evoting.cryptolib.primitives.securerandom.configuration.ConfigSecureRandomAlgorithmAndProvider;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.SecureRandomFactory;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.proofs.bean.ProofPreComputedValues;
import ch.post.it.evoting.cryptolib.proofs.maurer.configuration.ConfigProofHashCharset;
import ch.post.it.evoting.cryptolib.proofs.maurer.configuration.MaurerProofPolicy;
import ch.post.it.evoting.cryptolib.proofs.maurer.factory.Prover;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunction;
import ch.post.it.evoting.cryptolib.proofs.maurer.function.PhiFunctionExponentiation;
import ch.post.it.evoting.cryptolib.proofs.utils.HashBuilder;

class ProverTest {

	private static final String testString = "Hello, I am a short String";

	private static ZpSubgroup group;
	private static PhiFunction phiFunctionExponentiation;
	private static List<ZpGroupElement> publicValues;
	private static List<Exponent> privateValues;
	private static ProofPreComputedValues preComputedValues;
	private static Prover<ZpGroupElement> prover;
	private static HashBuilder hashBuilder;

	@BeforeAll
	static void setUp() throws GeneralCryptoLibException {

		BigInteger ZpSubgroupP = new BigInteger("23");
		BigInteger ZpSubgroupQ = new BigInteger("11");
		BigInteger ZpSubgroupG = new BigInteger("2");

		group = new ZpSubgroup(ZpSubgroupG, ZpSubgroupP, ZpSubgroupQ);

		CryptoRandomInteger cryptoRandomInteger = new SecureRandomFactory(getMaurerProofPolicy()).createIntegerRandom();

		// Set up the PHI function parameters

		List<ZpGroupElement> baseElementsSmall = new ArrayList<>();
		baseElementsSmall.add(group.getGenerator());

		phiFunctionExponentiation = new PhiFunctionExponentiation(group, baseElementsSmall);

		hashBuilder = new HashBuilder(new PrimitivesService(), getMaurerProofPolicy().getCharset().getCharset());

		prover = new Prover<>(group, phiFunctionExponentiation, hashBuilder);

		privateValues = new ArrayList<>();
		privateValues.add(Exponents.random(group, cryptoRandomInteger));

		publicValues = new ArrayList<>();
		publicValues.add(baseElementsSmall.get(0).exponentiate(privateValues.get(0)));

		preComputedValues = prover.preCompute(cryptoRandomInteger);
	}

	private static MaurerProofPolicy getMaurerProofPolicy() {

		return new MaurerProofPolicy() {

			@Override
			public ConfigMessageDigestAlgorithmAndProvider getMessageDigestAlgorithmAndProvider() {
				return ConfigMessageDigestAlgorithmAndProvider.SHA256_DEFAULT;
			}

			@Override
			public ConfigProofHashCharset getCharset() {
				return ConfigProofHashCharset.UTF8;
			}

			@Override
			public ConfigSecureRandomAlgorithmAndProvider getSecureRandomAlgorithmAndProvider() {

				return getSecureRandom();
			}
		};
	}

	private static ConfigSecureRandomAlgorithmAndProvider getSecureRandom() {

		switch (OperatingSystem.current()) {
		case WINDOWS:
			return ConfigSecureRandomAlgorithmAndProvider.PRNG_SUN_MSCAPI;
		case UNIX:
			return ConfigSecureRandomAlgorithmAndProvider.NATIVE_PRNG_SUN;
		default:
			throw new CryptoLibException("OS not supported");
		}
	}

	@Test
	void givenValidGroupAndPhiWhenCreateProverThenOK() {
		assertDoesNotThrow(() -> new Prover<>(group, phiFunctionExponentiation, hashBuilder));
	}

	@Test
	void givenNullGroupWhenCreateProverThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new Prover<>(null, phiFunctionExponentiation, hashBuilder));
	}

	@Test
	void givenNullPhiWhenCreateProverThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> new Prover<>(group, null, hashBuilder));
	}

	@Test
	void givenValidArgumentsWhenProveThenOK() {
		assertDoesNotThrow(() -> prover.prove(publicValues, privateValues, testString, preComputedValues));
	}

	@Test
	void givenNullPublicValuesWhenProveThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> prover.prove(null, privateValues, testString, preComputedValues));
	}

	@Test
	void givenNullPrivateValuesWhenProveThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> prover.prove(publicValues, null, testString, preComputedValues));
	}

	@Test
	void givenInvalidPrivateValuesSizeWhenProveThenException() {
		List<Exponent> wrongSizePrivateValues = new ArrayList<>();

		wrongSizePrivateValues.add(privateValues.get(0));
		wrongSizePrivateValues.add(privateValues.get(0));

		assertThrows(GeneralCryptoLibException.class, () -> prover.prove(publicValues, wrongSizePrivateValues, testString, preComputedValues));
	}

	@Test
	void givenNullStringWhenProveThenException() {
		assertThrows(GeneralCryptoLibException.class, () -> prover.prove(publicValues, privateValues, null, preComputedValues));
	}

}
