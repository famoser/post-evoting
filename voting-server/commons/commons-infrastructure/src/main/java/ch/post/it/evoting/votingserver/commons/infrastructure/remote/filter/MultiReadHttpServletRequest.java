/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.remote.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.io.IOUtils;

/**
 * This class purpose is extending HttpServletRequestWrapper, consuming the request InputStream and essentially caching the bytes. Now the request
 * body can be read more than once by wrapping the original request before passing it through the filter chain.
 */
public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {
	private ByteArrayOutputStream cachedBytes;

	public MultiReadHttpServletRequest(HttpServletRequest request) {
		super(request);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (cachedBytes == null) {
			cacheInputStream();
		}

		return new CachedServletInputStream();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream()));
	}

	private void cacheInputStream() throws IOException {
		/*
		 * Cache the inputStream in order to read it multiple times. For convenience, I use
		 * apache.commons IOUtils
		 */
		cachedBytes = new ByteArrayOutputStream();
		IOUtils.copy(super.getInputStream(), cachedBytes);
	}

	/* An inputStream which reads the cached request body */
	public class CachedServletInputStream extends ServletInputStream {
		private final ByteArrayInputStream input;

		public CachedServletInputStream() {
			/* create a new input stream from the cached request body */
			input = new ByteArrayInputStream(cachedBytes.toByteArray());
		}

		@Override
		public int read() throws IOException {
			return input.read();
		}
	}
}
