/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans;

import java.security.KeyPair;

import ch.post.it.evoting.cryptolib.certificates.cryptoapi.CryptoAPIX509Certificate;
import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalPublicKey;
import ch.post.it.evoting.sdm.datapacks.beans.SerializedCredentialDataPack;

public class VerificationCardSetCredentialDataPack extends SerializedCredentialDataPack {

	private KeyPair verificationCardSetIssuerKeyPair;

	private CryptoAPIX509Certificate verificationCardSetIssuerCert;

	private ElGamalPublicKey choiceCodesEncryptionPublicKey;

	private ElGamalPublicKey[] nonCombinedChoiceCodesEncryptionPublicKeys;

	public ElGamalPublicKey[] getNonCombinedChoiceCodesEncryptionPublicKeys() {
		return nonCombinedChoiceCodesEncryptionPublicKeys;
	}

	public void setNonCombinedChoiceCodesEncryptionPublicKeys(ElGamalPublicKey[] nonCombinedChoiceCodesEncryptionPublicKeys) {
		this.nonCombinedChoiceCodesEncryptionPublicKeys = nonCombinedChoiceCodesEncryptionPublicKeys;
	}

	public KeyPair getVerificationCardSetIssuerKeyPair() {
		return verificationCardSetIssuerKeyPair;
	}

	public void setVerificationCardSetIssuerKeyPair(final KeyPair verificationCardSetIssuerKeyPair) {
		this.verificationCardSetIssuerKeyPair = verificationCardSetIssuerKeyPair;
	}

	public CryptoAPIX509Certificate getVerificationCardSetIssuerCert() {
		return verificationCardSetIssuerCert;
	}

	public void setVerificationCardSetIssuerCert(final CryptoAPIX509Certificate verificationCardSetIssuerCert) {
		this.verificationCardSetIssuerCert = verificationCardSetIssuerCert;
	}

	public ElGamalPublicKey getChoiceCodesEncryptionPublicKey() {
		return choiceCodesEncryptionPublicKey;
	}

	public void setChoiceCodesEncryptionPublicKey(final ElGamalPublicKey choicesCodePublicKey) {
		this.choiceCodesEncryptionPublicKey = choicesCodePublicKey;
	}

}
