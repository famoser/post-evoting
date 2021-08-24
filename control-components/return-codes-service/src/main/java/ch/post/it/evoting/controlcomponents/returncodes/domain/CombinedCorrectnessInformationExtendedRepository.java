/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import org.springframework.data.repository.CrudRepository;

import ch.post.it.evoting.controlcomponents.returncodes.domain.primarykey.CombinedCorrectnessInformationExtendedPrimaryKey;

public interface CombinedCorrectnessInformationExtendedRepository
		extends CrudRepository<CombinedCorrectnessInformationExtended, CombinedCorrectnessInformationExtendedPrimaryKey> {
}
