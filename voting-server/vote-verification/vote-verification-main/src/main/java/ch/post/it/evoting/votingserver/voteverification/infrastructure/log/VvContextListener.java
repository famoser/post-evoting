/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.infrastructure.log;

import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.post.it.evoting.votingserver.commons.domain.model.tenant.TenantKeystoreRepository;
import ch.post.it.evoting.votingserver.commons.tenant.TenantActivator;
import ch.post.it.evoting.votingserver.voteverification.domain.model.tenant.VvTenantKeystoreRepository;
import ch.post.it.evoting.votingserver.voteverification.domain.model.tenant.VvTenantSystemKeys;

/**
 * Defines any steps to be performed when the VV context is first initialized and destroyed.
 */
public class VvContextListener implements ServletContextListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(VvContextListener.class);

	private static final String CONTEXT = "VV";

	@EJB
	@VvTenantKeystoreRepository
	TenantKeystoreRepository tenantKeystoreRepository;

	@EJB
	private VvTenantSystemKeys vvTenantSystemKeys;

	/**
	 * @see ServletContextListener#contextInitialized(ServletContextEvent)
	 */
	@Override
	public void contextInitialized(final ServletContextEvent servletContextEvent) {

		LOGGER.info(CONTEXT + " - context initialized, will attempt to activate tenant if data exists in DB");

		TenantActivator tenantActivator = new TenantActivator(tenantKeystoreRepository, vvTenantSystemKeys, CONTEXT);

		List<String> tenantIDsActivatedTenants = tenantActivator.activateTenantsFromDbAndFiles();

		LOGGER.info(CONTEXT + " - completed tenant activation process");
		for (String tenantID : tenantIDsActivatedTenants) {
			LOGGER.info(CONTEXT + " - activated tenant: {}", tenantID);
		}
	}

	/**
	 * @see ServletContextListener#contextDestroyed(ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(final ServletContextEvent servletContextEvent) {
		// Nothing to do on context destruction.
	}
}
