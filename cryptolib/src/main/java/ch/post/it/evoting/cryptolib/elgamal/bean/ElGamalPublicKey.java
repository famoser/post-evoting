/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */
package ch.post.it.evoting.cryptolib.elgamal.bean;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonValue;

import ch.post.it.evoting.cryptolib.api.exceptions.GeneralCryptoLibException;
import ch.post.it.evoting.cryptolib.commons.binary.ObjectArrays;
import ch.post.it.evoting.cryptolib.commons.serialization.AbstractJsonSerializable;
import ch.post.it.evoting.cryptolib.commons.validations.Validate;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpGroupElement;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Encapsulates an ElGamal public key.
 *
 * <p>Instances of this class contain a list of the Zp group elements corresponding to the key and
 * the Zp subgroup to which these elements belong.
 */
@JsonRootName("publicKey")
public final class ElGamalPublicKey extends AbstractJsonSerializable {

	private final ZpGroupElement[] elements;

	private final ZpSubgroup zpSubgroup;

	/**
	 * Creates an {@link ElGamalPublicKey} object, using the specified list of Zp group elements and the specified Zp subgroup.
	 *
	 * <p>Note: For performance reasons, a group membership check is not performed for any Zp group
	 * element in the specified list. Therefore, this membership should be ensured prior to specifying the list as input to this constructor.
	 *
	 * @param elements   the list of public key Zp group elements.
	 * @param zpSubgroup the Zp subgroup to which the Zp group elements of this public key belong.
	 * @throws GeneralCryptoLibException if the list of public key Zp group elements is null or empty or if the Zp subgroup is null.
	 */
	public ElGamalPublicKey(final List<ZpGroupElement> elements, final ZpSubgroup zpSubgroup) throws GeneralCryptoLibException {
		Validate.notNullOrEmptyAndNoNulls(elements, "List of ElGamal public key elements");
		Validate.notNull(zpSubgroup, "Zp subgroup");
		this.elements = elements.toArray(new ZpGroupElement[0]);
		this.zpSubgroup = zpSubgroup;
	}

	private ElGamalPublicKey(ZpGroupElement[] elements, ZpSubgroup zpSubgroup) {
		this.elements = elements;
		this.zpSubgroup = zpSubgroup;
	}

	/**
	 * Deserializes the instance from a string in JSON format.
	 *
	 * @param json the JSON
	 * @return the instance
	 * @throws GeneralCryptoLibException failed to deserialize the instance.
	 */
	public static ElGamalPublicKey fromJson(String json) throws GeneralCryptoLibException {
		return AbstractJsonSerializable.fromJson(json, ElGamalPublicKey.class);
	}

	/**
	 * Deserializes the instance from a given byte array.
	 *
	 * @param bytes the bytes
	 * @return the instance
	 * @throws GeneralCryptoLibException failed to deserialize the instance.
	 */
	public static ElGamalPublicKey fromBytes(byte[] bytes) throws GeneralCryptoLibException {
		Validate.notNull(bytes, "bytes");
		return Codec.decodePublicKey(bytes);
	}

	/**
	 * Multiplies given keys. See {@link #multiply(ElGamalPublicKey)} for details.
	 *
	 * @param key1   the first key
	 * @param key2   the second key
	 * @param others the other keys
	 * @return the "product" key
	 * @throws GeneralCryptoLibException some of the keys is {@code null} or uses a different group.
	 */
	public static ElGamalPublicKey multiply(ElGamalPublicKey key1, ElGamalPublicKey key2, ElGamalPublicKey... others)
			throws GeneralCryptoLibException {
		ElGamalPublicKey product = key1.multiply(key2);
		for (ElGamalPublicKey other : others) {
			product = product.multiply(other);
		}
		return product;
	}

	/**
	 * Multiplies given keys. The specified collection must not be empty. See {@link #multiply(ElGamalPublicKey)} for details.
	 *
	 * @param keys the keys
	 * @return the "product" key
	 * @throws GeneralCryptoLibException some of the keys is {@code null} or uses a different group.
	 */
	public static ElGamalPublicKey multiply(Collection<ElGamalPublicKey> keys) throws GeneralCryptoLibException {
		Iterator<ElGamalPublicKey> iterator = keys.iterator();
		if (!iterator.hasNext()) {
			throw new GeneralCryptoLibException("Keys are missing.");
		}
		ElGamalPublicKey product = iterator.next();
		while (iterator.hasNext()) {
			product = product.multiply(iterator.next());
		}
		return product;
	}

	/**
	 * Creates an instance from a given memento during JSON deserialization.
	 *
	 * @param memento the memento
	 * @return
	 * @throws GeneralCryptoLibException failed to create the instance.
	 */
	@JsonCreator
	static ElGamalPublicKey fromMemento(Memento memento) throws GeneralCryptoLibException {
		Validate.notNullOrEmptyAndNoNulls(memento.elements, "List of ElGamal public key elements");
		Validate.notNull(memento.zpSubgroup, "Zp subgroup");
		ZpGroupElement[] elements = new ZpGroupElement[memento.elements.length];
		for (int i = 0; i < elements.length; i++) {
			elements[i] = new ZpGroupElement(memento.elements[i], memento.zpSubgroup);
		}
		return new ElGamalPublicKey(elements, memento.zpSubgroup);
	}

	/**
	 * Retrieves the list of public key Zp group elements. The returned list is read-only.
	 *
	 * @return the list of Zp group elements.
	 */
	public List<ZpGroupElement> getKeys() {
		return unmodifiableList(asList(elements));
	}

	/**
	 * Retrieves the Zp subgroup to which the public key Zp group elements belong.
	 *
	 * @return the Zp subgroup.
	 */
	public ZpSubgroup getGroup() {
		return zpSubgroup;
	}

	/**
	 * "Multiplies" this key by a given one.
	 *
	 * <p>ElGamal public keys with the same number of elements have a group property, namely they are
	 * members of {@code Zp^n} subgroup, and multiplication is considered as a group operation.
	 *
	 * <p>The element number of the returned public key is the minimum of element number of the keys
	 * being multiplied.
	 *
	 * @param other the other
	 * @return the "product" key
	 * @throws GeneralCryptoLibException the other key is {@code null} or uses a different group.
	 */
	public ElGamalPublicKey multiply(ElGamalPublicKey other) throws GeneralCryptoLibException {
		Validate.notNull(other, "Other key");
		if (!zpSubgroup.equals(other.zpSubgroup)) {
			throw new GeneralCryptoLibException("The other key uses a different group.");
		}
		int length = min(elements.length, other.elements.length);
		ZpGroupElement[] newElements = new ZpGroupElement[length];
		for (int i = 0; i < newElements.length; i++) {
			newElements[i] = elements[i].multiply(other.elements[i]);
		}
		return new ElGamalPublicKey(newElements, zpSubgroup);
	}

	/**
	 * "Inverts" this key.
	 *
	 * <p>ElGamal public keys with the same number of elements have a group property, namely they are
	 * members of {@code Zp^n} subgroup, and inversion is considered as a group operation.
	 *
	 * @return the inverted key.
	 */
	public ElGamalPublicKey invert() {
		ZpGroupElement[] newElements = new ZpGroupElement[elements.length];
		for (int i = 0; i < newElements.length; i++) {
			newElements[i] = elements[i].invert();
		}
		return new ElGamalPublicKey(newElements, zpSubgroup);
	}

	/**
	 * "Divides" this key by a given one.
	 *
	 * <p>ElGamal public keys with the same number of elements have a group property, namely they are
	 * members of {@code Zp^n} subgroup, and division is considered as a group operation.
	 *
	 * <p>The element number of the returned public key is the minimum of element number of the keys
	 * being multiplied.
	 *
	 * @param other the other
	 * @return the "quotient" key
	 * @throws GeneralCryptoLibException the other key is {@code null} or uses a different group.
	 */
	public ElGamalPublicKey divide(ElGamalPublicKey other) throws GeneralCryptoLibException {
		Validate.notNull(other, "Other key");
		return multiply(other.invert());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(elements);
		result = prime * result + ((zpSubgroup == null) ? 0 : zpSubgroup.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ElGamalPublicKey other = (ElGamalPublicKey) obj;
		if (!ObjectArrays.constantTimeEquals(elements, other.elements)) {
			return false;
		}
		if (zpSubgroup == null) {
			return other.zpSubgroup == null;
		} else {
			return zpSubgroup.equals(other.zpSubgroup);
		}
	}

	/**
	 * Serializes the instance to a byte array.
	 *
	 * @return the bytes.
	 */
	public byte[] toBytes() {
		return Codec.encode(this);
	}

	/**
	 * Returns a memento used during JSON serialization.
	 *
	 * @return a memento.
	 */
	@JsonValue
	Memento toMemento() {
		Memento memento = new Memento();
		memento.zpSubgroup = zpSubgroup;
		memento.elements = new BigInteger[elements.length];
		for (int i = 0; i < elements.length; i++) {
			memento.elements[i] = elements[i].getValue();
		}
		return memento;
	}

	/**
	 * Memento for JSON serialization.
	 */
	static class Memento {
		@JsonProperty("zpSubgroup")
		public ZpSubgroup zpSubgroup;

		@JsonProperty("elements")
		public BigInteger[] elements;
	}
}
