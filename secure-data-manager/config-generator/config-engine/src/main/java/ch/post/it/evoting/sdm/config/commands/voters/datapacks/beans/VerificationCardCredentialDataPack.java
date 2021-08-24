/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.commands.voters.datapacks.beans;

import ch.post.it.evoting.cryptolib.elgamal.bean.ElGamalKeyPair;
import ch.post.it.evoting.sdm.datapacks.beans.SerializedCredentialDataPack;

public class VerificationCardCredentialDataPack extends SerializedCredentialDataPack {

	private ElGamalKeyPair verificationCardKeyPair;

	private byte[] signatureVCardPubKeyEEIDVCID;

	public ElGamalKeyPair getVerificationCardKeyPair() {
		return verificationCardKeyPair;
	}

	public void setVerificationCardKeyPair(final ElGamalKeyPair verificationCardKeyPair) {
		this.verificationCardKeyPair = verificationCardKeyPair;
	}

	public byte[] getSignatureVCardPubKeyEEIDVCID() {
		return signatureVCardPubKeyEEIDVCID;
	}

	public void setSignatureVCardPubKeyEEIDVCID(final byte[] signatureVCardPubKeyEEIDVCID) {
		this.signatureVCardPubKeyEEIDVCID = new byte[signatureVCardPubKeyEEIDVCID.length];
		System.arraycopy(signatureVCardPubKeyEEIDVCID, 0, this.signatureVCardPubKeyEEIDVCID, 0, signatureVCardPubKeyEEIDVCID.length);
	}

}
