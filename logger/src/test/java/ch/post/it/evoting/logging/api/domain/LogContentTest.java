/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */

package ch.post.it.evoting.logging.api.domain;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.logging.api.domain.LogContent.LogContentBuilder;

class LogContentTest {

	@Test
	void whenBuildLogInfoThenCheckAttributes() {

		LogContentBuilder builder = new LogContentBuilder();

		builder.logEvent(LogEventTest.TEST_EVENT);
		builder.objectType("objectType");
		builder.objectId("objectId");
		builder.user("userID");
		builder.electionEvent("electionEvent");

		final Map<String, String> additionalInfo = new HashMap<>();
		additionalInfo.put("exception", "message");

		builder.additionalInfo("key", "value");
		builder.additionalInfo("key2", "value2");

		LogContent logContent = builder.createLogInfo();

		assertEquals("objectType", logContent.getObjectType());
		assertEquals("objectId", logContent.getObjectId());

		String value = logContent.getAdditionalInfo().get("key");
		String value2 = logContent.getAdditionalInfo().get("key2");

		assertAll(() -> assertNotNull(value), () -> assertNotNull(value2), () -> assertEquals("value", value), () -> assertEquals("value2", value2));
	}
}
