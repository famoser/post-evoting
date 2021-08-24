/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.service.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import javax.ws.rs.client.WebTarget;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import ch.post.it.evoting.sdm.commons.Constants;
import ch.post.it.evoting.sdm.commons.PathResolver;
import ch.post.it.evoting.sdm.domain.model.ballotbox.BallotBoxRepository;
import ch.post.it.evoting.sdm.domain.model.electionevent.ElectionEventRepository;
import ch.post.it.evoting.sdm.domain.model.generator.DataGeneratorResponse;
import ch.post.it.evoting.sdm.domain.service.utils.SystemTenantPublicKeyLoader;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;

/**
 * JUnit for the {@link BallotBoxDataGeneratorServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class BallotBoxDataGeneratorServiceImplTest {

	@InjectMocks
	@Spy
	private final BallotBoxDataGeneratorServiceImpl ballotBoxDataGeneratorService = new BallotBoxDataGeneratorServiceImpl();
	private final String configPropertiesPath = "";
	private final String ballotBoxAsJson =
			"{ \"id\": \"1234aa1d3f194c11aac03e421472b6bb\", \"defaultTitle\": \"Ballot Box Title\", \"defaultDescription\": \"Ballot Box Description\", \"alias\": \"Ballot Box Alias\", \"dateFrom\": \"12/12/2012\", \"dateTo\": \"14/12/2012\",\"gracePeriod\": \"142\","
					+ "\"electionEvent\": { \"id\": \"314bd34dcf6e4de4b771a92fa3849d3d\"},"
					+ "\"ballot\": { \"id\": \"dd5bd34dcf6e4de4b771a92fa38abc11\"},"
					+ "\"electoralAuthority\": { \"id\": \"hhhbd34dcf6e4de4b771a92fa38abhhh\"}, \"test\": \"false\"}";
	private final String electionEventAsJson = "{ \"id\": \"1234aa1d3f194c11aac03e421472b6bb\", \"dateFrom\": \"12/12/2012\", \"dateTo\": \"14/12/2012\", \"settings\":{\"certificatesValidityPeriod\":1, \"writeInAlphabet\" : \"\"}}";

	@Mock
	private BallotBoxRepository ballotBoxRepositoryMock;

	@Mock
	private ElectionEventRepository electionEventRepository;

	@Mock
	private PathResolver pathResolverMock;

	@Mock
	private SystemTenantPublicKeyLoader systemTenantPublicKeyLoader;

	@Mock
	private Path pathMock;

	@Mock
	private Path configPathMock;

	@Mock
	private WebTarget webTarget;

	private String ballotBoxId;
	private String electionEventId;

	@Test
	void generateWithIdNull() throws IOException {
		DataGeneratorResponse result = ballotBoxDataGeneratorService.generate(ballotBoxId, electionEventId);
		assertFalse(result.isSuccessful());
	}

	@Test
	void generateWithIdEmpty() throws IOException {
		ballotBoxId = "";
		DataGeneratorResponse result = ballotBoxDataGeneratorService.generate(ballotBoxId, electionEventId);
		assertFalse(result.isSuccessful());
	}

	@Test
	void generateWithIdNotFound() throws IOException {
		ballotBoxId = "123456";

		when(ballotBoxRepositoryMock.find(ballotBoxId)).thenReturn(JsonConstants.EMPTY_OBJECT);

		DataGeneratorResponse result = ballotBoxDataGeneratorService.generate(ballotBoxId, electionEventId);
		assertFalse(result.isSuccessful());
	}

	@Test
	void generateThrowRuntimeException()
			throws IOException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		setFieldThatSpecifiesThePathOfCertProperties();

		ballotBoxId = "1234aa1d3f194c11aac03e421472b6bb";
		electionEventId = "1234aa1d3f194c11aac03e431472b6bb";

		when(ballotBoxRepositoryMock.find(ballotBoxId)).thenReturn(ballotBoxAsJson);
		when(pathResolverMock.resolve(anyString())).thenReturn(pathMock);
		when(pathMock.resolve(anyString())).thenReturn(configPathMock);
		when(configPathMock.toString()).thenReturn(configPropertiesPath);
		when(systemTenantPublicKeyLoader.load(anyString(), anyString(), any())).thenReturn("XXXXXXXXXXXXXXXXXXXXXXXXXX");

		when(electionEventRepository.find(electionEventId)).thenReturn(electionEventAsJson);
		doReturn(webTarget).when(ballotBoxDataGeneratorService).createWebClient();
		when(webTarget.request()).thenThrow(RuntimeException.class);

		Field field = BallotBoxDataGeneratorServiceImpl.class.getDeclaredField("tenantId");
		field.setAccessible(true);
		field.set(ballotBoxDataGeneratorService, Constants.HUNDRED);

		assertThrows(RuntimeException.class, () -> ballotBoxDataGeneratorService.generate(ballotBoxId, electionEventId));
	}

	@Test
	void generateThrowIOException() throws IOException {
		setFieldThatSpecifiesThePathOfCertProperties();

		ballotBoxId = "1234aa1d3f194c11aac03e421472b6bb";
		electionEventId = "1234aa1d3f194c11aac03e421444b6bb";
		when(electionEventRepository.find(electionEventId)).thenReturn(electionEventAsJson);
		when(ballotBoxRepositoryMock.find(ballotBoxId)).thenReturn(ballotBoxAsJson);
		when(pathResolverMock.resolve(anyString())).thenReturn(pathMock);
		when(pathMock.resolve(anyString())).thenReturn(configPathMock);
		when(configPathMock.toString()).thenReturn(configPropertiesPath);

		doThrow(IOException.class).when(systemTenantPublicKeyLoader).load(any(), any(), any());

		assertThrows(IOException.class, () -> ballotBoxDataGeneratorService.generate(ballotBoxId, electionEventId));
	}

	@Test
	void generateWithValidId() throws IOException {
		ballotBoxId = "1234aa1d3f194c11aac03e421472b6bb";
		electionEventId = "1234aa1d3f194c11aac03e421444b6bb";
		/*
		 * when(electionEventRepository.findByElectionEvent(electionEventId)).
		 * thenReturn(electionEventAsJson); when(ballotBoxRepositoryMock.find(ballotBoxId)).thenReturn(
		 * ballotBoxAsJson); when(ballotBoxRepositoryMock.find(ballotBoxId)).thenReturn(
		 * ballotBoxAsJson); when(pathResolverMock.resolve(Mockito.anyString())).thenReturn( pathMock);
		 * when(pathMock.toString()).thenReturn(configPath);
		 * when(pathMock.resolve(Mockito.anyString())).thenReturn(configPathMock );
		 * when(configPathMock.toString()).thenReturn(configPropertiesPath);
		 * javax.ws.rs.client.Entity<String> argument = javax.ws.rs.client.Entity.entity("",
		 * MediaType.APPLICATION_JSON); doReturn(webTarget).when(ballotBoxDataGeneratorService).
		 * createWebClient(); when(webTarget.request()).thenReturn(builderMock);
		 * when(when(builderMock.post(argument)).thenReturn(responseMock));
		 */
		DataGeneratorResponse response = new DataGeneratorResponse();
		doReturn(response).when(ballotBoxDataGeneratorService).generate(ballotBoxId, electionEventId);

		DataGeneratorResponse result = ballotBoxDataGeneratorService.generate(ballotBoxId, electionEventId);
		assertTrue(result.isSuccessful());
	}

	private void setFieldThatSpecifiesThePathOfCertProperties() throws UnsupportedEncodingException {

		ClassLoader classLoader = getClass().getClassLoader();
		String pathAsString = URLDecoder
				.decode(classLoader.getResource("properties/ballotBoxX509Certificate.properties").getPath(), StandardCharsets.UTF_8.toString());
		ReflectionTestUtils.setField(ballotBoxDataGeneratorService, "ballotBoxCertificateProperties", pathAsString);
	}
}
