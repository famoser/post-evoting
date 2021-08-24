/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.domain.model.sdmconfig;

import java.util.HashMap;
import java.util.Map;

public class SdmConfigData {

	Map<String, Object> config = new HashMap<String, Object>();

	public Map<String, Object> getConfig() {
		return config;
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

}
