/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.ballotbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;

import org.bouncycastle.cms.CMSException;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.domain.common.ConfigurationInput;
import ch.post.it.evoting.sdm.readers.ConfigurationInputReader;
import ch.post.it.evoting.sdm.utils.EncryptionParametersLoader;
import ch.post.it.evoting.sdm.utils.KeyStoreReader;
import ch.post.it.evoting.sdm.utils.X509CertificateLoader;

public class BallotBoxHolderInitializer {

	private final ConfigurationInputReader configurationInputReader;

	private final X509CertificateLoader x509CertificateLoader;

	private final KeyStoreReader keyStoreReader;

	private final EncryptionParametersLoader encryptionParametersLoader;

	public BallotBoxHolderInitializer(final ConfigurationInputReader configurationInputReader, final X509CertificateLoader x509CertificateLoader,
			final EncryptionParametersLoader encryptionParametersLoader, final KeyStoreReader keyStoreReader) {

		this.configurationInputReader = configurationInputReader;
		this.x509CertificateLoader = x509CertificateLoader;
		this.encryptionParametersLoader = encryptionParametersLoader;
		this.keyStoreReader = keyStoreReader;
	}

	public void init(final BallotBoxParametersHolder holder, final InputStream configurationInputStream)
			throws IOException, GeneralSecurityException, GeneralCryptoLibException, CMSException {

		final ConfigurationInput configurationInput = getConfigurationInput(configurationInputStream);
		initFromConfigurationInput(holder, configurationInput);
	}

	public void init(final BallotBoxParametersHolder holder, final File configurationInputFile)
			throws IOException, GeneralSecurityException, GeneralCryptoLibException, CMSException {

		try (InputStream is = Files.newInputStream(configurationInputFile.toPath())) {
			final ConfigurationInput configurationInput = getConfigurationInput(is);
			initFromConfigurationInput(holder, configurationInput);
		}
	}

	private void initFromConfigurationInput(final BallotBoxParametersHolder holder, final ConfigurationInput configurationInput)
			throws IOException, GeneralSecurityException, GeneralCryptoLibException, CMSException {

		final CredentialProperties credentialPropertiesServicesCA = configurationInput.getConfigProperties()
				.get(Constants.CONFIGURATION_SERVICES_CA_JSON_TAG);

		final CredentialProperties credentialPropertiesElectionCA = configurationInput.getConfigProperties()
				.get(Constants.CONFIGURATION_ELECTION_CA_JSON_TAG);

		final String aliasPrivateKey = credentialPropertiesServicesCA.getAlias().get(Constants.CONFIGURATION_SERVICES_CA_PRIVATE_KEY_JSON_TAG);

		final String nameServicesCA = credentialPropertiesServicesCA.getName();

		final String nameElectionCA = credentialPropertiesElectionCA.getName();

		final String pemFileServicesCA = nameServicesCA + Constants.PEM;

		final String pemFileElectionCA = nameElectionCA + Constants.PEM;

		final Path absolutePath = holder.getOutputPath();
		final CryptoAPIX509Certificate servicesCACert = x509CertificateLoader
				.load(absolutePath.resolve(Constants.CONFIG_DIR_NAME_OFFLINE).resolve(pemFileServicesCA));

		final CryptoAPIX509Certificate electionCACert = x509CertificateLoader
				.load(absolutePath.resolve(Constants.CONFIG_DIR_NAME_OFFLINE).resolve(pemFileElectionCA));

		final Path servicesKeyStorePath = absolutePath.resolve(Constants.CONFIG_DIR_NAME_OFFLINE).resolve(Constants.SERVICES_SIGNER_SKS_FILENAME);
		final Path passwordPath = absolutePath.resolve(Constants.CONFIG_DIR_NAME_OFFLINE).resolve(Constants.PW_TXT);

		final PrivateKey servicesCAPrivateKey = keyStoreReader.getPrivateKey(servicesKeyStorePath, passwordPath, nameServicesCA, aliasPrivateKey);

		final CredentialProperties ballotBoxCredentialProperties = configurationInput.getBallotBox();

		// absolutePath points to election event folder. encryption params is
		// now inside
		final EncryptionParameters encParams = encryptionParametersLoader.load(absolutePath);

		holder.setServicesCAPrivateKey(servicesCAPrivateKey);
		holder.setBallotBoxCredentialProperties(ballotBoxCredentialProperties);
		holder.setEncryptionParameters(encParams);
		holder.setServicesCACert(servicesCACert);
		holder.setElectionCACert(electionCACert);
	}

	private ConfigurationInput getConfigurationInput(final InputStream configurationInputStream) throws IOException {
		return configurationInputReader.fromStreamToJava(configurationInputStream);
	}
}
