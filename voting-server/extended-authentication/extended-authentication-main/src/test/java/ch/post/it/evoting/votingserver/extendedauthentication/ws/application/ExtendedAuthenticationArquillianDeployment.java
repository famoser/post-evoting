/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.extendedauthentication.ws.application;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import com.sun.net.httpserver.HttpServer;

import ch.post.it.evoting.domain.ObjectMappers;
import ch.post.it.evoting.domain.election.model.certificate.CertificateEntity;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.service.RemoteCertificateServiceImpl;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.ApplicationExceptionHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.DuplicateEntryExceptionHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.GlobalExceptionHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.ResourceNotFoundExceptionHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.RetrofitErrorHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.ValidationExceptionHandler;
import ch.post.it.evoting.votingserver.extendedauthentication.services.infrastructure.health.HealthCheckRegistryProducer;
import ch.post.it.evoting.votingserver.extendedauthentication.services.infrastructure.remote.EaRemoteCertificateServiceImpl;
import ch.post.it.evoting.votingserver.extendedauthentication.ws.application.it.RemoteCertificateServiceForTests;
import ch.post.it.evoting.votingserver.extendedauthentication.ws.application.it.SystemPropertiesLoader;

@ArquillianSuiteDeployment
@SuppressWarnings("restriction")
public class ExtendedAuthenticationArquillianDeployment extends ExtendedAuthenticationBaseITestCase {

	private static final String ADMIN_BOARD_CERT_URL = "/CR/certificates/public/name/AdministrationBoard " + TEST_ADMINBOARD_ID;

	private static final SystemPropertiesLoader SYSTEM_PROPERTIES_LOADER = new SystemPropertiesLoader();

	static HttpServer mockHTTPServer;

	static {
		SYSTEM_PROPERTIES_LOADER.setProperties();
	}

	@Deployment
	public static WebArchive createDeployment() throws Exception {

		final String crContext = SYSTEM_PROPERTIES_LOADER.load().getProperty(RemoteCertificateServiceImpl.CERTIFICATES_CONTEXT_URL_PROPERTY);
		final int serverPort = new URL(crContext).getPort();

		createCryptoMaterial();
		setupHttpServer(serverPort);

		return ShrinkWrap.create(WebArchive.class, "ea-ws-rest.war").addClasses(org.slf4j.Logger.class).addPackage("ch.post.it.evoting.logging")
				.addPackage("ch.post.it.evoting.cryptolib").addPackages(true, "org.h2").addPackages(true, "com.fasterxml.jackson.jaxrs")
				.addPackages(true, "ch.post.it.evoting.votingserver.extendedauthentication.ws")
				.addPackages(true, "ch.post.it.evoting.votingserver.extendedauthentication.services")
				.addPackages(true, "ch.post.it.evoting.votingserver.extendedauthentication.domain").addPackages(true,
						Filters.exclude(ResourceNotFoundExceptionHandler.class, GlobalExceptionHandler.class, ValidationExceptionHandler.class,
								ApplicationExceptionHandler.class, DuplicateEntryExceptionHandler.class, RetrofitErrorHandler.class),
						"ch.post.it.evoting.votingserver.commons").deleteClass(EaRemoteCertificateServiceImpl.class)
				.deleteClass(HealthCheckRegistryProducer.class).addClass(RemoteCertificateServiceForTests.class).addAsResource("log4j2.xml")
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml").addAsManifestResource("test-persistence.xml", "persistence.xml");
	}

	private static void setupHttpServer(int port) {

		try {

			final InetSocketAddress address = new InetSocketAddress(port);
			mockHTTPServer = HttpServer.create(address, 0);
			mockHTTPServer.createContext(ADMIN_BOARD_CERT_URL, crHandler -> {

				CertificateEntity ce = new CertificateEntity();
				ce.setCertificateContent(certificateString);
				final String response = ObjectMappers.toJson(ce);
				crHandler.getResponseHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
				crHandler.sendResponseHeaders(Response.Status.OK.getStatusCode(), response.length());
				final OutputStream os = crHandler.getResponseBody();
				os.write(response.getBytes(StandardCharsets.UTF_8));
				os.close();

			});

			mockHTTPServer.setExecutor(null); // creates a default executor
			mockHTTPServer.start();

		} catch (Exception exception) {
			throw new RuntimeException("Failed to create HTTP server on free port of localhost", exception);
		}
	}
}
