/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.remote.client;

import static java.util.Arrays.copyOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

/**
 * Tests of {@link InputStreamTypedOutput}.
 */
public class InputStreamTypedOutputTest {

	private static final String MIME_TYPE = "mimeType/type";

	private static final int LENGTH = 2;

	private static final byte[] BYTES = { 0, 1, 2 };

	private static final String FILE_NAME = "fileName";

	@Test
	public void testFileName() {
		InputStream in = new ByteArrayInputStream(BYTES);
		InputStreamTypedOutput output = new InputStreamTypedOutput(MIME_TYPE, in, LENGTH, FILE_NAME);
		assertEquals(FILE_NAME, output.fileName());
	}

	@Test
	public void testLength() {
		InputStream in = new ByteArrayInputStream(BYTES);
		InputStreamTypedOutput output = new InputStreamTypedOutput(MIME_TYPE, in, LENGTH);
		assertEquals(LENGTH, output.contentLength());
	}

	@Test
	public void testMimeType() {
		InputStream in = new ByteArrayInputStream(BYTES);
		InputStreamTypedOutput output = new InputStreamTypedOutput(MIME_TYPE, in, LENGTH);
		assertEquals(MIME_TYPE, output.contentType().toString());
	}

	@Test
	public void testWriteTo() throws IOException {
		InputStream in = new ByteArrayInputStream(BYTES);
		InputStreamTypedOutput output = new InputStreamTypedOutput(MIME_TYPE, in, LENGTH);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			output.writeTo(out);
		} finally {
			out.close();
		}
		assertArrayEquals(copyOf(BYTES, LENGTH), out.toByteArray());
	}

	@Test
	public void testWriteToAll() throws IOException {
		InputStream in = new ByteArrayInputStream(BYTES);
		InputStreamTypedOutput output = new InputStreamTypedOutput(MIME_TYPE, in, -1);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			output.writeTo(out);
		} finally {
			out.close();
		}
		assertArrayEquals(BYTES, out.toByteArray());
	}

	@Test(expected = IOException.class)
	public void testWriteToIOException() throws IOException {
		InputStream in = mock(InputStream.class);
		when(in.read()).thenThrow(new IOException("test"));
		when(in.read(any(byte[].class))).thenThrow(new IOException("test"));
		when(in.read(any(byte[].class), anyInt(), anyInt())).thenThrow(new IOException("test"));
		InputStreamTypedOutput output = new InputStreamTypedOutput(MIME_TYPE, in, -1);
		output.writeTo(new ByteArrayOutputStream());
	}
}
