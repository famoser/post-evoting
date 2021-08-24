/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.cryptolib.api.elgamal.ElGamalServiceAPI;
import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.extendedkeystore.KeyStoreService;
import ch.post.it.evoting.cryptolib.api.securerandom.CryptoAPIRandomString;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.factory.X509CertificateGenerator;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.cryptolib.elgamal.service.ElGamalService;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.domain.election.helpers.ReplacementsHolder;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans.VerificationCardSetCredentialInputDataPack;

@ExtendWith(MockitoExtension.class)
class VerificationCardSetCredentialDataPackGeneratorTest {

	private static final String ELECTION_EVENT_ID = "electionEventId";
	private static final String VERIFICATION_CARD_SET_ID = "verficationCardSetId";

	private final AsymmetricService asymmetricService = new AsymmetricService();
	private final KeyStoreService storesService = new ExtendedKeyStoreService();
	private final ElGamalServiceAPI elGamalService = new ElGamalService();

	@Mock
	private X509CertificateGenerator certificateGenerator;

	@Mock
	private CryptoAPIRandomString cryptoRandomString;

	@Test
	void generateCredentialHappyPath() throws IOException, GeneralCryptoLibException {

		final VerificationCardSetCredentialDataPackGenerator verificationCardSetCredentialDataPackGenerator = new VerificationCardSetCredentialDataPackGenerator(
				asymmetricService, certificateGenerator, storesService, cryptoRandomString, elGamalService);

		final String choiceCodesKeyString = "{\"publicKey\":{\"zpSubgroup\":{\"g\":\"Ag==\",\"p\":\"Fw==\",\"q\":\"Cw==\"},\"elements\":[\"Ag==\",\"Ag==\"]}}";

		// "publicKey" has to map to a json object and not a json as string.
		final String choiceCodesMessageKeyString = "{\"publicKey\":" + choiceCodesKeyString + "}";

		final VerificationCardSetCredentialInputDataPack inputDataPack = createInputDataPack("elgamalencryptionpublickey");

		final ElGamalPublicKey choiceCodesKey = ElGamalPublicKey.fromJson(choiceCodesKeyString);
		final ElGamalPublicKey expectedCombinedKey = choiceCodesKey.multiply(choiceCodesKey).multiply(choiceCodesKey).multiply(choiceCodesKey);

		final X509DistinguishedName.Builder builder = new X509DistinguishedName.Builder("commonName", "CH");
		final X509DistinguishedName x509DistinguishedName = builder.build();

		final CryptoAPIX509Certificate certificate = mock(CryptoAPIX509Certificate.class);

		when(certificate.getSubjectDn()).thenReturn(x509DistinguishedName);
		when(certificate.getSerialNumber()).thenReturn(BigInteger.TEN);
		when(certificateGenerator.generate(any(), any(), any())).thenReturn(certificate);

		final String choiceCodesEncryptionKeyAsConcatenatedString = StringUtils
				.join(new String[] { choiceCodesMessageKeyString, choiceCodesMessageKeyString, choiceCodesMessageKeyString,
						choiceCodesMessageKeyString }, ';');

		final VerificationCardSetCredentialDataPack result = verificationCardSetCredentialDataPackGenerator
				.generate(inputDataPack, VERIFICATION_CARD_SET_ID, choiceCodesEncryptionKeyAsConcatenatedString, getCertificateParameters());

		assertEquals(result.getChoiceCodesEncryptionPublicKey(), expectedCombinedKey);

		for (ElGamalPublicKey nonCombinedChoiceCodeEncryptionPublicKey : result.getNonCombinedChoiceCodesEncryptionPublicKeys()) {
			assertEquals(nonCombinedChoiceCodeEncryptionPublicKey, choiceCodesKey);
		}
	}

	private Properties getCertificateParameters() throws IOException {

		final Properties props = new Properties();

		try (InputStream input = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream("properties/verificationCardSetX509Certificate.properties")) {
			props.load(input);
		}

		return props;
	}

	private VerificationCardSetCredentialInputDataPack createInputDataPack(String alias) {

		final CredentialProperties credentialProperties = new CredentialProperties();
		credentialProperties.setAlias(new LinkedHashMap<>(1));
		credentialProperties.getAlias().put("publicKey", alias);
		credentialProperties.setCredentialType(CertificateParameters.Type.SIGN);
		credentialProperties.setName("verificationCardSet");
		credentialProperties.setParentName("servicesca");
		credentialProperties.setPropertiesFile("properties/verificationCardSetX509Certificate.properties");

		final VerificationCardSetCredentialInputDataPack inputDataPack = new VerificationCardSetCredentialInputDataPack(credentialProperties);
		inputDataPack.setParentKeyPair(asymmetricService.getKeyPairForSigning());
		inputDataPack.setEeid(ELECTION_EVENT_ID);

		final ReplacementsHolder replacementsHolder = new ReplacementsHolder(ELECTION_EVENT_ID);
		inputDataPack.setReplacementsHolder(replacementsHolder);

		final ZonedDateTime startValidityPeriod = ZonedDateTime.now(ZoneOffset.UTC);
		final ZonedDateTime endValidityPeriod = startValidityPeriod.plusYears(2);
		inputDataPack.setStartDate(startValidityPeriod);
		inputDataPack.setEndDate(endValidityPeriod);

		return inputDataPack;
	}
}
