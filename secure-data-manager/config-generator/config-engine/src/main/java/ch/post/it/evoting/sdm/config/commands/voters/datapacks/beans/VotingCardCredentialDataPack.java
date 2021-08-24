/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans;

import java.security.KeyPair;

import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.sdm.datapacks.beans.SerializedCredentialDataPack;

public class VotingCardCredentialDataPack extends SerializedCredentialDataPack {

	private KeyPair keyPairSign;

	private KeyPair keyPairAuth;

	private CryptoAPIX509Certificate certificateSign;

	private CryptoAPIX509Certificate certificateAuth;

	public KeyPair getKeyPairSign() {
		return keyPairSign;
	}

	public void setKeyPairSign(final KeyPair keyPairSign) {
		this.keyPairSign = keyPairSign;
	}

	public KeyPair getKeyPairAuth() {
		return keyPairAuth;
	}

	public void setKeyPairAuth(final KeyPair keyPairAuth) {
		this.keyPairAuth = keyPairAuth;
	}

	public CryptoAPIX509Certificate getCertificateSign() {
		return certificateSign;
	}

	public void setCertificateSign(final CryptoAPIX509Certificate certificateSign) {
		this.certificateSign = certificateSign;
	}

	public CryptoAPIX509Certificate getCertificateAuth() {
		return certificateAuth;
	}

	public void setCertificateAuth(final CryptoAPIX509Certificate certificateAuth) {
		this.certificateAuth = certificateAuth;
	}
}
