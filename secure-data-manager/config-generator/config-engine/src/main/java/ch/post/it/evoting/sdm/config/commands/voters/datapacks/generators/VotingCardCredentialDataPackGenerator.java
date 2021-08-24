/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators;

import java.security.KeyPair;
import java.util.Properties;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomString;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.factory.X509CertificateGenerator;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.domain.election.helpers.ReplacementsHolder;
import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.logging.core.factory.LoggingFactoryLog4j;
import ch.post.it.evoting.logging.core.formatter.PipeSeparatedFormatter;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VotingCardCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VotingCardCredentialInputDataPack;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateCredentialDataException;
import ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents;
import ch.post.it.evoting.sdm.datapacks.generators.CredentialDataPackGenerator;
import ch.post.it.evoting.sdm.logging.ExecutionTimeLogger;

public class VotingCardCredentialDataPackGenerator extends CredentialDataPackGenerator {

	private final LoggingWriter loggingWriter;

	public VotingCardCredentialDataPackGenerator(final AsymmetricServiceAPI asymmetricService, final X509CertificateGenerator certificateGenerator,
			final KeyStoreService storesService, final CryptoAPIRandomString cryptoRandomString) {
		super(asymmetricService, cryptoRandomString, certificateGenerator, storesService);

		final MessageFormatter formatter = new PipeSeparatedFormatter("OV", "CS");
		final LoggingFactory loggerFactory = new LoggingFactoryLog4j(formatter);
		this.loggingWriter = loggerFactory.getLogger(VotingCardCredentialDataPackGenerator.class);
	}

	public VotingCardCredentialDataPack generate(final VotingCardCredentialInputDataPack inputDataPack, final ReplacementsHolder replacementsHolder,
			final char[] keystoreSymmetricEncryptionKey, final String credentialID, final String votingCardSetID,
			Properties credentialSignCertificateProperties, final Properties credentialAuthCertificateProperties,
			final CryptoAPIX509Certificate... parentCerts) throws GeneralCryptoLibException {

		final VotingCardCredentialDataPack dataPack = new VotingCardCredentialDataPack();

		ExecutionTimeLogger timer = new ExecutionTimeLogger("generateVotingCardCredentialDataPack-Detailed");
		KeyPair keyPairSign;
		try {
			keyPairSign = asymmetricService.getKeyPairForSigning();

			loggingWriter.log(Level.DEBUG,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENCREDAT_SUCCESS_CREDENTIAL_SIGNING_KEYPAIR_GENERATED)
							.electionEvent(inputDataPack.getEeid()).user(Constants.ADMIN_ID).objectId(votingCardSetID)
							.additionalInfo(Constants.C_ID, credentialID).createLogInfo());

			dataPack.setKeyPairSign(keyPairSign);

		} catch (Exception e) {

			loggingWriter.log(Level.ERROR,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENCREDAT_ERROR_GENERATING_CREDENTIAL_SIGNING_KEYPAIR)
							.electionEvent(inputDataPack.getEeid()).user(Constants.ADMIN_ID).objectId(votingCardSetID)
							.additionalInfo(Constants.C_ID, credentialID).additionalInfo(Constants.ERR_DESC, e.getMessage()).createLogInfo());

			throw new GenerateCredentialDataException(e);
		}
		timer.log("generateKeyPairForSigning");
		KeyPair keyPairAuth;
		try {
			keyPairAuth = asymmetricService.getKeyPairForSigning();

			loggingWriter.log(Level.DEBUG, new LogContent.LogContentBuilder()
					.logEvent(ConfigGeneratorLogEvents.GENCREDAT_SUCCESS_CREDENTIAL_ID_AUTHENTICATION_KEYPAIR_GENERATED)
					.electionEvent(inputDataPack.getEeid()).user(Constants.ADMIN_ID).objectId(votingCardSetID)
					.additionalInfo(Constants.C_ID, credentialID).createLogInfo());

			dataPack.setKeyPairAuth(keyPairAuth);

		} catch (Exception e) {

			loggingWriter.log(Level.ERROR, new LogContent.LogContentBuilder()
					.logEvent(ConfigGeneratorLogEvents.GENCREDAT_ERROR_GENERATING_CREDENTIAL_ID_AUTHENTICATION_KEYPAIR)
					.electionEvent(inputDataPack.getEeid()).user(Constants.ADMIN_ID).objectId(votingCardSetID)
					.additionalInfo(Constants.C_ID, credentialID).additionalInfo(Constants.ERR_DESC, e.getMessage()).createLogInfo());

			throw new GenerateCredentialDataException(e);
		}
		timer.log("generateKeyPairForAuthentication");
		final CertificateParameters certificateParametersSign = getCertificateParameters(inputDataPack.getCredentialSignProperties(),
				inputDataPack.getStartDate(), inputDataPack.getEndDate(), replacementsHolder, credentialSignCertificateProperties);

		CryptoAPIX509Certificate certificateSign;
		try {
			certificateSign = createX509Certificate(inputDataPack, certificateParametersSign, keyPairSign);

			loggingWriter.log(Level.DEBUG, new LogContent.LogContentBuilder()
					.logEvent(ConfigGeneratorLogEvents.GENCREDAT_SUCCESS_CREDENTIAL_ID_SIGNING_CERTIFICATE_GENERATED)
					.electionEvent(inputDataPack.getEeid()).user(Constants.ADMIN_ID).objectId(votingCardSetID)
					.additionalInfo(Constants.C_ID, credentialID).additionalInfo(Constants.CERT_CN, certificateSign.getSubjectDn().getCommonName())
					.additionalInfo(Constants.CERT_SN, certificateSign.getSerialNumber().toString()).createLogInfo());

			dataPack.setCertificateSign(certificateSign);

		} catch (Exception e) {

			loggingWriter.log(Level.ERROR,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENCREDAT_ERROR_GENERATING_CREDENTIAL_ID_SIGNING_CERTIFICATE)
							.electionEvent(inputDataPack.getEeid()).user(Constants.ADMIN_ID).objectId(votingCardSetID)
							.additionalInfo(Constants.C_ID, credentialID)
							.additionalInfo(Constants.CERT_CN, certificateParametersSign.getUserSubjectCn())
							.additionalInfo(Constants.ERR_DESC, e.getMessage()).createLogInfo());

			throw new GenerateCredentialDataException(e);
		}

		timer.log("signCertificate");
		CryptoAPIX509Certificate certificateAuth;

		final CertificateParameters certificateParametersAuth = getCertificateParameters(inputDataPack.getCredentialAuthProperties(),
				inputDataPack.getStartDate(), inputDataPack.getEndDate(), replacementsHolder, credentialAuthCertificateProperties);

		try {

			certificateAuth = createX509Certificate(inputDataPack, certificateParametersAuth, keyPairAuth);

			loggingWriter.log(Level.DEBUG, new LogContent.LogContentBuilder()
					.logEvent(ConfigGeneratorLogEvents.GENCREDAT_SUCCESS_CREDENTIAL_ID_AUTHENTICATION_CERTIFICATE_GENERATED)
					.electionEvent(inputDataPack.getEeid()).user(Constants.ADMIN_ID).objectId(votingCardSetID)
					.additionalInfo(Constants.C_ID, credentialID).additionalInfo(Constants.CERT_CN, certificateAuth.getSubjectDn().getCommonName())
					.additionalInfo(Constants.CERT_SN, certificateAuth.getSerialNumber().toString()).createLogInfo());

			dataPack.setCertificateAuth(certificateAuth);

		} catch (Exception e) {

			loggingWriter.log(Level.ERROR, new LogContent.LogContentBuilder()
					.logEvent(ConfigGeneratorLogEvents.GENCREDAT_ERROR_GENERATING_CREDENTIAL_ID_AUTHENTICATION_CERTIFICATE)
					.electionEvent(inputDataPack.getEeid()).user(Constants.ADMIN_ID).objectId(votingCardSetID)
					.additionalInfo(Constants.C_ID, credentialID)
					.additionalInfo(Constants.CERT_CN, certificateParametersAuth.getUserSubjectDn().getCommonName())
					.additionalInfo(Constants.ERR_DESC, e.getMessage()).createLogInfo());

			throw new GenerateCredentialDataException(e);
		}

		timer.log("certificateAuth");

		CryptoAPIExtendedKeyStore keyStore = storesService.createKeyStore();

		final CryptoAPIX509Certificate[] certs;

		certs = new CryptoAPIX509Certificate[parentCerts.length + 1];

		certs[0] = certificateSign;
		System.arraycopy(parentCerts, 0, certs, 1, parentCerts.length);

		setPrivateKeyToKeystore(keyStore, inputDataPack.getCredentialSignProperties().obtainPrivateKeyAlias(), keyPairSign.getPrivate(),
				keystoreSymmetricEncryptionKey, certs);

		certs[0] = certificateAuth;

		setPrivateKeyToKeystore(keyStore, inputDataPack.getCredentialAuthProperties().obtainPrivateKeyAlias(), keyPairAuth.getPrivate(),
				keystoreSymmetricEncryptionKey, certs);

		dataPack.setKeystoreToBeSerialized(keyStore, keystoreSymmetricEncryptionKey);

		timer.log("createKeyStores");
		return dataPack;
	}
}
