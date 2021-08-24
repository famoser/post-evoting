/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;

@Service
public class ExtendedAuthenticationService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedAuthenticationService.class);
	@Autowired
	private PathResolver pathResolver;
	@Autowired
	private SignatureService signatureService;

	/**
	 * Signs the extended authentication files
	 *
	 * @param electionEventId
	 * @param votingCardSetId
	 * @param privateKey
	 * @return
	 * @throws IOException
	 */
	public boolean signExtendedAuthentication(final String electionEventId, final String votingCardSetId, final PrivateKey privateKey)
			throws IOException {

		Path extendAuthPath = pathResolver.resolve(Constants.CONFIG_FILES_BASE_DIR).resolve(electionEventId).resolve(Constants.CONFIG_DIR_NAME_ONLINE)
				.resolve(Constants.CONFIG_FILE_EXTENDED_AUTHENTICATION_DATA).resolve(votingCardSetId);

		boolean result = evaluateSignatureProccess(() -> {
			long numberFailed;
			try {
				numberFailed = Files.walk(extendAuthPath, 1).filter(csvPath -> {
					boolean failed = Boolean.FALSE;
					try {
						String name = csvPath.getFileName().toString();
						if (name.startsWith(Constants.CONFIG_FILE_EXTENDED_AUTHENTICATION_DATA) && name.endsWith(Constants.CSV)) {

							LOGGER.info("{} {}", Constants.SIGNING_FILE, name);
							String signatureB64 = signatureService.signCSV(privateKey, csvPath.toFile());
							LOGGER.info(Constants.SAVING_SIGNATURE);
							signatureService.saveCSVSignature(signatureB64, csvPath);
						}
					} catch (IOException | GeneralCryptoLibException e) {
						LOGGER.info("File signing failed.", e);
						failed = true;
					}
					return failed;
				}).count();
			} catch (IOException e) {
				LOGGER.info("File signing failed.", e);
				return Boolean.FALSE;
			}
			return numberFailed == 0;
		});

		if (!result) {
			signatureService.deleteSignaturesFromCSVs(extendAuthPath);
		}
		return result;
	}

	private boolean evaluateSignatureProccess(final Supplier<Boolean> arg) {
		return arg.get();
	}

}
