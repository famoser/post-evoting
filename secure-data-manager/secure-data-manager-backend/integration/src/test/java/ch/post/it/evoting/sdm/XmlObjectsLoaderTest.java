/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import ch.post.it.evoting.sdm.plugin.XmlObjectsLoader;

class XmlObjectsLoaderTest {

	@Test
	void testXmlDeserialization() throws URISyntaxException {
		String resourcePath = this.getClass().getResource("/validPlugin.xml").toURI().getPath();
		assertDoesNotThrow(() -> XmlObjectsLoader.unmarshal(new File(resourcePath).toPath()));
	}
}
