/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jdk.nashorn.internal.runtime.QuotedStringTokenizer;

@Service
public class SequentialExecutorImpl implements SequentialExecutor {

	private static final Logger LOGGER = LoggerFactory.getLogger(SequentialExecutorImpl.class);

	public void execute(List<String> commands, Parameters parameters, ExecutionListener listener) {

		for (String command : commands) {
			String mockCommand = command;
			try {
				// Replace the parameters.
				String[] partialCommands = replaceParameters(command, parameters);
				String fullCommand = partialCommands[0];
				mockCommand = partialCommands[1];

				// Tokenize the command while ignoring quoted parameters. Needed for example for the PRIVATE_KEY, which contains spaces and carriage
				// returns.
				final QuotedStringTokenizer stringTokenizer = new QuotedStringTokenizer(fullCommand, " \t\n\r\f");
				final String[] cmdarray = new String[stringTokenizer.countTokens()];
				for (int i = 0; stringTokenizer.hasMoreTokens(); i++) {
					cmdarray[i] = stringTokenizer.nextToken();
				}

				// Remove unwanted environment variable that will be inherited by the child process.
				final String[] envp = buildCommandEnv();

				// Execute the command.
				Process proc = Runtime.getRuntime().exec(cmdarray, envp);

				InputStream isIn = proc.getInputStream();
				consumeProcessStream(isIn, listener);

				StringBuilder stringBuilder = new StringBuilder();
				InputStream isError = proc.getErrorStream();
				consumeProcessStream(isError, stringBuilder);

				final int exitValue = proc.waitFor();
				detectError(exitValue, stringBuilder, mockCommand, listener);

			} catch (IllegalArgumentException e) {
				listener.onError(ResultCode.UNEXPECTED_ERROR.value());
				listener.onMessage(e.getMessage());
				LOGGER.error("Error '{}' when executing command: {} : {}", ResultCode.UNEXPECTED_ERROR.value(), mockCommand, e);
			} catch (RuntimeException | IOException e) {
				listener.onError(ResultCode.GENERAL_ERROR.value());
				listener.onMessage(e.getMessage());
				LOGGER.error("Error '{}' when executing command: {} : {}", ResultCode.GENERAL_ERROR.value(), mockCommand, e);
			} catch (InterruptedException e) {
				LOGGER.warn("Got interrupted when executing command: {}", mockCommand);
				Thread.currentThread().interrupt();
			}
		}
	}

	private String[] buildCommandEnv() {
		// Get the current environment variables into a modifiable map.
		final Map<String, String> env = System.getenv();
		final Map<String, String> modifiable = new HashMap<>(env);

		// Some variables logging is polluting the error stream of the child process.
		// JAVA_TOOL_OPTIONS must not be inherited otherwise it will be tried to attach two debuggers to the same jvm on the same port.
		modifiable.remove("JAVA_TOOL_OPTIONS");

		return modifiable.entrySet().stream().map(Object::toString).toArray(String[]::new);
	}

	private void consumeProcessStream(final InputStream is, final StringBuilder sb) throws IOException {
		String error = IOUtils.toString(is, StandardCharsets.UTF_8);
		sb.append(error);
	}

	private void consumeProcessStream(final InputStream is, final ExecutionListener listener) {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			LOGGER.debug("Plugin Output ----->>");
			while ((line = br.readLine()) != null) {
				if (line.contains("ERROR")) {
					LOGGER.warn("Plugin execution may have generated an error --->> {}", line);
					listener.onProgress(line);
					listener.onError(-1);
				} else {
					LOGGER.debug(line);
					listener.onProgress(line);
				}
			}
			LOGGER.debug("<<----- Plugin Output");
		} catch (IOException e) {
			LOGGER.warn("Failed to read plugin execution output", e);
		}
	}

	private String[] replaceParameters(String command, Parameters parameters) {
		String partialCommand = command;
		String replacedCommand = command;

		for (KeyParameter key : KeyParameter.values()) {
			if (replacedCommand.contains(key.toString())) {
				String value = parameters.getParam(key.name());
				if (value == null || value.isEmpty()) {
					throw new IllegalArgumentException("Parameter #" + key.name() + "# is null or empty");
				} else {
					replacedCommand = replacedCommand.replaceAll("#" + key + "#", value);
					if (key == KeyParameter.PRIVATE_KEY) {
						partialCommand = partialCommand.replaceAll("#" + key + "#", "PRIVATE_KEY");
					} else {
						partialCommand = partialCommand.replaceAll("#" + key + "#", value);
					}
				}
			}
		}

		return new String[] { replacedCommand, partialCommand };
	}

	/**
	 * In our plugin the error code is always '0' even if it fails internally (unless it throws an uncaught exception). Therefore, we have to infer
	 * the real exit value from the process outputstream and return it in the 'listener' object
	 */
	private void detectError(int exitValue, StringBuilder stringBuilder, String mockCommand, ExecutionListener listener) {

		String errorOutput = stringBuilder.toString().trim();

		if (exitValue != 0 || errorOutput.length() > 0) {
			if (exitValue != 0) {
				listener.onError(exitValue);
			} else {
				try {
					int actualError = Integer.parseInt(errorOutput);
					listener.onError(actualError);
				} catch (NumberFormatException e) {
					listener.onError(ResultCode.GENERAL_ERROR.value());
				}
			}
			listener.onMessage("Failed to execute: " + mockCommand);
			LOGGER.error("ExitCode '{}' when executing command {} : {}", exitValue, mockCommand, errorOutput);
		}
	}
}
