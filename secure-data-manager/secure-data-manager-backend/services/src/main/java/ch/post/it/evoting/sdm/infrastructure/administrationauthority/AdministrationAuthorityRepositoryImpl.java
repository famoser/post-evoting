/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure.administrationauthority;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ch.post.it.evoting.sdm.domain.model.administrationauthority.AdministrationAuthority;
import ch.post.it.evoting.sdm.domain.model.administrationauthority.AdministrationAuthorityRepository;
import ch.post.it.evoting.sdm.infrastructure.AbstractEntityRepository;
import ch.post.it.evoting.sdm.infrastructure.DatabaseManager;

/**
 * Implementation of operations with election event.
 */
@Repository
public class AdministrationAuthorityRepositoryImpl extends AbstractEntityRepository implements AdministrationAuthorityRepository {

	/**
	 * Constructor.
	 *
	 * @param databaseManager
	 */
	@Autowired
	public AdministrationAuthorityRepositoryImpl(final DatabaseManager databaseManager) {
		super(databaseManager);
	}

	@PostConstruct
	@Override
	public void initialize() {
		super.initialize();
	}

	@Override
	protected String entityName() {
		return AdministrationAuthority.class.getSimpleName();
	}
}
