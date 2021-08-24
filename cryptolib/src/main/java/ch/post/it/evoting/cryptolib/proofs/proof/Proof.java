/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.proof;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonValue;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.serialization.AbstractJsonSerializable;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;

/**
 * Class which represents a proof in a Zero-Knowledge Proof of Knowledge (ZK-PoK) exchange using Maurer's unified PHI function.
 */
@JsonRootName("zkProof")
public final class Proof extends AbstractJsonSerializable {

	private final Exponent hashValue;

	private final List<Exponent> values;

	/**
	 * Constructor for a Proof, which initializes the internal fields with the received values.
	 *
	 * @param hashValue   a hash value calculated from a number of inputs.
	 * @param proofValues a list of {@link Exponent}.
	 * @throws GeneralCryptoLibException if arguments are invalid.
	 */
	public Proof(Exponent hashValue, List<Exponent> proofValues) throws GeneralCryptoLibException {
		Validate.notNull(hashValue, "Hash value of proof");
		Validate.notNullOrEmptyAndNoNulls(proofValues, "List of proof exponents");
		for (Exponent exponent : proofValues) {
			Validate.isEqual(exponent.getQ(), hashValue.getQ(), "Zp subgroup q parameter of proof exponent value",
					"Zp subgroup q parameter of proof hash value");
		}
		this.hashValue = hashValue;
		values = new ArrayList<>(proofValues);
	}

	/**
	 * Deserializes the instance from a string in JSON format.
	 *
	 * @param json the JSON
	 * @return the instance
	 * @throws GeneralCryptoLibException failed to deserialize the instance.
	 */
	public static Proof fromJson(String json) throws GeneralCryptoLibException {
		return AbstractJsonSerializable.fromJson(json, Proof.class);
	}

	/**
	 * Creates an instance from a given memento during JSON deserialization.
	 *
	 * @param memento the memento
	 * @return
	 * @throws GeneralCryptoLibException failed to create the instance.
	 */
	@JsonCreator
	static Proof fromMemento(Memento memento) throws GeneralCryptoLibException {
		Validate.notNull(memento.values, "List of proof exponents");
		Exponent hashValue = new Exponent(memento.q, memento.hash);
		List<Exponent> proofValues = new ArrayList<>(memento.values.size());
		for (BigInteger value : memento.values) {
			proofValues.add(new Exponent(memento.q, value));
		}
		return new Proof(hashValue, proofValues);
	}

	/**
	 * Retrieves the hash value of the proof.
	 *
	 * @return the hash value.
	 */
	public Exponent getHashValue() {

		return hashValue;
	}

	/**
	 * Retrieves the proof values .
	 *
	 * @return the proof values.
	 */
	public List<Exponent> getValuesList() {

		return new ArrayList<>(values);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hashValue == null) ? 0 : hashValue.hashCode());
		result = prime * result + ((values == null) ? 0 : values.hashCode());
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
		Proof other = (Proof) obj;
		if (hashValue == null) {
			if (other.hashValue != null) {
				return false;
			}
		} else if (!hashValue.equals(other.hashValue)) {
			return false;
		}
		if (values == null) {
			return other.values == null;
		} else {
			return values.equals(other.values);
		}
	}

	/**
	 * Returns a memento used during JSON serialization.
	 *
	 * @return a memento.
	 */
	@JsonValue
	Memento toMemento() {
		Memento memento = new Memento();
		memento.hash = hashValue.getValue();
		memento.values = new ArrayList<>(values.size());
		for (Exponent exponent : values) {
			memento.values.add(exponent.getValue());
		}
		memento.q = hashValue.getQ();
		return memento;
	}

	/**
	 * Memento for JSON serialization.
	 */
	static class Memento {
		@JsonProperty("hash")
		public BigInteger hash;

		@JsonProperty("values")
		public List<BigInteger> values;

		@JsonProperty("q")
		public BigInteger q;
	}
}
