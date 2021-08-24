/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.votermaterial.ws.application.operation;

import static org.mockito.ArgumentMatchers.any;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.RandomUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.extension.rest.client.ArquillianResteasyResource;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.certificates.bean.CertificateParameters;
import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.domain.election.model.certificate.CertificateEntity;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.service.RemoteCertificateService;
import ch.post.it.evoting.votingserver.commons.tracking.TrackIdInstance;
import ch.post.it.evoting.votingserver.commons.util.CryptoUtils;
import ch.post.it.evoting.votingserver.commons.verify.CSVVerifier;
import ch.post.it.evoting.votingserver.votermaterial.domain.model.credential.Credential;
import ch.post.it.evoting.votingserver.votermaterial.infrastructure.persistence.CredentialRepositoryImpl;
import ch.post.it.evoting.votingserver.votermaterial.infrastructure.remote.VmRemoteCertificateService;
import ch.post.it.evoting.votingserver.votermaterial.infrastructure.transaction.VoterMaterialTransactionController;

@RunWith(Arquillian.class)
public class CredentialDataResourceArquillianTest {

	private static final String TENANT_CERTIFICATE_NAME = "Tenant100CA";

	private static final String TEST_PLATFORM_NAME = "Swiss Post";

	private static final String BASE_PATH = CredentialDataResource.RESOURCE_PATH + "/";

	private static final String VM_DEPLOYMENT_PATH = "vm-ws-rest";

	private static final String TEST_ELECTION_EVENT_ID = "thisisatestelectioneventid";

	private static final String TEST_TENANT_ID = "100";

	private static final String TEST_CREDENTIAL_ID = "thisisatestcredentialid";

	private static final String TEST_DATA = "thisisthetestdata";

	private static final String TEST_VCS_ID = "thisisthevcsid";

	private static final String TEST_ADMIN_BOARD_ID = "1";

	private static final String TEST_ADMIN_BOARD_NAME = "AdministrationBoard " + TEST_ADMIN_BOARD_ID;

	private static final String TEST_CERTIFICATE_NAME = "TestCertificateName" + RandomUtils.nextInt();

	private static final String TEST_CREDENTIAL_DATA_FILE_PATH = "credentialData.csv";

	private static final String TENANT_ID = "{tenantId}";

	private static final String ELECTION_EVENT_ID = "{electionEventId}";

	private static final String VCS_ID = "{votingCardSetId}";

	private static final String AB_ID = "{adminBoardId}";

	private static String SAVE_CREDENTIAL_DATA_PATH = BASE_PATH + CredentialDataResource.SAVE_CREDENTIAL_DATA_PATH;

	@Mock
	@Produces
	@VmRemoteCertificateService
	private static RemoteCertificateService remoteCertificateService;

	@Mock
	@Produces
	private static CSVVerifier verifier;

	@PersistenceContext(unitName = "persistenceUnitJdbc")
	EntityManager entityManager;

	@Resource
	UserTransaction userTransaction;

	private static void resolvePaths() {
		// JAX-RS incompatibility with resolveTemplate method

		SAVE_CREDENTIAL_DATA_PATH = SAVE_CREDENTIAL_DATA_PATH.replace(TENANT_ID, TEST_TENANT_ID).replace(ELECTION_EVENT_ID, TEST_ELECTION_EVENT_ID)
				.replace(VCS_ID, TEST_VCS_ID).replace(AB_ID, TEST_ADMIN_BOARD_ID);
	}

	@Deployment
	public static WebArchive createDeploymentVoterMaterial() {

		resolvePaths();

		return ShrinkWrap.create(WebArchive.class, VM_DEPLOYMENT_PATH + ".war").addClass(VoterMaterialTransactionController.class)
				.addClass(CredentialRepositoryImpl.class).addClass(TrackIdInstance.class).addClass(CredentialDataResource.class)
				.addClass(VmRemoteCertificateService.class).addPackages(true, "org.slf4j")
				.addPackages(true, "ch.post.it.evoting.votingserver.commons.util").addPackages(true, "ch.post.it.evoting.logging")
				.addPackages(true, "ch.post.it.evoting.cryptolib").addPackages(true, "org.h2").addPackages(true, "com.fasterxml.jackson.jaxrs")
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml").addAsManifestResource("test-persistence.xml", "persistence.xml");
	}

	@Before
	public void setupMock() throws IOException, GeneralCryptoLibException, RetrofitException {

		CertificateEntity certificateEntity = getTestCertificateEntity();
		certificateEntity.setCertificateName(TEST_CERTIFICATE_NAME);

		Mockito.when(remoteCertificateService.getAdminBoardCertificate(TEST_ADMIN_BOARD_NAME)).thenReturn(certificateEntity);
		Mockito.when(verifier.verify(any(PublicKey.class), any(Path.class))).thenReturn(true);
	}

	@Test
	@RunAsClient
	public void testSaveCredentialData(
			@ArquillianResteasyResource("")
					ResteasyWebTarget webTarget)
			throws IOException, HeuristicRollbackException, HeuristicMixedException, NotSupportedException, RollbackException, SystemException {

		try (InputStream testData = this.getClass().getClassLoader().getResourceAsStream(TEST_CREDENTIAL_DATA_FILE_PATH)) {

			createAndPersistTestCredential();

			ClientResponse response = (ClientResponse) webTarget.path(SAVE_CREDENTIAL_DATA_PATH).request(MediaType.APPLICATION_JSON_TYPE)
					.post(Entity.entity(testData, "text/csv"));

			Assert.assertEquals(response.getStatus(), Response.Status.OK.getStatusCode());
		}

	}

	private Credential createAndPersistTestCredential()
			throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {

		Credential testCredentialEntity = new Credential();
		testCredentialEntity.setElectionEventId(TEST_ELECTION_EVENT_ID);
		testCredentialEntity.setTenantId(TEST_TENANT_ID);
		testCredentialEntity.setCredentialId(TEST_CREDENTIAL_ID);
		testCredentialEntity.setData(TEST_DATA);

		userTransaction.begin();
		entityManager.persist(testCredentialEntity);
		userTransaction.commit();

		return testCredentialEntity;
	}

	private String getCertificateAsString(String cN) throws GeneralCryptoLibException {

		final KeyPair keyPairForSigning = CryptoUtils.getKeyPairForSigning();
		CryptoAPIX509Certificate certificate = CryptoUtils.createCryptoAPIx509Certificate(cN, CertificateParameters.Type.SIGN, keyPairForSigning);

		return new String(certificate.getPemEncoded(), StandardCharsets.UTF_8);
	}

	private CertificateEntity getTestCertificateEntity() throws GeneralCryptoLibException {

		String testCertificate = getCertificateAsString(TENANT_CERTIFICATE_NAME);
		CertificateEntity testCertificateEntity = new CertificateEntity();
		testCertificateEntity.setCertificateContent(testCertificate);
		testCertificateEntity.setCertificateName(TEST_CERTIFICATE_NAME);
		testCertificateEntity.setElectionEventId(TEST_ELECTION_EVENT_ID);
		testCertificateEntity.setPlatformName(TEST_PLATFORM_NAME);
		testCertificateEntity.setTenantId(TEST_TENANT_ID);

		return testCertificateEntity;
	}

}
