/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.plugin;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PluginSequenceResolver {

	private final Plugins plugins;

	public PluginSequenceResolver(Plugins plugins) {
		this.plugins = plugins;
	}

	public List<String> getActionsForPhase(String name) {
		PhaseName phaseName = PhaseName.fromValue(name);
		return getActionsForPhase(phaseName);
	}

	public List<String> getActionsForPhase(PhaseName phaseName) {

		Optional<Phase> selectedPhase = findPhaseBasedOnProperties(phaseName);

		if (selectedPhase.isPresent()) {
			return retrieveCommandLineActionsInOrder(selectedPhase.get());
		}
		return Collections.emptyList();
	}

	private Optional<Phase> findPhaseBasedOnProperties(PhaseName phaseName) {
		return plugins.getPhase().stream().filter(phase -> phaseName.equals(phase.getName())).findAny();
	}

	private List<String> retrieveCommandLineActionsInOrder(Phase phase) {
		List<Plugin> pluginList = phase.getPlugin();
		List<String> actionList = pluginList.stream().sorted((p1, p2) -> p1.getOrder().compareTo(p2.getOrder())).map(sc -> sc.getValue())
				.collect(Collectors.toList());
		return actionList;
	}

	private List<String> retrieveCommandLineActionsByOrder(Phase phase, Integer order) {
		List<Plugin> pluginList = phase.getPlugin();
		List<String> actionList = pluginList.stream().filter(p -> p.getOrder().equals(order)).map(sc -> sc.getValue()).collect(Collectors.toList());
		return actionList;
	}

	public List<String> getActionsForPhaseAndOrder(String name, Integer order) {
		PhaseName phaseName = PhaseName.fromValue(name);

		Optional<Phase> selectedPhase = findPhaseBasedOnProperties(phaseName);

		if (selectedPhase.isPresent()) {
			return retrieveCommandLineActionsByOrder(selectedPhase.get(), order);
		}
		return Collections.emptyList();
	}

}
