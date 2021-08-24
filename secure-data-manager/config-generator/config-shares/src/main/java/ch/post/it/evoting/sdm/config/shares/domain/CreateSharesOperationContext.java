/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.domain;

/**
 * Defines the context in which a create shares operation is to be performed.
 */
public final class CreateSharesOperationContext {

	private final SharesType sharesType;

	/**
	 * @param sharesType the type of key that the shares comprise.
	 */
	public CreateSharesOperationContext(final SharesType sharesType) {
		this.sharesType = sharesType;
	}

	/**
	 * @return the sharesType.
	 */
	public SharesType getSharesType() {
		return sharesType;
	}
}
