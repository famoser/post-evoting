/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.electionevent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.post.it.evoting.sdm.readers.ConfigurationInputReader;

@ExtendWith(MockitoExtension.class)
class CreateElectionEventHolderInitializerTest {

	@Mock
	ConfigurationInputReader reader;

	@InjectMocks
	CreateElectionEventHolderInitializer sut;

	@Test
	void setKeysConfigurationFromFile() throws Exception {

		// given
		CreateElectionEventParametersHolder holder = mock(CreateElectionEventParametersHolder.class);
		File configFile = mock(File.class);

		// when
		sut.init(holder, configFile);

		// then
		verify(holder, times(1)).setConfigurationInput(any());
	}

	@Test
	void setKeysConfigurationFromStream() throws Exception {

		// given
		CreateElectionEventParametersHolder holder = mock(CreateElectionEventParametersHolder.class);
		InputStream configInputStream = mock(InputStream.class);

		// when
		sut.init(holder, configInputStream);

		// then
		verify(holder, times(1)).setConfigurationInput(any());
	}
}
