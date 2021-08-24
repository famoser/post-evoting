/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.mathematical.groups.impl;

import java.math.BigInteger;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.serialization.AbstractJsonSerializable;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.bigintegers.BigIntegers;

/**
 * Implementation of an exponent that can be used for mathematical operations defined by the Zp subgroup.
 *
 * <p>Instances of this class are immutable.
 */
@JsonRootName("exponent")
public final class Exponent extends AbstractJsonSerializable {

	private final BigInteger q;

	private final BigInteger value;

	/**
	 * Creates an exponent with the specified Zp subgroup q parameter.
	 *
	 * <p>The value of the exponent should be within the range [0..q-1]. If the value provided is not
	 * within this range, then the value assigned to the exponent will be recalculated as follows:
	 *
	 * <p>{@code value = value mod q}
	 *
	 * @param q     the Zp subgroup q parameter.
	 * @param value the value of the exponent.
	 * @throws GeneralCryptoLibException if the Zp subgroup q parameter is null or zero, or if the value is null.
	 */
	@JsonCreator
	public Exponent(
			@JsonProperty("q")
					BigInteger q,
			@JsonProperty("value")
					BigInteger value) throws GeneralCryptoLibException {
		validateInput(q, value);
		this.q = q;
		this.value = calculateValue(value);
	}

	/**
	 * Deserializes the instance from a string in JSON format.
	 *
	 * @param json the JSON
	 * @return the instance
	 * @throws GeneralCryptoLibException failed to deserialize the instance.
	 */
	public static Exponent fromJson(String json) throws GeneralCryptoLibException {
		return AbstractJsonSerializable.fromJson(json, Exponent.class);
	}

	private static void validateInput(BigInteger q, BigInteger value) throws GeneralCryptoLibException {
		Validate.notNull(q, "Zp subgroup q parameter");
		Validate.notNull(value, "Exponent value");
		Validate.notLessThan(q, BigInteger.ONE, "Zp subgroup q parameter", "");
	}

	/**
	 * Retrieves the Zp subgroup q parameter for this exponent.
	 *
	 * @return the Zp subgroup q parameter.
	 */
	@JsonProperty("q")
	public BigInteger getQ() {
		return q;
	}

	/**
	 * Retrieves the value of the exponent.
	 *
	 * @return the value of the exponent.
	 */
	@JsonProperty("value")
	public BigInteger getValue() {
		return value;
	}

	/**
	 * Returns an {@code Exponent} whose value is {@code (this + exponent) mod q}.
	 *
	 * @param exponent the exponent to be added to this Exponent.
	 * @return {@code (this + exponent) mod q}.
	 * @throws GeneralCryptoLibException if exponents belong to different groups.
	 */
	public Exponent add(Exponent exponent) throws GeneralCryptoLibException {
		confirmSameQ(exponent);
		BigInteger newValue = this.value.add(exponent.getValue()).mod(q);
		return new Exponent(q, newValue);
	}

	/**
	 * Returns an {@code Exponent} whose value is {@code (this - exponent) mod q}.
	 *
	 * @param exponent the exponent to be subtracted from this.
	 * @return {@code (this - exponent) mod q}.
	 * @throws GeneralCryptoLibException if exponents belong to different groups.
	 */
	public Exponent subtract(Exponent exponent) throws GeneralCryptoLibException {
		confirmSameQ(exponent);
		BigInteger newValue = this.value.subtract(exponent.getValue()).mod(q);
		return new Exponent(q, newValue);
	}

	/**
	 * Returns an {@code Exponent} whose value is {@code (this * exponent) mod q}.
	 *
	 * @param exponent the exponent to be multiplied.
	 * @return {@code (this * exponent) mod q}.
	 * @throws GeneralCryptoLibException if exponents belong to different groups.
	 */
	public Exponent multiply(Exponent exponent) throws GeneralCryptoLibException {
		confirmSameQ(exponent);
		BigInteger result = BigIntegers.modMultiply(value, exponent.getValue(), q);
		return new Exponent(q, result);
	}

	/**
	 * Returns an {@code Exponent} whose value is {@code (-this) mod q}.
	 *
	 * @return {@code (-this mod q)}
	 */
	public Exponent negate() {
		try {
			return new Exponent(q, value.negate().mod(q));
		} catch (GeneralCryptoLibException e) {
			throw new CryptoLibException(e);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((q == null) ? 0 : q.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Exponent other = (Exponent) obj;
		if (q == null) {
			if (other.q != null) {
				return false;
			}
		} else if (!q.equals(other.q)) {
			return false;
		}
		if (value == null) {
			return other.value == null;
		} else {
			return value.equals(other.value);
		}
	}

	@Override
	public String toString() {
		return "Exponent [q=" + q + ", value=" + value + "]";
	}

	/**
	 * Calculates the value to set for this {@code Exponent}. An exponent value has to be a number between {@code 0} and {@code q-1} inclusive, so if
	 * the received value is less than {@code 0} or greater than {@code q-1}, {@code mod q} has to be applied.
	 *
	 * @param exponentValue the value of the exponent.
	 * @return the value to set to this exponent.
	 */
	private BigInteger calculateValue(BigInteger exponentValue) {

		BigInteger result;
		if ((q.compareTo(exponentValue) > 0) && (BigInteger.ZERO.compareTo(exponentValue) < 1)) {
			result = exponentValue;
		} else {
			result = exponentValue.mod(q);
		}
		return result;
	}

	private void confirmSameQ(Exponent exponent) throws GeneralCryptoLibException {
		Validate.notNull(exponent, "Exponent");
		Validate.isEqual(exponent.getQ(), q, "Zp subgroup q parameter of this exponent", "Zp subgroup q parameter of specified exponent");
	}
}
