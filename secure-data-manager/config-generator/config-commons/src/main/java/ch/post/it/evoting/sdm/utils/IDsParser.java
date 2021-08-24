/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.utils;

import java.util.Arrays;
import java.util.List;

/**
 * A utility class to parse IDs from command line
 */
public class IDsParser {

	/*
	 * This utility class parse a list of ids following this format:
	 * "[08b82ffc12e84dd6973ffd7b9feadeee,451a6ffc3e214ca8ae5c451d82e7fbe4,17ccbe962cf341bc93208c26e911090c,4b35ae490b2a495a98e709fb004e22a1,a524e6493db74c49b55a1c4547ed77a4,cf1970870e5049f78e5ecf63255bc7cc,f1a6106981de4ab3bac66b0e0a292a08,4799aadc61c24ef39cba775dad185991,c4c71f4c11e74408a1ad84d3e9df5bd3,570df8ceddb94171ad2b18021a4670cb]"
	 * to a List<String> object
	 */
	public List<String> parse(final String ids) {

		if (!ids.startsWith("[") || !ids.endsWith("]")) {
			throw new IllegalArgumentException("Incorrect format on IDSParser: " + ids);
		}

		// Removing "[" and "]" characters
		String strimmed = ids.substring(1, ids.length() - 1);
		return Arrays.asList(strimmed.split(","));
	}
}
