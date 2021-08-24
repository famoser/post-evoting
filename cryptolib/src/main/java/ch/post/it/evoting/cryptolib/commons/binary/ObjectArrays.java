/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.commons.binary;

/**
 * Utility class for working with Object arrays.
 */
public final class ObjectArrays {

	private ObjectArrays() {
	}

	/**
	 * Concatenates given byte arrays.
	 *
	 * @param array1 the first array
	 * @param array2 the second array
	 * @param others the other arrays
	 * @return the byte array.
	 */
	public static byte[] concatenate(byte[] array1, byte[] array2, byte[]... others) {
		ByteArrayBuilder builder = new ByteArrayBuilder();
		builder.append(array1);
		builder.append(array2);
		for (byte[] other : others) {
			builder.append(other);
		}
		return builder.build();
	}

	/**
	 * Returns whether two Object arrays have the same content. The operation time is {@code O(array.length)} to avoid timing attacks.
	 *
	 * @param array1 the first array
	 * @param array2 the second array
	 * @return the arrays are equal.
	 */
	public static boolean constantTimeEquals(Object[] array1, Object[] array2) {
		if (array1 == null || array2 == null || array1.length != array2.length) {
			return false;
		}
		boolean equals = true;
		for (int i = 0; i < array1.length; i++) {
			Object o1 = array1[i];
			Object o2 = array2[i];
			if (!(o1 == null ? o2 == null : o1.equals(o2))) {
				equals = false;
				break;
			}
		}
		return equals;
	}
}
