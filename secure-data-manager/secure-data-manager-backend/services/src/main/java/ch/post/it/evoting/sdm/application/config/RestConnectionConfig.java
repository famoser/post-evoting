/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.application.config;

public class RestConnectionConfig {

	private final String readTimeOut;

	private final String writeTimeOut;

	private final String connectionTimeOut;

	public RestConnectionConfig(String readTimeOut, String writeTimeOut, String connectionTimeOut) {
		this.readTimeOut = readTimeOut;
		this.writeTimeOut = writeTimeOut;
		this.connectionTimeOut = connectionTimeOut;

		System.setProperty("read.time.out", readTimeOut);
		System.setProperty("write.time.out", writeTimeOut);
		System.setProperty("connection.time.out", connectionTimeOut);

	}

	public String getReadTimeOut() {
		return readTimeOut;
	}

	public String getConnectionTimeOut() {
		return connectionTimeOut;
	}

	public String getWriteTimeOut() {
		return writeTimeOut;
	}
}
