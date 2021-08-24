/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.ballotbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.cryptolib.certificates.bean.CredentialProperties;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.domain.common.ConfigurationInput;
import ch.post.it.evoting.sdm.readers.ConfigurationInputReader;
import ch.post.it.evoting.sdm.utils.EncryptionParametersLoader;
import ch.post.it.evoting.sdm.utils.KeyStoreReader;
import ch.post.it.evoting.sdm.utils.X509CertificateLoader;

@ExtendWith(MockitoExtension.class)
class BallotBoxHolderInitializerTest {

	@TempDir
	static Path tempDir;

	@Mock
	ConfigurationInputReader configurationInputReader;
	@Mock
	X509CertificateLoader certificateLoader;
	@Mock
	EncryptionParametersLoader encryptionParametersLoader;
	@Mock
	KeyStoreReader keyStoreReader;
	@InjectMocks
	BallotBoxHolderInitializer sut;

	@Test
	void setKeysConfigurationFromFile() throws Exception {

		// given
		BallotBoxParametersHolder holder = mock(BallotBoxParametersHolder.class);
		when(holder.getOutputPath()).thenReturn(tempDir);

		ConfigurationInput mockInput = mock(ConfigurationInput.class);
		CredentialProperties mockCredentialProperties = mock(CredentialProperties.class);
		Map<String, CredentialProperties> mockProperties = mock(Map.class);
		when(mockProperties.get(Constants.CONFIGURATION_SERVICES_CA_JSON_TAG)).thenReturn(mockCredentialProperties);
		when(mockProperties.get(Constants.CONFIGURATION_ELECTION_CA_JSON_TAG)).thenReturn(mockCredentialProperties);
		when(mockInput.getConfigProperties()).thenReturn(mockProperties);
		when(mockInput.getBallotBox()).thenReturn(mockCredentialProperties);

		when(configurationInputReader.fromStreamToJava(any())).thenReturn(mockInput);
		final CryptoAPIX509Certificate mockCertificate = mock(CryptoAPIX509Certificate.class);
		when(certificateLoader.load(any(Path.class))).thenReturn(mockCertificate);
		final EncryptionParameters mockEncryptionParams = mock(EncryptionParameters.class);
		when(encryptionParametersLoader.load(any())).thenReturn(mockEncryptionParams);
		final PrivateKey mockPrivateKey = mock(PrivateKey.class);
		when(keyStoreReader.getPrivateKey(any(), any(), any(), any())).thenReturn(mockPrivateKey);

		// when
		sut.init(holder, mock(InputStream.class));

		// then
		int numberOfInvocations = 1;
		verify(holder, atLeastOnce()).getOutputPath(); // avoid error of
		// no more
		// interactions
		verify(holder, times(numberOfInvocations)).setServicesCAPrivateKey(mockPrivateKey);
		verify(holder, times(numberOfInvocations)).setBallotBoxCredentialProperties(mockCredentialProperties);
		verify(holder, times(numberOfInvocations)).setEncryptionParameters(mockEncryptionParams);
		verify(holder, times(numberOfInvocations)).setServicesCACert(any());
		verify(holder, times(numberOfInvocations)).setElectionCACert(mockCertificate);
		verifyNoMoreInteractions(holder);
	}

	@Test
	void setKeysConfigurationFromStream() throws Exception {

		// given
		BallotBoxParametersHolder holder = mock(BallotBoxParametersHolder.class);
		when(holder.getOutputPath()).thenReturn(tempDir);

		InputStream configInputStream = mock(InputStream.class);
		ConfigurationInput mockInput = mock(ConfigurationInput.class);
		CredentialProperties mockCredentialProperties = mock(CredentialProperties.class);
		Map<String, CredentialProperties> mockProperties = mock(Map.class);
		when(mockProperties.get(Constants.CONFIGURATION_SERVICES_CA_JSON_TAG)).thenReturn(mockCredentialProperties);
		when(mockProperties.get(Constants.CONFIGURATION_ELECTION_CA_JSON_TAG)).thenReturn(mockCredentialProperties);
		when(mockInput.getConfigProperties()).thenReturn(mockProperties);
		when(mockInput.getBallotBox()).thenReturn(mockCredentialProperties);

		when(configurationInputReader.fromStreamToJava(any())).thenReturn(mockInput);
		final CryptoAPIX509Certificate mockCertificate = mock(CryptoAPIX509Certificate.class);
		when(certificateLoader.load(any(Path.class))).thenReturn(mockCertificate);
		final EncryptionParameters mockEncryptionParams = mock(EncryptionParameters.class);
		when(encryptionParametersLoader.load(any())).thenReturn(mockEncryptionParams);
		final PrivateKey mockPrivateKey = mock(PrivateKey.class);
		when(keyStoreReader.getPrivateKey(any(), any(), any(), any())).thenReturn(mockPrivateKey);

		// when
		sut.init(holder, configInputStream);

		// then
		int numberOfInvocations = 1;
		verify(holder, atLeastOnce()).getOutputPath(); // avoid error of
		// no more
		// interactions
		verify(holder, times(numberOfInvocations)).setServicesCAPrivateKey(mockPrivateKey);
		verify(holder, times(numberOfInvocations)).setBallotBoxCredentialProperties(mockCredentialProperties);
		verify(holder, times(numberOfInvocations)).setEncryptionParameters(mockEncryptionParams);
		verify(holder, times(numberOfInvocations)).setServicesCACert(any());
		verify(holder, times(numberOfInvocations)).setElectionCACert(mockCertificate);
		verifyNoMoreInteractions(holder);
	}
}
