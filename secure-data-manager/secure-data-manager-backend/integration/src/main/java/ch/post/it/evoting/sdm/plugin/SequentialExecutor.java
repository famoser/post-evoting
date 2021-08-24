/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.plugin;

import java.util.List;

public interface SequentialExecutor {

	void execute(List<String> commands, Parameters parameters, ExecutionListener listener);

}
