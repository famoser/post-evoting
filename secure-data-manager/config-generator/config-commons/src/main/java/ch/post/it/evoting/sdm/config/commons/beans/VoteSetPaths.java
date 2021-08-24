/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commons.beans;

import java.nio.file.Path;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class VoteSetPaths {

	private List<Path> paths;

	@JsonCreator
	public VoteSetPaths(
			@JsonProperty("paths")
					List<Path> paths) {
		super();
		this.paths = paths;
	}

	public List<Path> getPaths() {
		return paths;
	}

	public void setPaths(List<Path> paths) {
		this.paths = paths;
	}

}
