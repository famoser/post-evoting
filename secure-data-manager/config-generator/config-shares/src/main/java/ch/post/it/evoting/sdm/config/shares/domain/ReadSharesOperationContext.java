/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.domain;

import java.security.PublicKey;

/**
 * Defines the context in which a read operation is to be performed.
 */
public final class ReadSharesOperationContext {

	private final SharesType sharesType;

	private final PublicKey authoritiesPublicKey;

	private final PublicKey boardPublicKey;

	/**
	 * @param sharesType           the type of key that the shares comprise.
	 * @param authoritiesPublicKey the public key corresponding to the private key with which the shares are signed.
	 * @param boardPublicKey       the board's public key, that will be used to extract parameters that will help the reconstruction of the private
	 *                             key.
	 */
	public ReadSharesOperationContext(final SharesType sharesType, final PublicKey authoritiesPublicKey, final PublicKey boardPublicKey) {
		this.sharesType = sharesType;
		this.authoritiesPublicKey = authoritiesPublicKey;
		this.boardPublicKey = boardPublicKey;
	}

	/**
	 * @return the sharesType.
	 */
	public SharesType getSharesType() {
		return sharesType;
	}

	/**
	 * @return the authoritiesPublicKey.
	 */
	public PublicKey getAuthoritiesPublicKey() {
		return authoritiesPublicKey;
	}

	/**
	 * @return the boardPublicKey.
	 */
	public PublicKey getBoardPublicKey() {
		return boardPublicKey;
	}
}
