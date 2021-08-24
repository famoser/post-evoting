/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.electioninformation.ws.config;

import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import ch.post.it.evoting.cryptolib.asymmetric.service.AsymmetricService;
import ch.post.it.evoting.cryptolib.extendedkeystore.service.ExtendedKeyStoreService;
import ch.post.it.evoting.cryptolib.primitives.service.PrimitivesService;
import ch.post.it.evoting.cryptolib.proofs.service.ProofsService;
import ch.post.it.evoting.domain.election.model.ballotbox.BallotBoxId;
import ch.post.it.evoting.domain.election.model.vote.Vote;
import ch.post.it.evoting.domain.mixnet.MixnetInitialPayload;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.ApplicationExceptionHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.DuplicateEntryExceptionHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.GlobalExceptionHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.ResourceNotFoundExceptionHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.RetrofitErrorHandler;
import ch.post.it.evoting.votingserver.commons.ui.ws.rs.exceptions.ValidationExceptionHandler;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.service.ballotbox.CleansedBallotBoxService;
import ch.post.it.evoting.votingserver.electioninformation.services.domain.service.ballotbox.CleansedBallotBoxServiceImpl;
import ch.post.it.evoting.votingserver.electioninformation.services.infrastructure.health.HealthCheckRegistryProducer;

@ArquillianSuiteDeployment
public class ElectionInformationArquillianDeployment {

	private static final SystemPropertiesLoader SYSTEM_PROPERTIES_LOADER = new SystemPropertiesLoader();

	@Deployment
	public static WebArchive buildDeployable() {
		beforeDeployment();
		WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "ei.war");
		return webArchive.addPackage("ch.post.it.evoting.logging").addPackages(true,
				Filters.exclude(ExtendedKeyStoreService.class, AsymmetricService.class, PrimitivesService.class, ProofsService.class),
				"ch.post.it.evoting.cryptolib").addPackages(true, "org.h2")
				.addPackages(true, "ch.post.it.evoting.votingserver.electioninformation.services")
				.addPackages(true, "ch.post.it.evoting.votingserver.electioninformation.ws")
				.addPackages(true, "ch.post.it.evoting.votingserver.commons.logging.service")
				.addPackages(true, "ch.post.it.evoting.votingserver.commons.tracking").addPackages(true, "com.fasterxml.jackson.jaxrs")
				.addPackages(true,
						Filters.exclude(ResourceNotFoundExceptionHandler.class, GlobalExceptionHandler.class, ValidationExceptionHandler.class,
								ApplicationExceptionHandler.class, DuplicateEntryExceptionHandler.class, RetrofitErrorHandler.class,
								ExtendedKeyStoreService.class), "ch.post.it.evoting.votingserver.commons").addClasses(org.slf4j.Logger.class)
				.deleteClass(HealthCheckRegistryProducer.class).addAsResource("log4j2.xml").addAsWebInfResource("beans.xml")
				.addAsManifestResource("test-persistence.xml", "persistence.xml").deleteClass(CleansedBallotBoxServiceImpl.class);
	}

	private static void beforeDeployment() {
		SYSTEM_PROPERTIES_LOADER.setEnvironmentalVariables();
	}

	public static class CleansedBallotBoxServiceTestImpl implements CleansedBallotBoxService {

		@Override
		public boolean isBallotBoxEmpty(String electionEventId, String ballotBoxId) {
			return false;
		}

		@Override
		public MixnetInitialPayload getMixnetInitialPayload(BallotBoxId ballotBoxId) {
			return null;
		}

		@Override
		public void storeCleansedVote(Vote vote) {
		}

		@Override
		public void storeSuccessfulVote(String tenantId, String electionEventId, String ballotBoxId, String votingCardId) {
		}
	}
}
