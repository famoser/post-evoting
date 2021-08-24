/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.service;

import ch.post.it.evoting.sdm.domain.model.EntityRepository;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;

/**
 * Interface for managing status of an entity from the configuration.
 */
public interface ConfigurationEntityStatusService {

	/**
	 * Updates the status of the given entity using the given repository.
	 *
	 * @param newStatus  the new state of the entity.
	 * @param id         the id of the entity to which to update the status.
	 * @param repository the specific entity repository to be used for updating the status.
	 * @return the content of the fields that were updated.
	 */
	String update(String newStatus, String id, EntityRepository repository);

	/**
	 * Updates the status of the given entity using the given repository.
	 *
	 * @param newStatus   the new state of the entity.
	 * @param id          the id of the entity to which to update the status.
	 * @param repository  the specific entity repository to be used for updating the status.
	 * @param syncDetails - details of the status of synchronization.
	 * @return
	 */
	String updateWithSynchronizedStatus(String newStatus, String id, EntityRepository repository, SynchronizeStatus syncDetails);
}
