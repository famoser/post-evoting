/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.proofs.utils;

import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.elgamal.bean.CiphertextImpl;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.elgamal.utils.ElGamalTestDataGenerator;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.proofs.bean.SimplePlaintextCiphertextPair;
import ch.post.it.evoting.cryptolib.proofs.bean.SimplePlaintextPublicKeyPair;

/**
 * Utility to generate various types of zero knowledge proof of knowledge related data needed by tests.
 */
public class ProofsTestDataGenerator {

	/**
	 * creates a pair of ElGamal public keys suitable for use with simple plaintext equality zero knowledge proofs of knowledge.
	 *
	 * @param zpSubgroup      the Zp subgroup to which the ElGamal public key elements are to belong.
	 * @param parentPublicKey the parent ElGamal public key from which to create the public key pair.
	 * @return the ElGamal public key pair.
	 * @throws GeneralCryptoLibException if the ElGamal pair craeation process fails.
	 */
	public static SimplePlaintextPublicKeyPair getSimplePlaintextPublicKeyPair(final ZpSubgroup zpSubgroup, final ElGamalPublicKey parentPublicKey)
			throws GeneralCryptoLibException {

		int parentPublicKeyLength = parentPublicKey.getKeys().size();

		boolean isEven = ((parentPublicKeyLength % 2) == 0);
		if (!isEven) {
			throw new GeneralCryptoLibException(
					"The number of ElGamal public key elements must be even for simple plaintext equality proofs; Found " + parentPublicKeyLength);
		}

		List<ZpGroupElement> publicKeyElements = parentPublicKey.getKeys().subList(0, (parentPublicKeyLength / 2));
		ElGamalPublicKey primaryPublicKey = new ElGamalPublicKey(publicKeyElements, zpSubgroup);

		publicKeyElements = parentPublicKey.getKeys().subList((parentPublicKeyLength / 2), parentPublicKeyLength);
		ElGamalPublicKey secondaryPublicKey = new ElGamalPublicKey(publicKeyElements, zpSubgroup);

		return new SimplePlaintextPublicKeyPair(primaryPublicKey, secondaryPublicKey);
	}

	/**
	 * Randomly generates a list of specified length of Zp group elements belonging to a specified Zp subgroup that is suitable for use with simple
	 * plaintext equality zero knowledge proofs of knowledge. By definition, the length of this type of plaintext must be even and all its elements
	 * must have identical value.
	 *
	 * @param zpSubgroup  the Zp subgroup to which the simple plaintext elements are to belong.
	 * @param numElements the number of Zp group elements in the simple plaintext.
	 * @return the generated simple plaintext.
	 * @throws GeneralCryptoLibException if the simple plaintext generation process fails.
	 */
	public static List<ZpGroupElement> getSimplePlaintext(final ZpSubgroup zpSubgroup, final int numElements) throws GeneralCryptoLibException {

		boolean isEven = ((numElements % 2) == 0);
		if (!isEven) {
			throw new GeneralCryptoLibException(
					"Number of plaintext elements must be even for simple plaintext equality proofs; Found " + numElements);
		}

		ZpGroupElement randomZpGroupElement = MathematicalTestDataGenerator.getZpGroupElement(zpSubgroup);

		List<ZpGroupElement> plaintext = new ArrayList<>();
		for (int i = 0; i < numElements; i++) {
			plaintext.add(randomZpGroupElement);
		}

		return plaintext;
	}

	/**
	 * ElGamal encrypts a simple plaintext and returns it as a pair of ElGamal ciphertexts suitable for use with simple plaintext zero knowledge
	 * proofs of knowledge.
	 *
	 * @param publicKey the ElGamal public key to be used for the encryption.
	 * @param plaintext the simple plaintext to encrypt.
	 * @return the generated ciphertext pair.
	 * @throws GeneralCryptoLibException if the ElGamal encryption process fails.
	 */
	public static SimplePlaintextCiphertextPair encryptSimplePlaintext(final ElGamalPublicKey publicKey, final List<ZpGroupElement> plaintext)
			throws GeneralCryptoLibException {

		int simplePlaintextLength = plaintext.size();

		boolean isEven = ((simplePlaintextLength % 2) == 0);
		if (!isEven) {
			throw new GeneralCryptoLibException(
					"Number of plaintext elements must be even for simple plaintext equality proofs; Found " + simplePlaintextLength);
		}

		ElGamalEncrypterValues encrypterValues = (ElGamalEncrypterValues) ElGamalTestDataGenerator.encryptGroupElements(publicKey, plaintext);

		Ciphertext primaryCiphertext = getSubCiphertext(encrypterValues, 0, (simplePlaintextLength / 2));
		Ciphertext secondaryCiphertext = getSubCiphertext(encrypterValues, (simplePlaintextLength / 2), simplePlaintextLength);
		Witness witness = encrypterValues;

		return new SimplePlaintextCiphertextPair(primaryCiphertext, secondaryCiphertext, witness);
	}

	/**
	 * Generates a plaintext from a list of base elements and an ephemeral key , generates an ElGamal public key from an equal number of additional
	 * base elements and then ElGamal encrypts the plaintext, using an additional base element and the ephemeral key to generate the gamma element and
	 * using the plaintext, public key and message key to generate the phi elements. The resulting ciphertext can be used in a zero knowledge proof of
	 * knowledge to show that each plaintext element was generated from the same ephemeral key and that each phi element was generated from the same
	 * message key. Note that the list of base elements provided as input must be an odd number for this calculation to work.
	 *
	 * @param zpSubgroup   the Zp subgroup.
	 * @param baseElements the list of base elements used to construct the plaintext and the ElGamal public key.
	 * @param witness1     the witness that wraps the ephemeral key.
	 * @param witness2     the witness that wraps the message key.
	 * @return the encrypted plaintext.
	 * @throws GeneralCryptoLibException if the encryption process fails.
	 */
	public static Ciphertext getPlaintextExponentEqualityProofCiphertext(final ZpSubgroup zpSubgroup, final List<ZpGroupElement> baseElements,
			final Witness witness1, final Witness witness2) throws GeneralCryptoLibException {

		int numBaseElements = baseElements.size();
		boolean numBaseElementsIsOdd = ((numBaseElements % 2) > 0);
		if (!numBaseElementsIsOdd) {
			throw new GeneralCryptoLibException(
					"Number of base elements must be odd for plaintext exponent equality proofs; Found " + numBaseElements);
		}

		int numPlaintextElements = (numBaseElements - 1) / 2;

		Exponent ephemeralKey = witness1.getExponent();
		Exponent messageKey = witness2.getExponent();

		ZpGroupElement gamma = baseElements.get(0).exponentiate(ephemeralKey);

		List<ZpGroupElement> plaintext = new ArrayList<>();
		for (int i = 0; i < numPlaintextElements; i++) {
			ZpGroupElement plaintextElement = baseElements.get(i + 1).exponentiate(ephemeralKey);
			plaintext.add(plaintextElement);
		}

		List<ZpGroupElement> publicKeyElements = new ArrayList<>();
		for (int i = 0; i < numPlaintextElements; i++) {
			ZpGroupElement publicKeyElement = baseElements.get(i + 1 + numPlaintextElements);
			publicKeyElements.add(publicKeyElement);
		}

		List<ZpGroupElement> phis = new ArrayList<>();
		for (int i = 0; i < numPlaintextElements; i++) {
			ZpGroupElement phi = plaintext.get(i).multiply(publicKeyElements.get(i).exponentiate(messageKey));
			phis.add(phi);
		}

		return new CiphertextImpl(gamma, phis);
	}

	private static ElGamalEncrypterValues getSubCiphertext(final ElGamalEncrypterValues encrypterValues, final int fromIndex, final int toIndex)
			throws GeneralCryptoLibException {

		Exponent exponent = encrypterValues.getR();

		ZpGroupElement gamma = encrypterValues.getElGamalCiphertext().getGamma();

		List<ZpGroupElement> phis = encrypterValues.getElGamalCiphertext().getPhis().subList(fromIndex, toIndex);
		ElGamalCiphertext elGamalCiphertext = new ElGamalCiphertext(gamma, phis);

		return new ElGamalEncrypterValues(exponent, elGamalCiphertext);
	}
}
