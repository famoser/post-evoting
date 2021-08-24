/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.spring.batch.writers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.FileSystemResource;

import ch.post.it.evoting.logging.api.domain.Level;
import ch.post.it.evoting.logging.api.domain.LogContent;
import ch.post.it.evoting.logging.api.factory.LoggingFactory;
import ch.post.it.evoting.logging.api.formatter.MessageFormatter;
import ch.post.it.evoting.logging.api.writer.LoggingWriter;
import ch.post.it.evoting.logging.core.factory.LoggingFactoryLog4j;
import ch.post.it.evoting.logging.core.formatter.PipeSeparatedFormatter;
import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.config.exceptions.specific.GenerateVerificationCardCodesException;
import ch.post.it.evoting.sdm.config.logevents.ConfigGeneratorLogEvents;

public class MultiFileDataWriter<T> extends FlatFileItemWriter<T> {

	protected final LoggingWriter loggingWriter;
	private final Path basePath;
	private final int maxNumCredentialsPerFile;
	private int numLinesRead;
	private int fileNumber;

	protected MultiFileDataWriter(final Path basePath, final int maxNumCredentialsPerFile) {

		this.basePath = basePath;
		this.maxNumCredentialsPerFile = maxNumCredentialsPerFile;

		final MessageFormatter formatter = new PipeSeparatedFormatter("OV", "SDM");
		final LoggingFactory loggerFactory = new LoggingFactoryLog4j(formatter);
		this.loggingWriter = loggerFactory.getLogger(MultiFileDataWriter.class);

		validateInput();

		initialize();
	}

	private void changeResourceIfNeeded() {
		numLinesRead++;
		if (numLinesRead > maxNumCredentialsPerFile) {
			numLinesRead = 1;
			fileNumber++;
			close();
			setResource(getNextResource());
			open(new ExecutionContext());
		}
	}

	private void validateInput() {
		if (maxNumCredentialsPerFile < 1) {
			throw new IllegalArgumentException(
					"Expected maximum number of credentials per file to be a positive integer; Found " + maxNumCredentialsPerFile
							+ "; Check Spring configuration properties of config-generator.");
		}
	}

	private FileSystemResource getNextResource() {
		String basePathStr = basePath.toString();
		String basePathPrefixStr = FilenameUtils.removeExtension(basePathStr);

		String path = String.format("%s.%s%s", basePathPrefixStr, fileNumber, Constants.CSV);
		return new FileSystemResource(path);
	}

	private void deletePreExistingOutputFiles() {
		Path baseParentPath = basePath.getParent();

		File baseParentDirectory = new File(baseParentPath.toString());
		if (!baseParentDirectory.exists()) {
			return;
		}

		try (Stream<Path> pathStream = Files.walk(baseParentPath)) {
			pathStream.map(Path::toFile)
					.filter(file -> (file.getName().startsWith(Constants.CONFIG_FILE_NAME_CREDENTIAL_DATA) && file.getName().endsWith(Constants.CSV)))
					.forEach(File::delete);
		} catch (IOException e) {
			String errorMsg = "Error - could not delete all pre-existing credential data files. " + e.getMessage();
			loggingWriter.log(Level.ERROR,
					new LogContent.LogContentBuilder().logEvent(ConfigGeneratorLogEvents.GENCREDAT_ERROR_GENERATING_KEYSTORE).user(Constants.ADMIN_ID)
							.additionalInfo(Constants.ERR_DESC, errorMsg).createLogInfo());
			throw new GenerateVerificationCardCodesException(errorMsg);
		}
	}

	private void initialize() {
		numLinesRead = 0;
		fileNumber = 0;

		setLineAggregator(lineAggregator());
		setTransactional(false);
		setResource(getNextResource());

		// These lines are necessary for the Spring FlatFileItemWriter base
		// class to work properly after resetting the resource (for the case of
		// writing the credential data to multiple files).
		deletePreExistingOutputFiles();
		setAppendAllowed(true);
		setShouldDeleteIfExists(false);
	}

	protected String getLine(T item) {
		return item.toString();
	}

	private LineAggregator<T> lineAggregator() {
		return item -> {
			changeResourceIfNeeded();
			return getLine(item);
		};
	}
}
