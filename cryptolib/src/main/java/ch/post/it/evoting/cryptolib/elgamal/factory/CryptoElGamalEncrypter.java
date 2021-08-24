/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.factory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalCiphertext;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalEncrypter;
import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponents;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.primitives.securerandom.factory.CryptoRandomInteger;

/**
 * Implementation of an ElGamal encrypter.
 */
public final class CryptoElGamalEncrypter implements CryptoAPIElGamalEncrypter {

	private static final String LIST_MATHEMATICAL_GROUP_ELEMENTS_LABEL = "List of mathematical group elements to ElGamal encrypt";
	private static final String NUMBER_MATHEMATICAL_GROUP_ELEMENTS_LABEL = "Number of mathematical group elements to ElGamal encrypt";
	private static final String ENCRYPTED_PUBLIC_KEY_LENGTH_LABEL = "encrypter public key length";
	private final ElGamalPublicKey elGamalPublicKey;
	private final int elGamalPublicKeyLength;
	private final CryptoRandomInteger cryptoRandomInteger;

	/**
	 * Creates an ElGamal encrypter.
	 *
	 * @param elGamalPublicKey    the ElGamal public key.
	 * @param cryptoRandomInteger a generator of random integers.
	 */
	CryptoElGamalEncrypter(final ElGamalPublicKey elGamalPublicKey, final CryptoRandomInteger cryptoRandomInteger) {

		this.elGamalPublicKey = elGamalPublicKey;
		elGamalPublicKeyLength = this.elGamalPublicKey.getKeys().size();
		this.cryptoRandomInteger = cryptoRandomInteger;
	}

	private static ElGamalCiphertext compute(final List<ZpGroupElement> messages, final ElGamalCiphertext computationValues)
			throws GeneralCryptoLibException {

		ElGamalCiphertext compressedComputationValues = getCompressedList(messages.size(), computationValues);

		List<ZpGroupElement> phis = new ArrayList<>(messages.size());

		// For each element in the array of messages, compute the
		// following:
		// element:phi[i]=message[i]*prePhi[i]
		for (int i = 0; i < compressedComputationValues.getPhis().size(); i++) {
			phis.add(messages.get(i).multiply(compressedComputationValues.getPhis().get(i)));
		}

		return new ElGamalCiphertext(compressedComputationValues.getGamma(), phis);
	}

	private static ElGamalCiphertext getCompressedList(final int numMessages, final ElGamalCiphertext preComputationValues)
			throws GeneralCryptoLibException {

		List<ZpGroupElement> phis = preComputationValues.getPhis();

		if (phis.size() <= numMessages) {
			return preComputationValues;
		}

		GroupElementsCompressor<ZpGroupElement> compressor = new GroupElementsCompressor<>();

		List<ZpGroupElement> compressedList = compressor.buildListWithCompressedFinalElement(numMessages, phis);

		return new ElGamalCiphertext(preComputationValues.getGamma(), compressedList);
	}

	private static BigInteger parseAsBigInteger(final String stringToParse) {

		try {
			return new BigInteger(stringToParse);
		} catch (NumberFormatException nfe) {
			throw new CryptoLibException("'" + stringToParse + " 'is not a valid representation of a BigInteger.", nfe);
		}
	}

	@Override
	public ElGamalEncrypterValues encryptGroupElements(final List<ZpGroupElement> messages) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(messages, LIST_MATHEMATICAL_GROUP_ELEMENTS_LABEL);
		Validate.notGreaterThan(messages.size(), elGamalPublicKeyLength, NUMBER_MATHEMATICAL_GROUP_ELEMENTS_LABEL, ENCRYPTED_PUBLIC_KEY_LENGTH_LABEL);

		return encryptGroupElements(messages, false);
	}

	@Override
	public ElGamalEncrypterValues encryptGroupElementsWithShortExponent(final List<ZpGroupElement> messages) throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(messages, LIST_MATHEMATICAL_GROUP_ELEMENTS_LABEL);
		Validate.notGreaterThan(messages.size(), elGamalPublicKeyLength, NUMBER_MATHEMATICAL_GROUP_ELEMENTS_LABEL, ENCRYPTED_PUBLIC_KEY_LENGTH_LABEL);

		return encryptGroupElements(messages, true);
	}

	private ElGamalEncrypterValues encryptGroupElements(final List<ZpGroupElement> messages, final boolean useShortExponent)
			throws GeneralCryptoLibException {

		ElGamalEncrypterValues preComputeValues = preCompute(useShortExponent);

		ElGamalCiphertext elGamalCiphertext = compute(messages, preComputeValues.getElGamalCiphertext());

		return new ElGamalEncrypterValues(preComputeValues.getR(), elGamalCiphertext);
	}

	@Override
	public ElGamalEncrypterValues encryptGroupElements(final List<ZpGroupElement> messages, final ElGamalEncrypterValues preComputedValues)
			throws GeneralCryptoLibException {

		Validate.notNullOrEmptyAndNoNulls(messages, LIST_MATHEMATICAL_GROUP_ELEMENTS_LABEL);
		Validate.notGreaterThan(messages.size(), elGamalPublicKeyLength, NUMBER_MATHEMATICAL_GROUP_ELEMENTS_LABEL, ENCRYPTED_PUBLIC_KEY_LENGTH_LABEL);
		Validate.notNull(preComputedValues, "ElGamal pre-computed values object");

		ElGamalCiphertext preComputedElGamalCiphertext = preComputedValues.getElGamalCiphertext();

		Validate.notGreaterThan(preComputedElGamalCiphertext.getPhis().size(), elGamalPublicKey.getKeys().size(),
				"Number of ElGamal pre-computed phi elements ", ENCRYPTED_PUBLIC_KEY_LENGTH_LABEL);

		ElGamalCiphertext elGamalCiphertext = compute(messages, preComputedElGamalCiphertext);

		return new ElGamalEncrypterValues(preComputedValues.getR(), elGamalCiphertext);
	}

	@Override
	public ElGamalEncrypterValues encryptStrings(final List<String> messages) throws GeneralCryptoLibException {

		return encryptGroupElements(getListAsZpGroupElements(messages));
	}

	@Override
	public ElGamalEncrypterValues encryptStrings(final List<String> messages, final ElGamalEncrypterValues preComputedValues)
			throws GeneralCryptoLibException {

		Validate.notNull(preComputedValues, "ElGamal pre-computed values object");

		return encryptGroupElements(getListAsZpGroupElements(messages), preComputedValues);
	}

	@Override
	public ElGamalEncrypterValues preCompute() {
		return preCompute(false);
	}

	private ElGamalEncrypterValues preCompute(final boolean useShortExponent) {
		List<ZpGroupElement> publicKeys = elGamalPublicKey.getKeys();
		ZpSubgroup publicKeyGroup = elGamalPublicKey.getGroup();

		Exponent randomExponent;
		ZpGroupElement gamma;
		List<ZpGroupElement> prePhis;

		try {
			// Generate a random exponent
			if (useShortExponent) {
				randomExponent = Exponents.shortRandom(publicKeyGroup, cryptoRandomInteger);
			} else {
				randomExponent = Exponents.random(publicKeyGroup, cryptoRandomInteger);
			}

			// Generate the first element of the ciphertext, gamma=g^random
			gamma = publicKeyGroup.getGenerator().exponentiate(randomExponent);

			// For each element in the array of public keys, compute the
			// following:
			// element:prephi[i]=pubKey[i]^(random)
			ZpGroupElement pubKeyRaised;
			prePhis = new ArrayList<>(publicKeys.size());

			for (ZpGroupElement publicKey : publicKeys) {
				pubKeyRaised = publicKey.exponentiate(randomExponent);
				prePhis.add(pubKeyRaised);
			}

		} catch (GeneralCryptoLibException e) {
			throw new CryptoLibException(e);
		}

		try {
			return new ElGamalEncrypterValues(randomExponent, new ElGamalCiphertext(gamma, prePhis));
		} catch (GeneralCryptoLibException e) {
			throw new CryptoLibException("Could not perform pre-computation of ElGamal encryption values.", e);
		}
	}

	private List<ZpGroupElement> getListAsZpGroupElements(final List<String> messages) throws GeneralCryptoLibException {

		Validate.notNullOrEmpty(messages, "List of stringified mathematical group elements to ElGamal encrypt");
		for (String elementStr : messages) {
			Validate.notNullOrBlank(elementStr, "A stringified mathematical group element to ElGamal encrypt");
		}
		Validate.notGreaterThan(messages.size(), elGamalPublicKeyLength, NUMBER_MATHEMATICAL_GROUP_ELEMENTS_LABEL, ENCRYPTED_PUBLIC_KEY_LENGTH_LABEL);

		List<ZpGroupElement> valuesAsZpGroupElements = new ArrayList<>();
		for (String elementStr : messages) {
			BigInteger parsedBigInteger = parseAsBigInteger(elementStr);
			valuesAsZpGroupElements.add(new ZpGroupElement(parsedBigInteger, elGamalPublicKey.getGroup()));
		}

		return valuesAsZpGroupElements;
	}
}
