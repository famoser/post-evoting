/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.voteverification.service.crypto;

import javax.enterprise.inject.Produces;

import ch.post.it.evoting.cryptolib.mathematical.groups.activity.GroupElementsCompressor;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;

/**
 * Symmetric Service Producer.
 */
public class GroupElementsCompressorProducer {

	/**
	 * Returns a group element compressor instance.
	 *
	 * @return a symmetric service.
	 */
	@Produces
	public GroupElementsCompressor<ZpGroupElement> getInstance() {
		return new GroupElementsCompressor<>();
	}
}
