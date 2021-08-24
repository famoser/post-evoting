/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.config.commands.primes;

import static ch.post.it.evoting.config.CommandParameter.OUT;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.config.CommandParameter;
import ch.post.it.evoting.config.Parameters;

@Service
public final class PrimesParametersAdapter {

	public PrimesParametersHolder adapt(final Parameters receivedParameters) {

		final String encryptionParamsPath = (String) receivedParameters.getParam(CommandParameter.ENCRYPTION_PARAMS.getParameterName());
		final String p12Path = (String) receivedParameters.getParam(CommandParameter.P12_PATH.getParameterName());
		final String trustedCAPath = (String) receivedParameters.getParam(CommandParameter.TRUSTED_CA_PATH.getParameterName());

		String outputPath = null;
		if (receivedParameters.contains(OUT.getParameterName())) {
			outputPath = (String) receivedParameters.getParam(OUT.getParameterName());
		}

		return new PrimesParametersHolder(p12Path, encryptionParamsPath, trustedCAPath, outputPath);
	}

}
