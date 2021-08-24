/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.configuration;

public enum ConfigGroupType {
	QR_2048(2048),
	QR_3072(3072);

	private final int pBitLength;

	private final int qBitLength;

	ConfigGroupType(final int pBitLength) {
		this.pBitLength = pBitLength;
		qBitLength = pBitLength - 1;
	}

	public int getL() {
		return pBitLength;
	}

	public int getN() {
		return qBitLength;
	}

}
