/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertificateException;

import org.bouncycastle.cms.CMSException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptoprimitives.SecurityLevel;
import ch.post.it.evoting.cryptoprimitives.SecurityLevelConfig;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.domain.mixnet.ObjectMapperMixnetConfig;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.service.ElectionEventDataGeneratorService;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;
import ch.post.it.evoting.sdm.utils.EncryptionParametersLoader;

/**
 * JUnit for the class {@link ElectionEventService}.
 */
@ExtendWith(MockitoExtension.class)
class ElectionEventServiceTest {

	@Spy
	private final ObjectMapper mapper = ObjectMapperMixnetConfig.getNewInstance();
	@InjectMocks
	private ElectionEventService electionEventService;
	@Mock
	private ElectionEventDataGeneratorService electionEventDataGeneratorServiceMock;
	@Mock
	private ConfigurationEntityStatusService configurationEntityStatusService;
	@Mock
	private PathResolver pathResolver;
	@Mock
	private EncryptionParametersLoader encryptionParametersLoaderMock;

	@Test
	void createElectionEventGenerateFails() throws IOException, CertificateException, CMSException, GeneralCryptoLibException {
		try (MockedStatic<SecurityLevelConfig> mockedSecurityLevel = mockStatic(SecurityLevelConfig.class)) {
			mockedSecurityLevel.when(SecurityLevelConfig::getSystemSecurityLevel).thenReturn(SecurityLevel.TESTING_ONLY);
			DataGeneratorResponse resultElectionEventGeneration = new DataGeneratorResponse();
			resultElectionEventGeneration.setSuccessful(false);
			when(electionEventDataGeneratorServiceMock.generate(anyString())).thenReturn(resultElectionEventGeneration);
			when(pathResolver.resolve(eq(Constants.CONFIG_FILES_BASE_DIR), anyString())).thenReturn(Paths.get("test"));
			doNothing().when(mapper).writeValue((File) any(), any());

			final EncryptionParameters encryptionParameters = new EncryptionParameters("11", "5", "3");
			when(encryptionParametersLoaderMock.load(any())).thenReturn(encryptionParameters);

			assertFalse(electionEventService.create("").isSuccessful());
		}
	}

	@Test
	void create(
			@TempDir
			final Path tempDir) throws IOException, CertificateException, CMSException, GeneralCryptoLibException {

		try (MockedStatic<SecurityLevelConfig> mockedSecurityLevel = mockStatic(SecurityLevelConfig.class)) {
			mockedSecurityLevel.when(SecurityLevelConfig::getSystemSecurityLevel).thenReturn(SecurityLevel.TESTING_ONLY);
			final DataGeneratorResponse resultElectionEventGeneration = new DataGeneratorResponse();
			resultElectionEventGeneration.setSuccessful(true);
			when(electionEventDataGeneratorServiceMock.generate(anyString())).thenReturn(resultElectionEventGeneration);
			when(configurationEntityStatusService.update(anyString(), anyString(), any())).thenReturn("");

			when(pathResolver.resolve(eq(Constants.CONFIG_FILES_BASE_DIR), anyString())).thenReturn(tempDir);
			final Path outputPath = tempDir.resolve(Constants.CONFIG_DIR_NAME_OFFLINE);
			Files.createDirectories(outputPath);

			final EncryptionParameters encryptionParameters = new EncryptionParameters("11", "5", "3");
			when(encryptionParametersLoaderMock.load(any())).thenReturn(encryptionParameters);

			assertTrue(electionEventService.create("").isSuccessful());
			assertTrue(Files.exists(outputPath.resolve(Constants.SETUP_SECRET_KEY_FILE_NAME)));
		}
	}
}
