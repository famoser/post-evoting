/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.utils;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.GroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;

/**
 * Utility class providing high-level functions for obtaining a hash value from lists of group elements and an auxiliary string. This class uses the
 * PrimitivesServiceAPI to perform the hashing
 */
public final class HashBuilder {

	private final Charset charset;
	private final PrimitivesServiceAPI primitivesService;

	public HashBuilder(PrimitivesServiceAPI primitivesService, Charset charset) {
		Objects.requireNonNull(primitivesService, "A primitives service instance is required");
		Objects.requireNonNull(charset, "A charset is required");

		this.primitivesService = primitivesService;
		this.charset = charset;
	}

	/**
	 * Get the name of the charset encoding system used by this class. This charset encoding system is used when converting Strings to arrays of
	 * bytes
	 *
	 * @return a charset name.
	 */
	public Charset getCharset() {
		return charset;
	}

	/**
	 * Generates a hash value from the {@link GroupElement#getValue()} and string value of {@code data}.
	 *
	 * @param publicValues    group elements representing public values.
	 * @param generatedValues group elements representing generated values.
	 * @param auxiliaryData   auxiliary data encoded as a {@code String}.
	 * @return the generated hash value.
	 * @throws GeneralCryptoLibException if the list of public or generated group elements is null, empty or contains one or more null elements, or if
	 *                                   the data encoded as a string is null or blank.
	 */
	public byte[] buildHashForProofs(final List<? extends GroupElement> publicValues, final List<? extends GroupElement> generatedValues,
			final String auxiliaryData) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(publicValues, "List of public group elements");
		Validate.notNullOrEmptyAndNoNulls(generatedValues, "List of generated group elements");
		Validate.notNullOrBlank(auxiliaryData, "Data encoded as string");

		// Create a stream of all objects to include in the hash.
		Stream<Object> objectsToBeHashed = Stream.concat(publicValues.stream().map(GroupElement::getValue),
				Stream.concat(generatedValues.stream().map(GroupElement::getValue), Stream.of(auxiliaryData)));

		return primitivesService.getHashOfObjects(objectsToBeHashed, charset);
	}

	/**
	 * Hashes the public values, the generated values and the auxiliary data
	 *
	 * @param q               the Zp subgroup q parameter.
	 * @param publicValues    group elements representing public values.
	 * @param generatedValues group elements representing generated values.
	 * @param auxiliaryData   auxiliary data encoded as a {@code String}.
	 */
	public Exponent generateHash(BigInteger q, final List<? extends GroupElement> publicValues, final List<? extends GroupElement> generatedValues,
			final String auxiliaryData) throws GeneralCryptoLibException {

		byte[] hashAsByteArray = buildHashForProofs(publicValues, generatedValues, auxiliaryData);

		return new Exponent(q, new BigInteger(1, hashAsByteArray));
	}
}
