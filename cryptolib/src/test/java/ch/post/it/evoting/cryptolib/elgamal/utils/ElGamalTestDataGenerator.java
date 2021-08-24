/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.CryptoLibException;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.system.OperatingSystem;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncrypterValues;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.VerifiableElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.WitnessImpl;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ConfigGroupType;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicy;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Ciphertext;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.Witness;
import ch.post.it.evoting.cryptolib.elgamal.factory.CryptoElGamalDecrypter;
import ch.post.it.evoting.cryptolib.elgamal.factory.CryptoElGamalEncrypter;
import ch.post.it.evoting.cryptolib.elgamal.factory.ElGamalFactory;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.utils.MathematicalTestDataGenerator;
import ch.post.it.evoting.cryptolib.primitives.securerandom.configuration.ConfigSecureRandomAlgorithmAndProvider;

/**
 * Utility to generate various types of ElGamal related data needed by tests.
 */
public class ElGamalTestDataGenerator {

	private static ElGamalService elGamalService;

	/**
	 * Retrieves the ElGamal encryption parameters for a pre-generated Zp subgroup.
	 *
	 * @param zpSubgroup the pre-generated Zp subgroup.
	 * @return the ElGamal encryption parameters.
	 * @throws GeneralCryptoLibException if the ElGamal encryption parameters cannot be retrieved.
	 */
	public static ElGamalEncryptionParameters getElGamalEncryptionParameters(final ZpSubgroup zpSubgroup) throws GeneralCryptoLibException {

		return new ElGamalEncryptionParameters(zpSubgroup.getP(), zpSubgroup.getQ(), zpSubgroup.getG());
	}

	/**
	 * Randomly generates an ElGamal key pair for a specified Zp subgroup and of a specified component length.
	 *
	 * @param zpSubgroup the Zp subgroup to which the components of each key to be generated belong.
	 * @param length     the component length of each key to be generated.
	 * @return the generated ElGamal key pair.
	 * @throws GeneralCryptoLibException if the key pair generation process fails.
	 */
	public static ElGamalKeyPair getKeyPair(final ZpSubgroup zpSubgroup, final int length) throws GeneralCryptoLibException {

		ElGamalEncryptionParameters encryptionParams = new ElGamalEncryptionParameters(zpSubgroup.getP(), zpSubgroup.getQ(), zpSubgroup.getG());

		return getElGamalService().generateKeyPair(encryptionParams, length);
	}

	/**
	 * Randomly generates a witness object for a specified Zp subgroup.
	 *
	 * @param zpSubgroup the Zp subgroup to which the exponent of the generated witness is to belong.
	 * @return the generated witness.
	 * @throws GeneralCryptoLibException if the witness generation process fails.
	 */
	public static Witness getWitness(final ZpSubgroup zpSubgroup) throws GeneralCryptoLibException {

		Exponent randomExponent = MathematicalTestDataGenerator.getExponent(zpSubgroup);

		return new WitnessImpl(randomExponent);
	}

	/**
	 * ElGamal encrypts a list of Zp group elements using a specified ElGamal public key.
	 *
	 * @param publicKey       the ElGamal public key used for the encryption.
	 * @param zpGroupElements the list of Zp group elements to encrypt.
	 * @return the encrypted Zp group elements.
	 * @throws GeneralCryptoLibException if the ElGamal encryption process fails.
	 */
	public static Ciphertext encryptGroupElements(final ElGamalPublicKey publicKey, final List<ZpGroupElement> zpGroupElements)
			throws GeneralCryptoLibException {

		return getElGamalService().createEncrypter(publicKey).encryptGroupElements(zpGroupElements);
	}

	/**
	 * Pre-computes the ElGamal encrypter values for a specified ElGamal public key.
	 *
	 * @param publicKey the ElGamal public key used for the encryption.
	 * @return the pre-computed encrypter values.
	 * @throws GeneralCryptoLibException if the pre-computation process fails.
	 */
	public static ElGamalEncrypterValues preComputeEncrypterValues(final ElGamalPublicKey publicKey) throws GeneralCryptoLibException {

		return getElGamalService().createEncrypter(publicKey).preCompute();
	}

	/**
	 * ElGamal encrypts a list of Zp group elements, using a specified ElGamal public key and using pre-computed values.
	 *
	 * @param publicKey         the ElGamal public key used for the encryption.
	 * @param zpGroupElements   the list of Zp group elements to encrypt.
	 * @param preComputedValues the pre-computed values.
	 * @return the encrypted Zp group elements.
	 * @throws GeneralCryptoLibException if the ElGamal encryption process fails.
	 */
	public static Ciphertext encryptGroupElements(final ElGamalPublicKey publicKey, final List<ZpGroupElement> zpGroupElements,
			final ElGamalEncrypterValues preComputedValues) throws GeneralCryptoLibException {

		return getElGamalService().createEncrypter(publicKey).encryptGroupElements(zpGroupElements, preComputedValues);
	}

	/**
	 * Creates an ElGamal encrypter for use with short exponents, for a specified ElGamal public key, which also must be for short exponents.
	 *
	 * @param publicKey the ElGamal public key for use with short exponents.
	 * @return the ElGamal encrypter for use with short exponents.
	 * @throws GeneralCryptoLibException if the encrypter cannot be created.
	 */
	public static CryptoElGamalEncrypter getEncrypterForShortExponents(final ElGamalPublicKey publicKey) {

		ElGamalFactory elGamalFactory = new ElGamalFactory(getElGamalPolicyForQr2048Group());

		return elGamalFactory.createEncrypter(publicKey);
	}

	/**
	 * Creates an ElGamal decrypter for use with short exponents, for a specified ElGamal private key, which also must be for short exponents.
	 *
	 * @param privateKey the ElGamal private key for use with short exponents.
	 * @return the ElGamal decrypter for use with short exponents.
	 * @throws GeneralCryptoLibException if the decrypter cannot be created.
	 */
	public static CryptoElGamalDecrypter getDecrypterForShortExponents(final ElGamalPrivateKey privateKey) {

		ElGamalFactory elGamalFactory = new ElGamalFactory(getElGamalPolicyForQr2048Group());

		return elGamalFactory.createDecrypter(privateKey);
	}

	/**
	 * Creates an instance of {@link VerifiableElGamalEncryptionParameters} from a JSON string.
	 *
	 * @return {@link VerifiableElGamalEncryptionParameters}
	 * @throws GeneralCryptoLibException
	 */
	public static VerifiableElGamalEncryptionParameters getElGamalVerifiableEncryptionParameters() throws GeneralCryptoLibException, IOException {

		String str;

		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream("verifiable-encryption-parameters.json")))) {
			str = reader.lines().collect(Collectors.joining());
		}

		return VerifiableElGamalEncryptionParameters.fromJson(str);
	}

	private static ElGamalPolicy getElGamalPolicyForQr2048Group() {

		return new ElGamalPolicy() {
			@Override
			public ConfigGroupType getGroupType() {
				return ConfigGroupType.QR_2048;
			}

			@Override
			public ConfigSecureRandomAlgorithmAndProvider getSecureRandomAlgorithmAndProvider() {

				switch (OperatingSystem.current()) {
				case WINDOWS:
					return ConfigSecureRandomAlgorithmAndProvider.PRNG_SUN_MSCAPI;
				case UNIX:
					return ConfigSecureRandomAlgorithmAndProvider.NATIVE_PRNG_SUN;
				default:
					throw new CryptoLibException("OS not supported");
				}
			}
		};
	}

	/**
	 * @return the ElGamal service used by the functions in the class.
	 * @throws GeneralCryptoLibException if something goes wrong
	 */
	private static ElGamalServiceAPI getElGamalService() {
		if (null == elGamalService) {
			elGamalService = new ElGamalService();
		}

		return elGamalService;
	}
}
