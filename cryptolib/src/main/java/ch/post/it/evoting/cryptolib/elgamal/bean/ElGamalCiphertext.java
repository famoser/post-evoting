/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonValue;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Class which encapsulates the gamma and list of phi elements of ElGamal ciphertext (γ, 𝜙₀,..., 𝜙ₙ₋₁).
 */
@JsonRootName("ciphertext")
public final class ElGamalCiphertext extends BaseElGamalCiphertext<ZpGroupElement> {
	/**
	 * Creates a {@code ElGamalCiphertext} using the specified gamma and phi values.
	 *
	 * @param gammaElement the gamma (i.e. first) element of the ciphertext.
	 * @param phiElements  the phi elements of the ciphertext.
	 * @throws GeneralCryptoLibException if the gamma element is null or the list of phi elements is null, empty or contains one or more null values.
	 */
	public ElGamalCiphertext(final ZpGroupElement gammaElement, final List<ZpGroupElement> phiElements) throws GeneralCryptoLibException {
		super(gammaElement, phiElements);
	}

	/**
	 * Creates a {@code ElGamalCiphertext} using the specified list of elements. This list must fulfill this contract:
	 *
	 * <ul>
	 *   <li>The first element has to be the {@code gamma}.
	 *   <li>The other elements have to be the {@code phis} in the correct order.
	 *   <li>All elements should be members of the same mathematical group.
	 * </ul>
	 *
	 * @param ciphertext the sequence of the elements, first the {@code gamma}, then the {@code phi} values with the correct order.
	 * @throws GeneralCryptoLibException if the ciphertext is null, empty or contains one or more null values.
	 */
	public ElGamalCiphertext(List<ZpGroupElement> ciphertext) throws GeneralCryptoLibException {
		super(ciphertext);
	}

	/**
	 * Deserializes the instance from a string in JSON format.
	 *
	 * @param json the JSON
	 * @return the instance
	 * @throws GeneralCryptoLibException failed to deserialize the instance.
	 */
	public static ElGamalCiphertext fromJson(String json) throws GeneralCryptoLibException {
		return fromJson(json, ElGamalCiphertext.class);
	}

	/**
	 * Creates an instance from a given memento during JSON deserialization.
	 *
	 * @param memento the memento
	 * @return
	 * @throws GeneralCryptoLibException failed to create the instance.
	 */
	@JsonCreator
	static ElGamalCiphertext fromMemento(Memento memento) throws GeneralCryptoLibException {
		Validate.notNull(memento.gamma, "ElGamal gamma element");
		Validate.notNullOrEmpty(memento.phis, "List of ElGamal phi elements");
		Validate.notNull(memento.p, "Zp subgroup p parameter");
		Validate.notNull(memento.q, "Zp subgroup q parameter");
		ZpGroupElement gamma = new ZpGroupElement(memento.gamma, memento.p, memento.q);
		List<ZpGroupElement> phis = new ArrayList<>(memento.phis.size());
		for (BigInteger phi : memento.phis) {
			phis.add(new ZpGroupElement(phi, memento.p, memento.q));
		}
		return new ElGamalCiphertext(gamma, phis);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gamma == null) ? 0 : gamma.hashCode());
		result = prime * result + ((phis == null) ? 0 : phis.hashCode());
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
		ElGamalCiphertext other = (ElGamalCiphertext) obj;
		if (gamma == null) {
			if (other.gamma != null) {
				return false;
			}
		} else if (!gamma.equals(other.gamma)) {
			return false;
		}
		if (phis == null) {
			return other.phis == null;
		} else {
			return phis.equals(other.phis);
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
		memento.gamma = gamma.getValue();
		memento.phis = new ArrayList<>(phis.size());
		for (ZpGroupElement element : phis) {
			memento.phis.add(element.getValue());
		}
		memento.p = gamma.getP();
		memento.q = gamma.getQ();
		return memento;
	}

	/**
	 * Memento for JSON serialization.
	 */
	static class Memento {
		@JsonProperty("gamma")
		public BigInteger gamma;

		@JsonProperty("phis")
		public List<BigInteger> phis;

		@JsonProperty("p")
		public BigInteger p;

		@JsonProperty("q")
		public BigInteger q;
	}
}
