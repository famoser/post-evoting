/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement.persistence;

import org.springframework.data.repository.CrudRepository;

public interface CcmjElectionKeysEntityRepository extends CrudRepository<CcmjElectionKeysEntity, CcmjElectionKeysEntityPrimaryKey> {
}
