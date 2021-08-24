/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.ballotbox;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Properties;

import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans.ElectionInputDataPack;

/**
 * Encapsulates the parameters required by this command:
 * <ul>
 * <li>a {@link Ballot} ID.
 * <li>a list of ballot box IDs.
 * <li>the absolute output path as a {@code Path}.
 * <li>the signer private key.
 * </ul>
 */
public class BallotBoxParametersHolder {

	private final String ballotBoxID;
	private final String alias;
	private final Path outputPath;
	private final String ballotID;
	private final String electoralAuthorityID;
	private final String eeID;
	private final ElectionInputDataPack electionInputDataPack;
	private final String writeInAlphabet;
	private final Properties certificateProperties;

	private PrivateKey signerPrivateKey;
	private CredentialProperties ballotBoxCredentialProperties;
	private EncryptionParameters encParams;
	private CryptoAPIX509Certificate servicesCACert;
	private CryptoAPIX509Certificate electionCACert;
	private String keyForProtectingKeystorePassword;
	private String test;
	private String gracePeriod;

	public BallotBoxParametersHolder(final String ballotID, final String electoralAuthorityID, final String ballotBoxID, final String alias,
			final Path outputPath, final String eeID, final ElectionInputDataPack electionInputDataPack, final String test, final String gracePeriod,
			final String writeInAlphabet, Properties certificateProperties) {
		this.ballotID = ballotID;
		this.electoralAuthorityID = electoralAuthorityID;
		this.ballotBoxID = ballotBoxID;
		this.alias = alias;
		this.outputPath = outputPath;
		this.eeID = eeID;
		this.electionInputDataPack = electionInputDataPack;
		this.test = test;
		this.gracePeriod = gracePeriod;
		this.writeInAlphabet = writeInAlphabet;
		this.certificateProperties = certificateProperties;
	}

	public String getBallotBoxID() {
		return ballotBoxID;
	}

	public Path getOutputPath() {
		return outputPath;
	}

	public String getBallotID() {
		return ballotID;
	}

	public String getElectoralAuthorityID() {
		return electoralAuthorityID;
	}

	public PrivateKey getSignerPrivateKey() {
		return signerPrivateKey;
	}

	public CredentialProperties getBallotBoxCredentialProperties() {
		return ballotBoxCredentialProperties;
	}

	public void setBallotBoxCredentialProperties(final CredentialProperties ballotBoxCredentialProperties) {
		this.ballotBoxCredentialProperties = ballotBoxCredentialProperties;
	}

	public String getEeID() {
		return eeID;
	}

	public ElectionInputDataPack getInputDataPack() {
		return electionInputDataPack;
	}

	public EncryptionParameters getEncryptionParameters() {
		return encParams;
	}

	public void setEncryptionParameters(final EncryptionParameters encryptionParameters) {
		encParams = encryptionParameters;
	}

	public CryptoAPIX509Certificate getServicesCACert() {
		return servicesCACert;
	}

	public void setServicesCACert(final CryptoAPIX509Certificate servicesCACert) {
		this.servicesCACert = servicesCACert;
	}

	public CryptoAPIX509Certificate getElectionCACert() {
		return electionCACert;
	}

	public void setElectionCACert(final CryptoAPIX509Certificate electionCACert) {
		this.electionCACert = electionCACert;
	}

	public String getKeyForProtectingKeystorePassword() {
		return keyForProtectingKeystorePassword;
	}

	public void setKeyForProtectingKeystorePassword(final String keyForProtectingKeystorePassword) {
		this.keyForProtectingKeystorePassword = keyForProtectingKeystorePassword;
	}

	public String getTest() {
		return test;
	}

	public void setTest(String test) {
		this.test = test;
	}

	public void setServicesCAPrivateKey(final PrivateKey servicesCAPrivateKey) {
		signerPrivateKey = servicesCAPrivateKey;
	}

	public String getGracePeriod() {
		return gracePeriod;
	}

	public void setGracePeriod(String gracePeriod) {
		this.gracePeriod = gracePeriod;
	}

	public String getAlias() {
		return alias;
	}

	public String getWriteInAlphabet() {
		return writeInAlphabet;
	}

	public Properties getCertificateProperties() {
		return certificateProperties;
	}
}
