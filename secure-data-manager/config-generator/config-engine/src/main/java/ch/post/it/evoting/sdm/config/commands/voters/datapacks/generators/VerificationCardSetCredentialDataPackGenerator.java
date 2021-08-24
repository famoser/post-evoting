/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators;

import static ch.post.it.evoting.sdm.commons.Constants.SEMICOLON;

import java.security.KeyPair;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.asymmetric.AsymmetricServiceAPI;
import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomString;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.factory.X509CertificateGenerator;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialInputDataPack;
import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateCredentialDataException;
import ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents;

public class VerificationCardSetCredentialDataPackGenerator extends ElGamalCredentialDataPackGenerator {

	private static final Logger LOGGER = LoggerFactory.getLogger(VerificationCardSetCredentialDataPackGenerator.class);

	public VerificationCardSetCredentialDataPackGenerator(final AsymmetricServiceAPI asymmetricService,
			final X509CertificateGenerator certificateGenerator, final KeyStoreService storesService, final CryptoAPIRandomString cryptoRandomString,
			final ElGamalServiceAPI elGamalService) {
		super(asymmetricService, cryptoRandomString, certificateGenerator, storesService, elGamalService);
	}

	public VerificationCardSetCredentialDataPack generate(final VerificationCardSetCredentialInputDataPack inputDataPack,
			final String verificationCardSetID, final String choiceCodesEncryptionKeyAsConcatenatedString,
			final Properties verificationCardSetCerificateProperties) {

		final VerificationCardSetCredentialDataPack dataPack = new VerificationCardSetCredentialDataPack();

		KeyPair verificationCardSetIssuerKeyPair;
		try {

			verificationCardSetIssuerKeyPair = asymmetricService.getKeyPairForSigning();

			LOGGER.info(ConfigGeneratorLogEvents.GENVCD_SUCCESS_GENERATING_VERIFICATION_CARD_SET_ISSUER_KEYPAIR.getInfo(), inputDataPack.getEeid(),
					Constants.ADMIN_ID, Constants.VERIFCS_ID, verificationCardSetID);

			dataPack.setVerificationCardSetIssuerKeyPair(verificationCardSetIssuerKeyPair);

		} catch (Exception e) {

			LOGGER.error(ConfigGeneratorLogEvents.GENVCD_ERROR_GENERATING_VERIFICATION_CARD_SET_ISSUER_KEYPAIR.getInfo(), inputDataPack.getEeid(),
					Constants.ADMIN_ID, Constants.VERIFCS_ID, verificationCardSetID, Constants.ERR_DESC, e.getMessage());

			throw new GenerateCredentialDataException(e);
		}

		final CertificateParameters verificationCardIssuerCertificateParameters = getCertificateParameters(
				inputDataPack.getVerificationCardSetProperties(), inputDataPack.getStartDate(), inputDataPack.getEndDate(),
				inputDataPack.getReplacementsHolder(), verificationCardSetCerificateProperties);

		CryptoAPIX509Certificate verificationCardSetIssuerCert;
		try {
			verificationCardSetIssuerCert = createX509Certificate(inputDataPack, verificationCardIssuerCertificateParameters,
					verificationCardSetIssuerKeyPair);

			LOGGER.info(ConfigGeneratorLogEvents.GENVCD_SUCCESS_GENERATING_VERIFICATION_CARD_SET_ISSUER_CERIFICATE.getInfo(), inputDataPack.getEeid(),
					Constants.ADMIN_ID, "cert_cn", verificationCardSetIssuerCert.getSubjectDn().getCommonName(), "cert_sn",
					verificationCardSetIssuerCert.getSerialNumber());

		} catch (Exception e) {

			LOGGER.error(ConfigGeneratorLogEvents.GENVCD_ERROR_GENERATING_VERIFICATION_CARD_SET_ISSUER_CERIFICATE.getInfo(), inputDataPack.getEeid(),
					Constants.ADMIN_ID, "cert_cn", verificationCardIssuerCertificateParameters.getUserSubjectDn().getCommonName(), Constants.ERR_DESC,
					e.getMessage());

			throw new GenerateCredentialDataException(e);
		}

		dataPack.setVerificationCardSetIssuerCert(verificationCardSetIssuerCert);

		String[] choiceCodesEncryptionKeys = choiceCodesEncryptionKeyAsConcatenatedString.split(SEMICOLON);
		ElGamalPublicKey combinedChoiceCodesEncryptionPublicKey;
		ElGamalPublicKey[] nonCombinedChoiceCodesEncryptionPublicKeys = new ElGamalPublicKey[choiceCodesEncryptionKeys.length];

		try {

			final ObjectMapper mapper = new ObjectMapper();
			JsonNode jsonNode = mapper.readTree(choiceCodesEncryptionKeys[0]);
			// Use the toString method because "publicKey" maps to a json object, not a json as string.
			String choiceCodesEncryptionPublicKeyJson = jsonNode.get("publicKey").toString();
			ElGamalPublicKey choiceCodesEncryptionPublicKey = ElGamalPublicKey.fromJson(choiceCodesEncryptionPublicKeyJson);
			nonCombinedChoiceCodesEncryptionPublicKeys[0] = choiceCodesEncryptionPublicKey;

			combinedChoiceCodesEncryptionPublicKey = choiceCodesEncryptionPublicKey;
			for (int i = 1; i < choiceCodesEncryptionKeys.length; i++) {
				jsonNode = mapper.readTree(choiceCodesEncryptionKeys[i]);
				choiceCodesEncryptionPublicKeyJson = jsonNode.get("publicKey").toString();
				choiceCodesEncryptionPublicKey = ElGamalPublicKey.fromJson(choiceCodesEncryptionPublicKeyJson);
				nonCombinedChoiceCodesEncryptionPublicKeys[i] = choiceCodesEncryptionPublicKey;
				combinedChoiceCodesEncryptionPublicKey = combinedChoiceCodesEncryptionPublicKey.multiply(choiceCodesEncryptionPublicKey);
			}

			LOGGER.info(ConfigGeneratorLogEvents.GENVCD_SUCCESS_GENERATING_CHOICES_CODES_KEYPAIR.getInfo(), inputDataPack.getEeid(),
					Constants.ADMIN_ID, Constants.VERIFCS_ID, verificationCardSetID);

		} catch (Exception e) {

			LOGGER.error(ConfigGeneratorLogEvents.GENVCD_ERROR_GENERATING_CHOICES_CODES_KEYPAIR.getInfo(), inputDataPack.getEeid(),
					Constants.ADMIN_ID, Constants.VERIFCS_ID, verificationCardSetID, Constants.ERR_DESC, e.getMessage());

			throw new CreateVotingCardSetException("An error occurred while trying to set the choices codes ElGamal public key", e);
		}

		dataPack.setChoiceCodesEncryptionPublicKey(combinedChoiceCodesEncryptionPublicKey);
		dataPack.setNonCombinedChoiceCodesEncryptionPublicKeys(nonCombinedChoiceCodesEncryptionPublicKeys);

		return dataPack;
	}
}
