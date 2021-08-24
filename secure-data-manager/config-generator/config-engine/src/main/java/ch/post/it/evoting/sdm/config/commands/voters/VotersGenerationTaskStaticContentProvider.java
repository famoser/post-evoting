/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;
import ch.post.it.evoting.domain.election.Ballot;
import ch.post.it.evoting.domain.election.Contest;
import ch.post.it.evoting.domain.election.ElectionAttributes;
import ch.post.it.evoting.domain.election.ElectionOption;
import ch.post.it.evoting.domain.election.EncryptionParameters;
import ch.post.it.evoting.sdm.config.exceptions.CreateVotingCardSetException;

public class VotersGenerationTaskStaticContentProvider {

	private final ZpSubgroup zpsubgroup;

	private final EncryptionParameters encryptionParameters;

	private final List<ZpGroupElement> optionRepresentations;

	private final Map<BigInteger, List<String>> representationsWithCorrectness;

	public VotersGenerationTaskStaticContentProvider(final EncryptionParameters encryptionParameters, final VotersParametersHolder holder) {
		this.encryptionParameters = encryptionParameters;
		try {
			zpsubgroup = new ZpSubgroup(new BigInteger(encryptionParameters.getG()), new BigInteger(encryptionParameters.getP()),
					new BigInteger(encryptionParameters.getQ()));
		} catch (GeneralCryptoLibException e) {
			throw new CreateVotingCardSetException("A problem occurs when creating the mathematical group from encryption parameters.", e);
		}
		optionRepresentations = obtainOptionRepresentationsFromBallot(holder.getBallot(), zpsubgroup);
		representationsWithCorrectness = obtainAttributtesWithCorrectnessByRepresentation(holder.getBallot());
	}

	private List<ZpGroupElement> obtainOptionRepresentationsFromBallot(final Ballot enrichedBallot, final ZpSubgroup subgroup) {
		List<ZpGroupElement> optionRepresentationList = new ArrayList<>();
		try {
			for (final Contest contest : enrichedBallot.getContests()) {
				for (final ElectionOption electionOption : contest.getOptions()) {
					ZpGroupElement groupElement = new ZpGroupElement(new BigInteger(electionOption.getRepresentation()), subgroup);
					optionRepresentationList.add(groupElement);
				}
			}

		} catch (GeneralCryptoLibException e) {
			throw new CreateVotingCardSetException("An error occurred when obtaining the option's representation.", e);
		}
		return optionRepresentationList;
	}

	private Map<BigInteger, List<String>> obtainAttributtesWithCorrectnessByRepresentation(final Ballot enrichedBallot) {

		final List<ElectionOption> electionOptions = new ArrayList<>();
		final List<String> correctnessAttributes = new ArrayList<>();

		electionOptions.addAll(enrichedBallot.getContests().stream().flatMap(contest -> contest.getOptions().stream()).collect(Collectors.toList()));

		correctnessAttributes.addAll(enrichedBallot.getContests().stream().flatMap(contest -> contest.getAttributes().stream())
				.filter(optionAttribute -> optionAttribute.isCorrectness()).map(optionAttribute -> optionAttribute.getId())
				.collect(Collectors.toList()));

		Map<String, List<String>> relatedCorrectnessAttributes = new HashMap<>();
		List<ElectionAttributes> allAttributes = enrichedBallot.getContests().stream().flatMap(contest -> contest.getAttributes().stream())
				.collect(Collectors.toList());
		for (ElectionAttributes electionAttributes : allAttributes) {
			for (String relatedAttribute : electionAttributes.getRelated()) {
				if (correctnessAttributes.contains(relatedAttribute)) {
					addRelationsToMap(relatedCorrectnessAttributes, electionAttributes, relatedAttribute);
				}
			}
		}
		return mapRepresentationsWithCorrectnessAttributeIds(electionOptions, correctnessAttributes, relatedCorrectnessAttributes);
	}

	private void addRelationsToMap(Map<String, List<String>> relatedCorrectnessAttributes, ElectionAttributes electionAttributes,
			String relatedAttribute) {
		if (relatedCorrectnessAttributes.containsKey(electionAttributes.getId())) {
			relatedCorrectnessAttributes.get(electionAttributes.getId()).add(relatedAttribute);
		} else {
			List<String> relatedTopCorrectnessAttributes = new ArrayList<>();
			relatedTopCorrectnessAttributes.add(relatedAttribute);
			relatedCorrectnessAttributes.put(electionAttributes.getId(), relatedTopCorrectnessAttributes);
		}
	}

	private Map<BigInteger, List<String>> mapRepresentationsWithCorrectnessAttributeIds(final List<ElectionOption> electionOptions,
			final List<String> correctnessAttributes, Map<String, List<String>> relatedCorrectnessAttributes) {

		final Map<BigInteger, List<String>> representationWithCorrectnessAttrs = new HashMap<>();
		for (ElectionOption electionOption : electionOptions) {
			List<String> ids = new ArrayList<>();
			List<String> relatedCorrectnesses = relatedCorrectnessAttributes.get(electionOption.getAttribute());
			if (relatedCorrectnesses != null) {
				filterIds(correctnessAttributes, ids, relatedCorrectnesses);
			}
			representationWithCorrectnessAttrs.put(new BigInteger(electionOption.getRepresentation()), ids);
		}
		return representationWithCorrectnessAttrs;
	}

	private void filterIds(final List<String> correctnessAttributes, List<String> ids, List<String> relatedCorrectnesses) {
		for (String related : relatedCorrectnesses) {
			if (correctnessAttributes.contains(related)) {
				ids.add(related);
			}
		}
	}

	public ZpSubgroup getZpsubgroup() {
		return zpsubgroup;
	}

	public EncryptionParameters getEncryptionParameters() {
		return encryptionParameters;
	}

	public List<ZpGroupElement> getOptionRepresentations() {
		return optionRepresentations;
	}

	public Map<BigInteger, List<String>> getRepresentationsWithCorrectness() {
		return representationsWithCorrectness;
	}

}
