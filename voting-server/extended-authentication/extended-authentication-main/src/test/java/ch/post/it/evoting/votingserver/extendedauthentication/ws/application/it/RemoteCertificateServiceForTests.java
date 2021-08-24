/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.extendedauthentication.ws.application.it;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;

import ch.post.it.evoting.domain.election.model.certificate.CertificateEntity;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.client.RetrofitException;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.service.RemoteCertificateService;
import ch.post.it.evoting.votingserver.commons.infrastructure.remote.service.RemoteCertificateServiceImpl;
import ch.post.it.evoting.votingserver.extendedauthentication.services.infrastructure.remote.EaRemoteCertificateService;

/**
 * Remote Repository for handling certificates
 */
@Stateless(name = "eaRemoteCertificateService")
@EaRemoteCertificateService
public class RemoteCertificateServiceForTests extends RemoteCertificateServiceImpl implements RemoteCertificateService {

	@Override
	@PostConstruct
	public void intializeElectionInformationAdminClient() {
		SystemPropertiesLoader spl = new SystemPropertiesLoader();
		spl.setProperties();
		super.intializeElectionInformationAdminClient();
	}

	@Override
	public CertificateEntity getAdminBoardCertificate(String id) throws RetrofitException {
		return super.getAdminBoardCertificate(id);
	}

	@Override
	public CertificateEntity getTenantCACertificate(String tenantId) throws RetrofitException {
		return super.getTenantCACertificate(tenantId);
	}
}
