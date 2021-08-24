/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * This class is used to temporarily hold complex objects not supported in String Batch execution context, but that need to be passed around the
 * multiple steps of an execution. It is meant to be constructed as a Spring singleton bean
 */
public class JobExecutionObjectContext {

	private final Map<String, Object> map;

	public JobExecutionObjectContext() {
		map = new HashMap<>();
	}

	public <T> void put(final String jobInstanceId, final T value, final Class<T> clazz) {
		// add or update the value if not already exists (might be dangerous..)
		final String objectName = clazz.getSimpleName();
		map.put(generateKey(jobInstanceId, objectName), value);
	}

	public <T> T get(final String jobInstanceId, final Class<T> clazz) {
		final String objectName = clazz.getSimpleName();
		return (T) map.get(generateKey(jobInstanceId, objectName));
	}

	public void removeAll(final String jobInstanceId) {
		Objects.requireNonNull(jobInstanceId, "jobInstanceId cannot be null");
		final String[] keySet = map.keySet().toArray(new String[0]);
		Stream.of(keySet).filter(key -> StringUtils.isNotBlank(key)).filter(key -> key.startsWith(jobInstanceId)).forEach(key -> map.remove(key));
	}

	private String generateKey(String key, String suffix) {
		return String.format("%s#%s", key, suffix);
	}

}
