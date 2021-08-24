/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.bean;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Container class for the pre-computed values of a zero knowledge proof.
 */
public final class ProofPreComputedValues {

	private final List<Exponent> exponents;

	private final List<ZpGroupElement> phiOutputs;

	/**
	 * Default constructor.
	 *
	 * @param exponents  the randomly generated exponents, as a list of {@link Exponent} objects.
	 * @param phiOutputs the outputs of the phi function, as a list of {@link ZpGroupElement} objects.
	 * @throws GeneralCryptoLibException if the list of random exponents or phi function outputs is null, empty or contains one or more null
	 *                                   elements.
	 */
	public ProofPreComputedValues(final List<Exponent> exponents, final List<ZpGroupElement> phiOutputs) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(exponents, "List of random exponents");
		Validate.notNullOrEmptyAndNoNulls(phiOutputs, "List of phi function outputs");

		this.exponents = new ArrayList<>(exponents);
		this.phiOutputs = new ArrayList<>(phiOutputs);
	}

	public List<Exponent> getExponents() {

		return new ArrayList<>(exponents);
	}

	public List<ZpGroupElement> getPhiOutputs() {

		return new ArrayList<>(phiOutputs);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((exponents == null) ? 0 : exponents.hashCode());
		result = prime * result + ((phiOutputs == null) ? 0 : phiOutputs.hashCode());
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
		ProofPreComputedValues other = (ProofPreComputedValues) obj;
		if (exponents == null) {
			if (other.exponents != null) {
				return false;
			}
		} else if (!exponents.equals(other.exponents)) {
			return false;
		}
		if (phiOutputs == null) {
			return other.phiOutputs == null;
		} else {
			return phiOutputs.equals(other.phiOutputs);
		}
	}
}
