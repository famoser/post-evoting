/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.security.cert.CertificateException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.api.primitives.PrimitivesServiceAPI;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.sdm.config.commands.voters.JobExecutionObjectContext;
import ch.post.it.evoting.sdm.config.commands.voters.VotersHolderInitializer;
import ch.post.it.evoting.sdm.config.commands.voters.VotersParametersHolder;
import ch.post.it.evoting.sdm.config.commands.voters.VotersSerializationDestProvider;
import ch.post.it.evoting.sdm.config.commands.voters.datapacks.generators.VerificationCardSetCredentialDataPackGenerator;

@Configuration
public class TestConfigServices {

	@Bean
	JobExecutionObjectContext executionObjectContext() {
		return mock(JobExecutionObjectContext.class);
	}

	@Bean
	VerificationCardSetCredentialDataPackGenerator verificationCardSetCredentialDataPackGenerator() {
		return mock(VerificationCardSetCredentialDataPackGenerator.class);
	}

	@Bean
	VotersHolderInitializer votersHolderInitializer(VotersParametersHolder holder) throws CertificateException, GeneralCryptoLibException {
		final VotersHolderInitializer initializer = mock(VotersHolderInitializer.class);
		when(initializer.init(any(), any(InputStream.class))).thenReturn(holder);
		return initializer;
	}

	@Bean
	public PrimitivesServiceAPI primitivesServiceAPI() {
		return new PrimitivesService();
	}

	@Bean
	VotersSerializationDestProvider serializationDestProvider() {
		return mock(VotersSerializationDestProvider.class);
	}

}
