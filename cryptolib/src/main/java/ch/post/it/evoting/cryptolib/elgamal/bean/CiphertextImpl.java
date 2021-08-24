/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import static java.lang.System.arraycopy;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.serialization.AbstractJsonSerializable;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Implementation of {@link Ciphertext}.
 *
 * <p>This class is immutable.
 */
@JsonRootName("ciphertext")
public final class CiphertextImpl extends AbstractJsonSerializable implements Ciphertext {
	private final ZpGroupElement gamma;

	private final ZpGroupElement[] phis;

	@JsonCreator
	public CiphertextImpl(
			@JsonProperty("gamma")
					BigInteger value,
			@JsonProperty("phis")
					BigInteger[] phi,
			@JsonProperty("p")
					BigInteger p,
			@JsonProperty("q")
					BigInteger q) throws GeneralCryptoLibException {
		gamma = new ZpGroupElement(value, p, q);
		phis = new ZpGroupElement[phi.length];
		for (int i = 0; i < phi.length; i++) {
			phis[i] = new ZpGroupElement(phi[i], p, q);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param gamma the gamma
	 * @param phis  the phis, must be non-empty
	 * @throws GeneralCryptoLibException gamma is null or phis is empty or some phi is null or gamma and phis belong to different groups.
	 */
	public CiphertextImpl(ZpGroupElement gamma, List<ZpGroupElement> phis) throws GeneralCryptoLibException {
		this(gamma, phis.toArray(new ZpGroupElement[0]));
	}

	/**
	 * Constructor.
	 *
	 * @param gamma    the gamma
	 * @param firstPhi the first phi
	 * @param lastPhis the last phis
	 * @throws GeneralCryptoLibException the gamma is null or some phi is null.
	 */
	public CiphertextImpl(ZpGroupElement gamma, ZpGroupElement firstPhi, ZpGroupElement... lastPhis) throws GeneralCryptoLibException {
		this(gamma, joinPhis(firstPhi, lastPhis));
	}

	private CiphertextImpl(ZpGroupElement gamma, ZpGroupElement[] phis) throws GeneralCryptoLibException {
		Validate.notNull(gamma, "Gamma");
		Validate.notNullOrEmptyAndNoNulls(phis, "Phis");
		BigInteger p = gamma.getP();
		BigInteger q = gamma.getQ();
		for (ZpGroupElement phi : phis) {
			Validate.isEqual(p, phi.getP(), "P of Gamma", "P of Phi");
			Validate.isEqual(q, phi.getQ(), "Q of Gamma", "Q of Phi");
		}
		this.gamma = gamma;
		this.phis = phis;
	}

	private static ZpGroupElement[] joinPhis(ZpGroupElement firstPhi, ZpGroupElement[] lastPhis) {
		ZpGroupElement[] phis = new ZpGroupElement[lastPhis.length + 1];
		phis[0] = firstPhi;
		arraycopy(lastPhis, 0, phis, 1, lastPhis.length);
		return phis;
	}

	@Override
	public List<ZpGroupElement> getElements() {
		List<ZpGroupElement> elements = new ArrayList<>(phis.length + 1);
		elements.add(gamma);
		elements.addAll(asList(phis));
		return elements;
	}

	@Override
	public ZpGroupElement getGamma() {
		return gamma;
	}

	@Override
	public List<ZpGroupElement> getPhis() {
		return unmodifiableList(asList(phis));
	}

	@Override
	public int size() {
		return phis.length + 1;
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

		CiphertextImpl other = (CiphertextImpl) obj;

		return gamma.equals(other.getGamma()) && getPhis().equals(other.getPhis());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + gamma.hashCode();
		for (ZpGroupElement phi : phis) {
			result = prime * result + phi.hashCode();
		}
		return result;
	}

	@Override
	public String toString() {
		return "CiphertextImpl [gamma=" + gamma + ", phis=" + Arrays.toString(phis) + "]";
	}

	@Override
	public CiphertextImpl multiply(final Ciphertext otherCiphertext) throws GeneralCryptoLibException {

		if (otherCiphertext == null) {
			throw new GeneralCryptoLibException("The received ciphertext was null");
		}
		if (otherCiphertext.getPhis() == null) {
			throw new GeneralCryptoLibException("The received ciphertext does not contain an initialized list of phi values");
		}

		int numPhis = phis.length;
		final List<ZpGroupElement> otherPhis = otherCiphertext.getPhis();
		int numOtherPhis = otherPhis.size();
		if (numPhis != numOtherPhis) {
			throw new GeneralCryptoLibException(String.format("Ciphertexts of different lengths. This: %d other: %d", numPhis, numOtherPhis));
		}

		final ZpGroupElement resultGamma = gamma.multiply(otherCiphertext.getGamma());

		final ZpGroupElement[] resultPhis = new ZpGroupElement[numPhis];
		for (int i = 0; i < numPhis; i++) {
			resultPhis[i] = phis[i].multiply(otherPhis.get(i));
		}
		return new CiphertextImpl(resultGamma, resultPhis);
	}

	@Override
	public CiphertextImpl exponentiate(final Exponent exponent) throws GeneralCryptoLibException {

		if (exponent == null) {
			throw new GeneralCryptoLibException("The received exponent was null");
		}

		final ZpGroupElement gammaExponentiated = gamma.exponentiate(exponent);

		int numPhis = phis.length;
		final ZpGroupElement[] phisExponentiated = new ZpGroupElement[numPhis];
		for (int i = 0; i < numPhis; i++) {
			phisExponentiated[i] = phis[i].exponentiate(exponent);
		}

		return new CiphertextImpl(gammaExponentiated, phisExponentiated);
	}
}
