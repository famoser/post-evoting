/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.ws.application.operation;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.cryptolib.certificates.bean.X509CertificateType;
import ch.post.it.evoting.domain.election.model.tenant.TenantActivationData;
import ch.post.it.evoting.domain.election.model.tenant.TenantInstallationData;
import ch.post.it.evoting.votingserver.commons.beans.validation.ContextValidationResult;
import ch.post.it.evoting.votingserver.commons.beans.validation.ValidationType;
import ch.post.it.evoting.votingserver.commons.domain.model.tenant.TenantKeystoreRepository;
import ch.post.it.evoting.votingserver.commons.tenant.TenantActivator;
import ch.post.it.evoting.votingserver.voteverification.domain.model.tenant.TenantKeystore;
import ch.post.it.evoting.votingserver.voteverification.domain.model.tenant.VvTenantKeystoreRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.tenant.VvTenantSystemKeys;

/**
 * Endpoint for upload the information during the installation of the tenant in the system
 */
@Path("tenantdata")
@Stateless(name = "vv-tenantDataResource")
public class VvTenantDataResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(VvTenantDataResource.class);

	private static final String TENANT_PARAMETER = "tenantId";

	private static final String CONTEXT = "VV";

	@EJB
	@VvTenantKeystoreRepository
	TenantKeystoreRepository tenantKeystoreRepository;

	@EJB
	private VvTenantSystemKeys vvTenantSystemKeys;

	/**
	 * Installs the tenant keystores in the Service
	 *
	 * @param data, json object containing the information
	 */
	@POST
	@Path("/tenant/{tenantId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response saveTenantData(
			@PathParam(TENANT_PARAMETER)
			final String tenantId, final TenantInstallationData data) {

		try {

			LOGGER.info("VV - install tenant request received");

			TenantKeystore tenantKeystoreEntity = new TenantKeystore();
			tenantKeystoreEntity.setKeystoreContent(data.getEncodedData());
			tenantKeystoreEntity.setTenantId(tenantId);
			tenantKeystoreEntity.setKeyType(X509CertificateType.ENCRYPT.name());

			tenantKeystoreRepository.save(tenantKeystoreEntity);

			TenantActivator tenantActivator = new TenantActivator(tenantKeystoreRepository, vvTenantSystemKeys, CONTEXT);
			tenantActivator.activateUsingReceivedKeystore(tenantId, data.getEncodedData());

			return Response.ok().build();

		} catch (Exception e) {
			String errorMsg = "VV - error while trying to install a tenant: " + e.getMessage();
			LOGGER.error(errorMsg);
			throw new IllegalStateException(errorMsg, e);
		}
	}

	@POST
	@Path("activatetenant")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response activateTenant(final TenantActivationData tenantActivationData) {

		try {

			LOGGER.info("VV - activate tenant request received");

			TenantActivator tenantActivator = new TenantActivator(tenantKeystoreRepository, vvTenantSystemKeys, CONTEXT);
			tenantActivator.activateFromDB(tenantActivationData);

			return Response.ok().build();

		} catch (Exception e) {
			String errorMsg = "VV - error while trying to install a tenant: " + e.getMessage();
			LOGGER.error(errorMsg);
			throw new IllegalStateException(errorMsg, e);
		}
	}

	/**
	 * Check if a tenant has been activated for this context
	 *
	 * @param tenantId - identifier of the tenant
	 * @return
	 */
	@GET
	@Path("activatetenant/tenant/{tenantId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response checkTenantActivation(
			@PathParam(TENANT_PARAMETER)
			final String tenantId) {

		ContextValidationResult validation = new ContextValidationResult.Builder().setContextName(CONTEXT)
				.setValidationType(ValidationType.TENANT_ACTIVATION).setResult(vvTenantSystemKeys.getInitialized(tenantId)).build();
		return Response.ok(validation).build();
	}
}
