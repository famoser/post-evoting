/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.returncodes.domain;

import org.springframework.data.repository.CrudRepository;

interface VerificationCardPublicKeyExtendedRawRepository extends CrudRepository<VerificationCardPublicKeyExtendedRaw, String> {
}
