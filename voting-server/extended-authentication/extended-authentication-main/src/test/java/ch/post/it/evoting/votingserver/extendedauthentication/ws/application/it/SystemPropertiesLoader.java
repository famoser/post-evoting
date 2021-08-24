/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.extendedauthentication.ws.application.it;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

public class SystemPropertiesLoader {

	public static final String FILE_NAME = "arq_system.properties";
	@ClassRule
	public static EnvironmentVariables environmentVariables = new EnvironmentVariables();

	public void setProperties() {
		Properties props = load();
		if (props != null) {
			for (Map.Entry<Object, Object> entry : props.entrySet()) {
				environmentVariables.set(entry.getKey().toString(), entry.getValue().toString());
			}
		}
	}

	public void unsetProperties() {
		Properties props = load();
		if (props != null) {
			for (Map.Entry<Object, Object> entry : props.entrySet()) {
				System.clearProperty(entry.getKey().toString());
			}
		}
	}

	public Properties load() {
		try (InputStream propsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(FILE_NAME)) {
			if (propsStream != null) {
				Properties props = new Properties();
				props.load(propsStream);
				return props;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new RuntimeException("Could not load properties", e);
		}
	}
}
