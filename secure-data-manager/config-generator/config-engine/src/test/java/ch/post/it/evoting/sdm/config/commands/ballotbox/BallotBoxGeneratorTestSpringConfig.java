/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.sdm.config.commands.ballotbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateData;
import ch.post.it.evoting.cryptolib.certificates.bean.ValidityDates;
import ch.post.it.evoting.cryptolib.certificates.bean.X509DistinguishedName;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.certificates.service.CertificatesService;
import ch.post.it.evoting.cryptolib.extendedkeystore.cryptoapi.CryptoAPIExtendedKeyStore;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.core.factory.LoggingFactoryLog4j;
import ch.post.it.evoting.logging.core.formatter.PipeSeparatedFormatter;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.beans.ElectionCredentialDataPack;
import ch.post.it.evoting.sdm.config.commands.electionevent.datapacks.generators.ElectionCredentialDataPackGenerator;

@Configuration
public class BallotBoxGeneratorTestSpringConfig {

	@Bean
	public BallotBoxGenerator ballotBoxGenerator(final ElectionCredentialDataPackGenerator electionCredentialDataPackGenerator) {
		return new BallotBoxGenerator(electionCredentialDataPackGenerator);
	}

	@Bean
	public ElectionCredentialDataPackGenerator electionCredentialDataPackGenerator() throws GeneralCryptoLibException {

		ElectionCredentialDataPackGenerator electionCredentialDataPackGeneratorMock = mock(ElectionCredentialDataPackGenerator.class);
		ElectionCredentialDataPack dataPackMock = mock(ElectionCredentialDataPack.class);
		CryptoAPIExtendedKeyStore ballotBoxKeystore = mock(CryptoAPIExtendedKeyStore.class);

		when(ballotBoxKeystore.toJSON("keystorePassword".toCharArray())).thenReturn("mockedKeystoreString");

		CryptoAPIX509Certificate certificate = createCryptoAPIX509CertificateTest();
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime end = now.plusYears(1);

		when(dataPackMock.getCertificate()).thenReturn(certificate);
		when(dataPackMock.getStartDate()).thenReturn(now);
		when(dataPackMock.getEndDate()).thenReturn(end);
		when(dataPackMock.getPassword()).thenReturn("keystorePassword".toCharArray());
		when(dataPackMock.getEncryptedPassword()).thenReturn("encryptedKeystorePassword");
		when(dataPackMock.getKeyStore()).thenReturn(ballotBoxKeystore);

		when(electionCredentialDataPackGeneratorMock.generate(any(), any(), any(), any(), any(), any())).thenReturn(dataPackMock);

		return electionCredentialDataPackGeneratorMock;
	}

	@Bean
	public LoggingFactory loggingFactory() {
		PipeSeparatedFormatter pipeSeparatedFormatterMock = mock(PipeSeparatedFormatter.class);

		when(pipeSeparatedFormatterMock.buildMessage(any())).thenReturn("splunkFormatterMock buildMessage call");

		return new LoggingFactoryLog4j(pipeSeparatedFormatterMock);
	}

	private CryptoAPIX509Certificate createCryptoAPIX509CertificateTest() throws GeneralCryptoLibException {
		KeyPair keyPair = new AsymmetricService().getKeyPairForSigning();

		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
		ZonedDateTime end = now.plusYears(1);
		ValidityDates validityDates = new ValidityDates(Date.from(now.toInstant()), Date.from(end.toInstant()));

		CertificateData certificateData = new CertificateData();
		certificateData.setSubjectPublicKey(keyPair.getPublic());
		certificateData.setValidityDates(validityDates);

		X509DistinguishedName distinguishedName = new X509DistinguishedName.Builder("commonName", "CH").build();
		certificateData.setSubjectDn(distinguishedName);
		certificateData.setIssuerDn(distinguishedName);

		return new CertificatesService().createSignX509Certificate(certificateData, keyPair.getPrivate());
	}
}
