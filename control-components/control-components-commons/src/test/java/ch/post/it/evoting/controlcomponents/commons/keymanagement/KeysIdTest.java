/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.controlcomponents.commons.keymanagement;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class KeysIdTest {
	@Test
	void testGetCcrjReturnCodesKeysInstance() {
		final KeysId id1 = KeysId.getCcrjReturnCodesKeysInstance("ee1", "v1");
		final KeysId id2 = KeysId.getCcrjReturnCodesKeysInstance("ee1", "v1");
		final KeysId id3 = KeysId.getCcrjReturnCodesKeysInstance("ee1", "v2");
		final KeysId id4 = KeysId.getCcrjReturnCodesKeysInstance("ee2", "v1");
		final KeysId id5 = KeysId.getCcrjReturnCodesKeysInstance("ee2", "v2");

		assertAll(() -> assertEquals(id1, id2), () -> assertNotEquals(id1, id3), () -> assertNotEquals(id1, id4), () -> assertNotEquals(id1, id5));
	}

	@Test
	void testGetElectionSigningInstance() {
		final KeysId id1 = KeysId.getElectionSigningInstance("ee1");
		final KeysId id2 = KeysId.getElectionSigningInstance("ee1");
		final KeysId id3 = KeysId.getElectionSigningInstance("ee2");

		assertAll(() -> assertEquals(id1, id2), () -> assertNotEquals(id1, id3));
	}

	@Test
	void testGetCcmjElectionKeysInstance() {
		final KeysId id1 = KeysId.getCcmjElectionKeysInstance("ee1");
		final KeysId id2 = KeysId.getCcmjElectionKeysInstance("ee1");
		final KeysId id3 = KeysId.getCcmjElectionKeysInstance("ee2");

		assertAll(() -> assertEquals(id1, id2), () -> assertNotEquals(id1, id3));
	}
}
