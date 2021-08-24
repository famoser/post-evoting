/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.service.impl;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.springframework.stereotype.Service;

import ch.post.it.evoting.sdm.domain.model.EntityRepository;
import ch.post.it.evoting.sdm.domain.model.status.SynchronizeStatus;
import ch.post.it.evoting.sdm.infrastructure.JsonConstants;
import ch.post.it.evoting.sdm.infrastructure.service.ConfigurationEntityStatusService;

/**
 * This class manages the status of a configuration entity. As we are working with entity in format json, the type is string.
 */
@Service
public class ConfigurationEntityStatusServiceImpl implements ConfigurationEntityStatusService {

	/**
	 * @see ConfigurationEntityStatusService#update(java.lang.String, java.lang.String, EntityRepository)
	 */
	@Override
	public String update(String newStatus, String id, EntityRepository repository) {
		JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
		jsonObjectBuilder.add(JsonConstants.ID, id);
		jsonObjectBuilder.add(JsonConstants.STATUS, newStatus);
		jsonObjectBuilder.add(JsonConstants.SYNCHRONIZED, Boolean.FALSE.toString());
		return repository.update(jsonObjectBuilder.build().toString());
	}

	/**
	 * @see ConfigurationEntityStatusService#updateWithSynchronizedStatus(String, String, EntityRepository, SynchronizeStatus)
	 */
	@Override
	public String updateWithSynchronizedStatus(String newStatus, String id, EntityRepository repository, SynchronizeStatus syncDetails) {
		JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
		jsonObjectBuilder.add(JsonConstants.ID, id);
		jsonObjectBuilder.add(JsonConstants.STATUS, newStatus);
		jsonObjectBuilder.add(JsonConstants.SYNCHRONIZED, syncDetails.getIsSynchronized().toString());
		jsonObjectBuilder.add(JsonConstants.DETAILS, syncDetails.getStatus());
		return repository.update(jsonObjectBuilder.build().toString());
	}

}
