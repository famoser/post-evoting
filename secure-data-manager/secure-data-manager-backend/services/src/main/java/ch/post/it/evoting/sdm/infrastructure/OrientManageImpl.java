/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.infrastructure;

import org.springframework.stereotype.Component;

import com.orientechnologies.orient.core.Orient;

/**
 * Implementation of {@link OrientManager}.
 */
@Component
public final class OrientManageImpl implements OrientManager {
	private final Orient orient;

	/**
	 * Constructor. For internal use only.
	 *
	 * @param orient
	 */
	OrientManageImpl(final Orient orient) {
		this.orient = orient;
	}

	/**
	 * Constructor.
	 */
	public OrientManageImpl() {
		this(Orient.instance());
	}

	@Override
	public boolean isActive() {
		return orient.isActive();
	}

	@Override
	public void shutdown() {
		orient.shutdown();
	}
}
