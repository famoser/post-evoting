/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.domain.cryptoadapters;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientCiphertext;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientKeyPair;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPrivateKey;
import ch.post.it.evoting.cryptoprimitives.elgamal.ElGamalMultiRecipientPublicKey;
import ch.post.it.evoting.cryptoprimitives.math.BigIntegerOperations;
import ch.post.it.evoting.cryptoprimitives.math.GqElement;
import ch.post.it.evoting.cryptoprimitives.math.GqGroup;
import ch.post.it.evoting.cryptoprimitives.math.ZqElement;
import ch.post.it.evoting.cryptoprimitives.math.ZqGroup;

/**
 * Adapters to convert between the cryptolib and crypto-primitives objects
 */
public final class CryptoAdapters {

	private static final int MIN_GENERATOR = 2;
	private static final int MAX_GENERATOR = 4;

	private CryptoAdapters() {
		//Intentionally left blank
	}

	/*
		Converts a cryptolib ZpSubGroup to a ZqGroup
	 */
	public static ZqGroup convert(ZpSubgroup cryptolibGroup) {
		checkNotNull(cryptolibGroup);
		return new ZqGroup(cryptolibGroup.getQ());
	}

	/*
		Converts a cryptolib exponent to a ZqElement
	 */
	public static ZqElement convert(Exponent cryptolibExponent) {
		checkNotNull(cryptolibExponent);
		BigInteger value = cryptolibExponent.getValue();
		BigInteger q = cryptolibExponent.getQ();
		ZqGroup zqGroup = new ZqGroup(q);
		return ZqElement.create(value, zqGroup);
	}

	/*
		Converts an ElGamalPrivateKey to an ElGamalMultiRecipientPrivateKey
	 */
	public static ElGamalMultiRecipientPrivateKey convert(ElGamalPrivateKey cryptolibPrivateKey) {
		checkNotNull(cryptolibPrivateKey);
		List<ZqElement> privateKeyElements = cryptolibPrivateKey.getKeys().stream().map(CryptoAdapters::convert).collect(Collectors.toList());
		return new ElGamalMultiRecipientPrivateKey(privateKeyElements);
	}

	/**
	 * Converts a cryptolib ZpGroupElement to a GqElement.
	 *
	 * @param cryptolibElement   the element to convert
	 * @param cryptolibGenerator the generator of the group
	 * @return a GqElement with value the same as the cryptolibElement and the group defined by the generator element.
	 */
	public static GqElement convert(ZpGroupElement cryptolibElement, ZpGroupElement cryptolibGenerator) {
		checkNotNull(cryptolibElement);
		checkNotNull(cryptolibGenerator);
		GqGroup group = new GqGroup(cryptolibGenerator.getP(), cryptolibGenerator.getQ(), cryptolibGenerator.getValue());
		return GqElement.create(cryptolibElement.getValue(), group);
	}

	/*
		Converts a cryptolib ElGamalPrivateKey to an ElGamalMultiRecipientKeyPair
	*/
	public static ElGamalMultiRecipientKeyPair toElGamalMultiRecipientKeyPair(final ElGamalPrivateKey cryptolibPrivateKey) {
		checkNotNull(cryptolibPrivateKey);

		final ZpGroupElement generatorCryptolib = cryptolibPrivateKey.getGroup().getGenerator();
		final GqElement generator = convert(generatorCryptolib, generatorCryptolib);

		final ElGamalMultiRecipientPrivateKey privateKey = convert(cryptolibPrivateKey);

		return ElGamalMultiRecipientKeyPair.from(privateKey, generator);
	}

	public static ElGamalMultiRecipientCiphertext convert(final ElGamalCiphertext cryptolibCiphertext, final ZpSubgroup cryptolibGroup) {
		final ZpGroupElement groupGenerator = cryptolibGroup.getGenerator();
		final GqElement gamma = CryptoAdapters.convert(cryptolibCiphertext.getGamma(), groupGenerator);
		final List<GqElement> phis = cryptolibCiphertext.getPhis().stream().map(phi -> CryptoAdapters.convert(phi, groupGenerator))
				.collect(Collectors.toList());

		return ElGamalMultiRecipientCiphertext.create(gamma, phis);
	}

	// -----------------------------------------------------------------------------------------------------------------------------------------------
	// Crypto-primitives to cryptolib.
	// -----------------------------------------------------------------------------------------------------------------------------------------------

	public static ElGamalPublicKey convert(final ElGamalMultiRecipientPublicKey elGamalMultiRecipientPublicKey) {
		final BigInteger p = elGamalMultiRecipientPublicKey.getGroup().getP();
		final BigInteger q = elGamalMultiRecipientPublicKey.getGroup().getQ();
		final BigInteger g = elGamalMultiRecipientPublicKey.getGroup().getGenerator().getValue();

		try {
			final ZpSubgroup zpSubgroup = new ZpSubgroup(g, p, q);

			// Convert crypto-primitives public key to cryptolib public key.
			final List<ZpGroupElement> elements = new ArrayList<>();
			for (GqElement publicKeyElement : elGamalMultiRecipientPublicKey.getKeyElements()) {
				elements.add(new ZpGroupElement(publicKeyElement.getValue(), zpSubgroup));
			}
			return new ElGamalPublicKey(elements, zpSubgroup);

		} catch (GeneralCryptoLibException e) {
			throw new IllegalArgumentException("Failed to convert to ElGamalPublicKey");
		}
	}

	public static ElGamalPrivateKey convert(final ElGamalMultiRecipientPrivateKey elGamalMultiRecipientPrivateKey) {
		final BigInteger q = elGamalMultiRecipientPrivateKey.getGroup().getQ();
		final BigInteger p = q.shiftLeft(1).add(BigInteger.ONE);
		final BigInteger g = findGroupGenerator(p, q);

		try {
			final ZpSubgroup zpSubgroup = new ZpSubgroup(g, p, q);

			// Convert crypto-primitives private key to cryptolib private key.
			final List<Exponent> exponents = new ArrayList<>();
			for (int i = 0; i < elGamalMultiRecipientPrivateKey.size(); i++) {
				final ZqElement privateKeyElement = elGamalMultiRecipientPrivateKey.get(i);
				exponents.add(new Exponent(q, privateKeyElement.getValue()));
			}
			return new ElGamalPrivateKey(exponents, zpSubgroup);
		} catch (GeneralCryptoLibException e) {
			throw new IllegalArgumentException("Failed to convert to ElGamalPublicKey");
		}
	}

	public static List<ElGamalCiphertext> convert(final ElGamalMultiRecipientCiphertext elGamalMultiRecipientCiphertext) {
		final ZpGroupElement gamma = convert(elGamalMultiRecipientCiphertext.getGamma());

		return elGamalMultiRecipientCiphertext.getPhi().stream().map(CryptoAdapters::convert).map(phi -> {
			try {
				return new ElGamalCiphertext(gamma, Collections.singletonList(phi));
			} catch (GeneralCryptoLibException e) {
				throw new IllegalArgumentException("Failed to convert ElGamalMultiRecipientCiphertext.", e);
			}
		}).collect(Collectors.toList());
	}

	public static ZpGroupElement convert(final GqElement gqElement) {
		final BigInteger value = gqElement.getValue();
		final BigInteger p = gqElement.getGroup().getP();
		final BigInteger q = gqElement.getGroup().getQ();
		try {
			return new ZpGroupElement(value, p, q);
		} catch (GeneralCryptoLibException e) {
			throw new IllegalArgumentException("Failed to convert GqElement.", e);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------------------------------------
	// Utilities.
	// -----------------------------------------------------------------------------------------------------------------------------------------------

	private static BigInteger findGroupGenerator(final BigInteger p, final BigInteger q) {
		return IntStream.rangeClosed(MIN_GENERATOR, MAX_GENERATOR).mapToObj(BigInteger::valueOf)
				.filter(value -> BigIntegerOperations.modExponentiate(value, q, p).compareTo(BigInteger.ONE) == 0).findFirst()
				.orElse(null); // If 2 and 3 are not generators, 4 will always be one so null is never returned.
	}
}
