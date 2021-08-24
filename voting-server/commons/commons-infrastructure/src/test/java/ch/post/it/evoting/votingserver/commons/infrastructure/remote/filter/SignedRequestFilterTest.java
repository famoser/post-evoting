/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.commons.infrastructure.remote.filter;

import static ch.post.it.evoting.votingserver.commons.infrastructure.remote.filter.SignedRequestFilter.URL_REGEX_INIT_PARAM_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.stores.keystore.configuration.NodeIdentifier;
import ch.post.it.evoting.votingserver.commons.exception.OvCommonsSignException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RestClientInterceptor;
import ch.post.it.evoting.votingserver.commons.sign.RequestSigner;
import ch.post.it.evoting.votingserver.commons.sign.beans.SignedRequestContent;
import ch.post.it.evoting.votingserver.commons.verify.RequestVerifier;

import mockit.Mock;
import mockit.MockUp;

public class SignedRequestFilterTest {

	@BeforeClass
	public static void setUpAll() {
		// Make sure the SignedRequestKeyManager has not already been instantiated by another JUnit test.
		SignedRequestKeyManager.instance = null;
	}

	@Test
	public void testFilterLetsTheRequestToContinue() throws IOException, ServletException, OvCommonsSignException, GeneralCryptoLibException {
		// create the objects to be mocked
		HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
		HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);

		// Create the request content
		String url = "/test";
		String method = "GET";
		String body = "1..2..3, This is the request body!";
		NodeIdentifier nodeIdentifier = NodeIdentifier.CONFIG_PLATFORM_ROOT;
		String requestOriginatorName = nodeIdentifier.name();
		SignedRequestContent signedRequestContent = new SignedRequestContent(url, method, body, requestOriginatorName);

		// Generate keys to be used
		AsymmetricService asymmetricService = new AsymmetricService();
		KeyPair keyPair = asymmetricService.getKeyPairForSigning();
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();

		// Generate request signature and verify it
		RequestSigner requestSigner = new RequestSigner();
		RequestVerifier requestVerifier = new RequestVerifier();
		byte[] signature = requestSigner.sign(signedRequestContent, privateKey);
		String signatureStrEnc = Base64.getEncoder().encodeToString(signature);
		boolean verified = requestVerifier.verifySignature(signedRequestContent, signature, publicKey);

		Assert.assertTrue(verified);

		when(httpServletRequest.getRequestURI()).thenReturn(url);
		when(httpServletRequest.getMethod()).thenReturn(method);
		when(httpServletRequest.getHeader(RestClientInterceptor.HEADER_SIGNATURE)).thenReturn(signatureStrEnc);
		when(httpServletRequest.getHeader(RestClientInterceptor.HEADER_ORIGINATOR)).thenReturn(requestOriginatorName);

		new MockUp<SignedRequestFilter>() {
			@Mock
			PublicKey getPublicKey(final NodeIdentifier nodeIdentifier) {
				return publicKey;
			}

			@Mock
			String getRequestBodyToString(final MultiReadHttpServletRequest multiReadHttpServletRequest) {
				return body;
			}

			@Mock
			private boolean uriNeedsToBeFiltered(String url) {
				return true;
			}
		};

		SignedRequestFilter signedRequestFilter = new SignedRequestFilter();
		signedRequestFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		// Verify if the response has no more interactions meaning passed the filter.
		Mockito.verifyNoMoreInteractions(httpServletResponse);

	}

	@Test
	public void testFiltersURLencodedURI() throws IOException, ServletException, OvCommonsSignException, GeneralCryptoLibException {
		// create the objects to be mocked
		HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
		HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);
		FilterConfig filterConfig = mock(FilterConfig.class);
		ServletContext servletContext = mock(ServletContext.class);

		// Create the request content
		String url = "/s%65cur%65d";
		String method = "GET";
		String body = "1..2..3, This is the request body!";
		NodeIdentifier nodeIdentifier = NodeIdentifier.CONFIG_PLATFORM_ROOT;
		String requestOriginatorName = nodeIdentifier.name();
		SignedRequestContent signedRequestContent = new SignedRequestContent(url, method, body, requestOriginatorName);

		// Generate keys to be used
		AsymmetricService asymmetricService = new AsymmetricService();
		KeyPair keyPair = asymmetricService.getKeyPairForSigning();
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();

		// Generate request signature and verify it
		RequestSigner requestSigner = new RequestSigner();
		RequestVerifier requestVerifier = new RequestVerifier();
		byte[] signature = requestSigner.sign(signedRequestContent, privateKey);
		String signatureStrEnc = Base64.getEncoder().encodeToString(signature);
		boolean verified = requestVerifier.verifySignature(signedRequestContent, signature, publicKey);

		Assert.assertTrue(verified);

		when(httpServletRequest.getRequestURI()).thenReturn(url);
		when(httpServletRequest.getMethod()).thenReturn(method);
		when(httpServletRequest.getHeader(RestClientInterceptor.HEADER_SIGNATURE)).thenReturn(signatureStrEnc);
		when(httpServletRequest.getHeader(RestClientInterceptor.HEADER_ORIGINATOR)).thenReturn(requestOriginatorName);
		when(filterConfig.getServletContext()).thenReturn(servletContext);
		when(filterConfig.getInitParameter(URL_REGEX_INIT_PARAM_NAME)).thenReturn(".*secured.*|.*platformdata.*");

		new MockUp<SignedRequestFilter>() {
			@Mock
			private boolean signatureIsValid(final MultiReadHttpServletRequest multiReadHttpServletRequest, final NodeIdentifier nodeIdentifier)
					throws IOException {
				return true;
			}
		};

		SignedRequestFilter signedRequestFilter = new SignedRequestFilter();
		signedRequestFilter.init(filterConfig);
		signedRequestFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		// Verify that the request has been filtered despite having URL encoded characters
		Mockito.verify(filterChain).doFilter(argThat(req -> req instanceof MultiReadHttpServletRequest), any());

	}

	@Test
	public void testFilterRequest401() throws IOException, ServletException, OvCommonsSignException, GeneralCryptoLibException {
		// create the objects to be mocked
		HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
		HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
		FilterChain filterChain = mock(FilterChain.class);

		String url = "/test";
		String method = "GET";
		String body = "1..2..3, This is the request body!";
		String originator = NodeIdentifier.CONFIG_PLATFORM_ROOT.name();

		AsymmetricService asymmetricService = new AsymmetricService();
		KeyPair keyPair = asymmetricService.getKeyPairForSigning();
		PublicKey publicKey = keyPair.getPublic();
		PrivateKey privateKey = keyPair.getPrivate();

		SignedRequestContent signedRequestContent = new SignedRequestContent("/wrongUrlToFail", method, body, originator);

		RequestSigner requestSigner = new RequestSigner();
		byte[] signature = requestSigner.sign(signedRequestContent, privateKey);
		String signatureStrEnc = Base64.getEncoder().encodeToString(signature);

		RequestVerifier requestVerifier = new RequestVerifier();
		boolean verified = requestVerifier.verifySignature(signedRequestContent, signature, publicKey);

		Assert.assertTrue(verified);

		when(httpServletRequest.getRequestURI()).thenReturn(url);
		when(httpServletRequest.getMethod()).thenReturn(method);
		when(httpServletRequest.getHeader(RestClientInterceptor.HEADER_SIGNATURE)).thenReturn(signatureStrEnc);
		when(httpServletRequest.getHeader(RestClientInterceptor.HEADER_ORIGINATOR)).thenReturn(NodeIdentifier.ADMIN_PORTAL.name());

		new MockUp<SignedRequestFilter>() {
			@Mock
			PublicKey getPublicKey(final NodeIdentifier nodeIdentifier) {
				return publicKey;
			}

			@Mock
			private boolean uriNeedsToBeFiltered(String url) {
				return true;
			}

		};

		SignedRequestFilter signedRequestFilter = new SignedRequestFilter();
		signedRequestFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		// verify if a setStatus() was performed with the expected code.
		verify(httpServletResponse).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test
	public void testEnum() {

		NodeIdentifier nodeIdentifier;
		try {
			String originatorStr = NodeIdentifier.ADMIN_PORTAL.name();
			nodeIdentifier = NodeIdentifier.valueOf(originatorStr);

		} catch (IllegalArgumentException e) {
			return;
		}
		Assert.assertNotNull(nodeIdentifier);

		try {
			String originatorStr = "wrong-value";
			nodeIdentifier = NodeIdentifier.valueOf(originatorStr);

		} catch (IllegalArgumentException e) {
			nodeIdentifier = null;
		}

		Assert.assertNull(nodeIdentifier);

	}

	@Test
	public void testRegularExpressions() {
		Pattern p = Pattern.compile(".*secured.*|.*platformdata.*");

		Matcher m = p.matcher("/whatever/secured");
		boolean b = m.matches();
		Assert.assertTrue(b);

	}

}
