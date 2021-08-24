/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.remote.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.security.PublicKey;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.stores.keystore.configuration.NodeIdentifier;
import ch.post.it.evoting.votingserver.commons.infrastructure.exception.OvCommonsInfrastructureException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RestClientInterceptor;
import ch.post.it.evoting.votingserver.commons.sign.beans.SignedRequestContent;
import ch.post.it.evoting.votingserver.commons.verify.RequestVerifier;

/**
 * Filter to check Signature inside every request.
 */
public final class SignedRequestFilter implements Filter {

	public static final String URL_REGEX_INIT_PARAM_NAME = "urlRegularExpression";
	private static final Logger LOGGER = LoggerFactory.getLogger(SignedRequestFilter.class);
	private SignedRequestKeyManager signedRequestKeyManager;
	private String urlRegularExpression;

	/**
	 * @see Filter#init(FilterConfig)
	 */
	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {

		LOGGER.info("Initializing filter - name: " + filterConfig.getFilterName() + ", path: " + filterConfig.getServletContext().getContextPath()
				+ ", container: " + filterConfig.getServletContext().getServerInfo());

		this.urlRegularExpression = filterConfig.getInitParameter(URL_REGEX_INIT_PARAM_NAME);

		try {
			signedRequestKeyManager = SignedRequestKeyManager.getInstance();

		} catch (OvCommonsInfrastructureException e) {
			throw new ServletException("Something went wrong when creating the filter: " + e, e);
		}
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain filterChain)
			throws IOException, ServletException {

		LOGGER.debug("Filtering request to verify Originator signature...");

		String uri;

		if (request instanceof HttpServletRequest) {
			uri = ((HttpServletRequest) request).getRequestURI();
			LOGGER.debug("URL: " + uri);
		} else {
			LOGGER.info("The request is not a HttpServletRequest. Aborting");
			return;
		}

		HttpServletResponse httpServletResponse;
		if (response instanceof HttpServletResponse) {
			httpServletResponse = (HttpServletResponse) response;
		} else {
			LOGGER.info("The response is not the expected type. Expected a HttpServletResponse. Aborting.");
			return;
		}

		String decodedUri = URLDecoder.decode(uri, "UTF-8");

		if (uriNeedsToBeFiltered(decodedUri)) {

			// Convert original request...
			/* wrap the request in order to read the inputStream multiple times */
			MultiReadHttpServletRequest multiReadRequest = new MultiReadHttpServletRequest((HttpServletRequest) request);

			// Check 'Originator' header
			NodeIdentifier nodeIdentifier;
			try {
				String originatorStr = multiReadRequest.getHeader(RestClientInterceptor.HEADER_ORIGINATOR);
				nodeIdentifier = NodeIdentifier.valueOf(originatorStr);

			} catch (IllegalArgumentException | NullPointerException e) {
				LOGGER.warn("The request has not identified Originator or it is invalid. Aborting", e);
				httpServletResponse.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
				return;
			}

			if (signatureIsValid(multiReadRequest, nodeIdentifier)) {
				LOGGER.info("Request signature in headers is VALID. Processing...");
				// PROCEED WITH THE REQUEST
				filterChain.doFilter(multiReadRequest, response);
				return;

			} else {
				LOGGER.warn("Request signature in headers is INVALID. Aborting...");
				httpServletResponse.setStatus(Response.Status.UNAUTHORIZED.getStatusCode());
				return;
			}
		} else {
			LOGGER.debug("URL not need to be filtered. Processing...");
			// PROCEED WITH THE REQUEST
			filterChain.doFilter(request, response);
			return;
		}
	}

	private boolean signatureIsValid(final MultiReadHttpServletRequest multiReadHttpServletRequest, final NodeIdentifier nodeIdentifier) {

		LOGGER.debug("Retrieving Originator public key... ");

		PublicKey publicKey;
		try {
			publicKey = getPublicKey(nodeIdentifier);
		} catch (OvCommonsInfrastructureException e) {
			LOGGER.warn("Error obtaining the public key.", e);
			return false;
		}

		LOGGER.debug("Validating signature... ");

		String signatureStr = multiReadHttpServletRequest.getHeader(RestClientInterceptor.HEADER_SIGNATURE);

		if (signatureStr == null) {
			LOGGER.warn("The Signature is empty in this request.");
			return false;
		}

		byte[] signature;
		try {
			signature = Base64.getDecoder().decode(signatureStr);
		} catch (IllegalArgumentException e) {
			LOGGER.warn("The Signature is malformed.", e);
			return false;
		}

		RequestVerifier requestVerifier = new RequestVerifier();

		String body = getRequestBodyToString(multiReadHttpServletRequest);

		SignedRequestContent signedRequestContent = new SignedRequestContent(multiReadHttpServletRequest.getRequestURI(),
				multiReadHttpServletRequest.getMethod(), body, nodeIdentifier.name());

		boolean result = false;

		try {
			result = requestVerifier.verifySignature(signedRequestContent, signature, publicKey);

		} catch (GeneralCryptoLibException e) {
			LOGGER.error("Error validating signature: " + e.getMessage(), e);
			return false;
		}
		return result;
	}

	@Override
	public void destroy() {
		LOGGER.info("destroying SignedRequestFilter");
	}

	private PublicKey getPublicKey(final NodeIdentifier nodeIdentifier) throws OvCommonsInfrastructureException {

		return signedRequestKeyManager.getPublicKeyFromOriginator(nodeIdentifier);
	}

	/**
	 * Reads the request body from the request and returns it as a String.
	 *
	 * @param multiReadHttpServletRequest HttpServletRequest that contains the request body
	 * @return request body as a String or null
	 */
	private String getRequestBodyToString(final MultiReadHttpServletRequest multiReadHttpServletRequest) {
		try {
			// Read from request
			StringBuilder buffer = new StringBuilder();
			BufferedReader reader = multiReadHttpServletRequest.getReader();
			String line;
			while ((line = reader.readLine()) != null) {
				buffer.append(line);
			}
			return buffer.toString();
		} catch (Exception e) {
			LOGGER.error("Failed to read the request body from the request.", e);
		}
		return null;
	}

	/**
	 * Checks the URL against the regular expression to apply the filter.
	 *
	 * @param uri
	 * @return
	 */
	private boolean uriNeedsToBeFiltered(String uri) {

		if (this.urlRegularExpression == null) {
			return false;
		}

		Pattern p = Pattern.compile(this.urlRegularExpression);
		Matcher m = p.matcher(uri);

		return m.matches();
	}
}
