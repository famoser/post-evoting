/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.service;

import java.math.BigInteger;
import java.util.Properties;

import ch.post.it.evoting.cryptolib.CryptolibService;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalEncryptionParameters;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPrivateKey;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ConfigGroupType;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicy;
import ch.post.it.evoting.cryptolib.elgamal.configuration.ElGamalPolicyFromProperties;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalDecrypter;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalEncrypter;
import ch.post.it.evoting.cryptolib.elgamal.cryptoapi.CryptoAPIElGamalKeyPairGenerator;
import ch.post.it.evoting.cryptolib.elgamal.factory.CryptoElGamalKeyPairGenerator;
import ch.post.it.evoting.cryptolib.elgamal.factory.ElGamalFactory;
import ch.post.it.evoting.cryptolib.mathematical.MathematicalUtils;
import ch.post.it.evoting.cryptolib.mathematical.groups.MathematicalGroup;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Class which provides high-level ElGamal cryptographic services.
 */
public final class ElGamalService extends CryptolibService implements ElGamalServiceAPI {

	private final ElGamalFactory elGamalFactory;

	private final ElGamalPolicy policy;

	private CryptoElGamalKeyPairGenerator keyPairGenerator;

	/**
	 * Creates and instance of service and initializes all properties according to the properties specified by {@link
	 * ch.post.it.evoting.cryptolib.commons.configuration.PolicyFromPropertiesHelper#CRYPTOLIB_POLICY_PROPERTIES_FILE_PATH}.
	 */
	public ElGamalService() {
		policy = new ElGamalPolicyFromProperties();
		elGamalFactory = new ElGamalFactory(policy);
	}

	/**
	 * Constructor which initializes its state using the properties file located at the specified path.
	 *
	 * @param properties the properties to be used to configure the service.
	 */
	public ElGamalService(Properties properties) {
		policy = new ElGamalPolicyFromProperties(properties);
		elGamalFactory = new ElGamalFactory(policy);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This method is equivalent to the crypto-primitives' GenKeyPair method.
	 */
	@Override
	public ElGamalKeyPair generateKeyPair(final ElGamalEncryptionParameters encryptionParameters, final int keyCount)
			throws GeneralCryptoLibException {
		// Validate the encryption parameters' cryptographic strength.
		checkGroupPolicy(encryptionParameters.getGroup());

		// Generate the key pair.
		return getKeyPairGenerator().generateKeys(encryptionParameters, keyCount);
	}

	@Override
	public CryptoAPIElGamalEncrypter createEncrypter(final ElGamalPublicKey publicKey) throws GeneralCryptoLibException {

		Validate.notNull(publicKey, "ElGamal public key");
		final ZpSubgroup group = publicKey.getGroup();
		checkGroupPolicy(group);
		publicKey.getKeys().forEach(element -> MathematicalUtils.checkGroupMembership(group, element));

		return elGamalFactory.createEncrypter(publicKey);
	}

	@Override
	public CryptoAPIElGamalDecrypter createDecrypter(final ElGamalPrivateKey privateKey) throws GeneralCryptoLibException {

		Validate.notNull(privateKey, "ElGamal private key");
		final ZpSubgroup group = privateKey.getGroup();
		checkGroupPolicy(group);
		privateKey.getKeys().forEach(exponent -> MathematicalUtils.checkGroupMembership(group, exponent));

		return elGamalFactory.createDecrypter(privateKey);
	}

	@Override
	public void checkGroupPolicy(MathematicalGroup<?> group) {
		ConfigGroupType groupType = policy.getGroupType();
		BigInteger computedP = BigInteger.valueOf(2).multiply(group.getQ()).add(BigInteger.ONE);

		if ((group.getQ().bitLength() != groupType.getN()) || !group.getP().equals(computedP)) {
			throw new IllegalArgumentException("The provided quadratic residue group does not match the current policy");
		}
	}

	/**
	 * @return an on-demand instance of the ElGamal key pair generator.
	 */
	private CryptoAPIElGamalKeyPairGenerator getKeyPairGenerator() {
		if (null == keyPairGenerator) {
			keyPairGenerator = elGamalFactory.createCryptoElGamalKeyPairGenerator();
		}

		return keyPairGenerator;
	}
}
