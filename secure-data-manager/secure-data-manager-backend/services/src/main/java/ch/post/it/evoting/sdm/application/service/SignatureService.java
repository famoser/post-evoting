/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.service;

import static java.nio.file.Files.newInputStream;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.infrastructure.CompositeInputStream;

@Service
public class SignatureService {

	public static final int MAX_DIRECTORY_LEVELS_TO_VISIT = 1;
	private static final byte[] LINE_SEPARATOR = "\n".getBytes(StandardCharsets.UTF_8);

	/**
	 * Signs a csv file returning the content of the signature
	 *
	 * @param privateKey
	 * @param csvFile
	 * @return
	 * @throws IOException
	 * @throws GeneralCryptoLibException
	 */
	public String signCSV(final PrivateKey privateKey, final File csvFile) throws IOException, GeneralCryptoLibException {
		try (FileInputStream csvFileIn = new FileInputStream(csvFile)) {

			AsymmetricService asymmetricService = new AsymmetricService();
			final byte[] signature = asymmetricService.sign(privateKey, csvFileIn);

			return Base64.getEncoder().encodeToString(signature);
		}
	}

	/**
	 * Creates a file containing the signature in base64 of the CSV, appending the suffix .sign
	 *
	 * @param signatureB64
	 * @param csvPath
	 * @throws IOException
	 */
	public void saveCSVSignature(final String signatureB64, final Path csvPath) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath.toString() + Constants.SIGN))) {
			writer.write(signatureB64);
			writer.flush();
		}
	}

	/**
	 * Receives a root path and deletes all the signature files within the folder
	 *
	 * @param rootPath
	 * @throws IOException
	 */
	public void deleteSignaturesFromCSVs(Path rootPath) throws IOException {
		try (Stream<Path> pathStream = Files.walk(rootPath, MAX_DIRECTORY_LEVELS_TO_VISIT)) {
			List<Path> filePaths = pathStream.collect(Collectors.toList());

			for (Path filePath : filePaths) {
				Path fileName = filePath.getFileName();
				if (fileName == null) {
					throw new IOException("Invalid file name");
				}
				String name = fileName.toString();
				if (name.endsWith(Constants.SIGN)) {
					Files.deleteIfExists(filePath);
				}

			}
		}
	}

	// Ignore 'Resources should be closed' Sonar's rule since the closing is handled by closing the returned CompositeInputStream.
	@SuppressWarnings("squid:S2095")
	public InputStream newCSVAndSignatureInputStream(final Path csvFile) throws IOException {
		Path signatureFile = csvFile.resolveSibling(csvFile.getFileName() + Constants.SIGN);

		InputStream separator = null;
		InputStream csv = null;

		try {
			csv = newInputStream(csvFile);

			separator = new ByteArrayInputStream(LINE_SEPARATOR);
			InputStream signature = newInputStream(signatureFile);

			return new CompositeInputStream(csv, separator, signature);
		} catch (IOException | RuntimeException e) {
			try {
				if (csv != null) {
					csv.close();
				}
				if (separator != null) {
					separator.close();
				}
			} catch (IOException e1) {
				e.addSuppressed(e1);
			}
			throw e;
		}
	}
}
