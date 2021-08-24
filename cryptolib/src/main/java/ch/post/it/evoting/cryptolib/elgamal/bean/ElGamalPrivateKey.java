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
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.Exponent;
import ch.post.it.evoting.cryptolib.mathematical.groups.impl.ZpSubgroup;

/**
 * Encapsulates an ElGamal private key.
 *
 * <p>Instances of this class contain a list of the exponents corresponding to the key and the Zp
 * subgroup to which these exponents belong.
 */
@JsonRootName("privateKey")
public final class ElGamalPrivateKey extends AbstractJsonSerializable {

	private final Exponent[] exponents;

	private final ZpSubgroup zpSubgroup;

	/**
	 * Constructs an {@link ElGamalPrivateKey} object, using the specified list of exponents and the specified Zp subgroup.
	 *
	 * <p>Note: For performance reasons, a group membership check is not performed for any exponent in
	 * the specified list. Therefore, this membership should be ensured prior to specifying the list as input to this constructor.
	 *
	 * @param exponents  the list of private key exponents.
	 * @param zpSubgroup the Zp subgroup to which the exponents of this private key belong.
	 * @throws GeneralCryptoLibException if the list of private key exponents is null, empty or contains one more null elements, or if the Zp subgroup
	 *                                   is null.
	 */
	public ElGamalPrivateKey(List<Exponent> exponents, ZpSubgroup zpSubgroup) throws GeneralCryptoLibException {
		Validate.notNullOrEmptyAndNoNulls(exponents, "List of ElGamal private key exponents");
		Validate.notNull(zpSubgroup, "Zp subgroup");
		this.exponents = exponents.toArray(new Exponent[0]);
		this.zpSubgroup = zpSubgroup;
	}

	private ElGamalPrivateKey(Exponent[] exponents, ZpSubgroup zpSubgroup) {
		this.exponents = exponents;
		this.zpSubgroup = zpSubgroup;
	}

	/**
	 * Deserializes the instance from a string in JSON format.
	 *
	 * @param json the JSON
	 * @return the instance
	 * @throws GeneralCryptoLibException failed to deserialize the instance.
	 */
	public static ElGamalPrivateKey fromJson(String json) throws GeneralCryptoLibException {
		return AbstractJsonSerializable.fromJson(json, ElGamalPrivateKey.class);
	}

	/**
	 * Deserializes the instance from a given byte array.
	 *
	 * @param bytes the bytes
	 * @return the instance
	 * @throws GeneralCryptoLibException failed to deserialize the instance.
	 */
	public static ElGamalPrivateKey fromBytes(byte[] bytes) throws GeneralCryptoLibException {
		Validate.notNull(bytes, "bytes");
		return Codec.decodePrivateKey(bytes);
	}

	/**
	 * Multiplies given keys. See {@link #multiply(ElGamalPrivateKey)} for details.
	 *
	 * @param key1   the first key
	 * @param key2   the second key
	 * @param others the other keys
	 * @return the "product" key
	 * @throws GeneralCryptoLibException some of the keys is {@code null} or uses a different group.
	 */
	public static ElGamalPrivateKey multiply(ElGamalPrivateKey key1, ElGamalPrivateKey key2, ElGamalPrivateKey... others)
			throws GeneralCryptoLibException {
		ElGamalPrivateKey product = key1.multiply(key2);
		for (ElGamalPrivateKey other : others) {
			product = product.multiply(other);
		}
		return product;
	}

	/**
	 * Multiplies given keys. The specified collection must not be empty. See {@link #multiply(ElGamalPrivateKey)} for details.
	 *
	 * @param keys the keys
	 * @return the "product" key
	 * @throws GeneralCryptoLibException some of the keys is {@code null} or uses a different group.
	 */
	public static ElGamalPrivateKey multiply(Collection<ElGamalPrivateKey> keys) throws GeneralCryptoLibException {
		Iterator<ElGamalPrivateKey> iterator = keys.iterator();
		if (!iterator.hasNext()) {
			throw new GeneralCryptoLibException("Keys are missing.");
		}
		ElGamalPrivateKey product = iterator.next();
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
	static ElGamalPrivateKey fromMemento(Memento memento) throws GeneralCryptoLibException {
		Validate.notNullOrEmptyAndNoNulls(memento.exponents, "List of ElGamal private key exponents");
		Validate.notNull(memento.zpSubgroup, "Zp subgroup");
		BigInteger q = memento.zpSubgroup.getQ();
		Exponent[] exponents = new Exponent[memento.exponents.length];
		for (int i = 0; i < exponents.length; i++) {
			exponents[i] = new Exponent(q, memento.exponents[i]);
		}
		return new ElGamalPrivateKey(exponents, memento.zpSubgroup);
	}

	/**
	 * Retrieves the list of private key exponents. The returned list is read-only.
	 *
	 * @return the list of exponents.
	 */
	public List<Exponent> getKeys() {

		return unmodifiableList(asList(exponents));
	}

	/**
	 * Retrieves the Zp subgroup to which the private key exponents belong.
	 *
	 * @return the Zp subgroup.
	 */
	public ZpSubgroup getGroup() {
		return zpSubgroup;
	}

	/**
	 * "Multiplies" this key by a given one.
	 *
	 * <p>ElGamal private keys with the same number of exponents have a group property, namely they
	 * are members of {@code Zq^n} group, and multiplication is considered as a group operation.
	 *
	 * <p>The exponent number of the returned private key is the minimum of exponent number of the
	 * keys being multiplied.
	 *
	 * @param other the other
	 * @return the "product" key
	 * @throws GeneralCryptoLibException the other key is {@code null} or uses a different group.
	 */
	public ElGamalPrivateKey multiply(ElGamalPrivateKey other) throws GeneralCryptoLibException {
		Validate.notNull(other, "Other key");
		if (!zpSubgroup.equals(other.zpSubgroup)) {
			throw new GeneralCryptoLibException("The other key uses a different group.");
		}
		int length = min(exponents.length, other.exponents.length);
		Exponent[] newExponents = new Exponent[length];
		for (int i = 0; i < newExponents.length; i++) {
			newExponents[i] = exponents[i].add(other.exponents[i]);
		}
		return new ElGamalPrivateKey(newExponents, zpSubgroup);
	}

	/**
	 * "Inverts" this key.
	 *
	 * <p>ElGamal private keys with the same number of exponents have a group property, namely they
	 * are members of {@code Zq^n} group, and inversion is considered as a group operation.
	 *
	 * @return the inverted key.
	 */
	public ElGamalPrivateKey invert() {
		Exponent[] newExponents = new Exponent[exponents.length];
		for (int i = 0; i < newExponents.length; i++) {
			newExponents[i] = exponents[i].negate();
		}
		return new ElGamalPrivateKey(newExponents, zpSubgroup);
	}

	/**
	 * "Divides" this key by a given one.
	 *
	 * <p>ElGamal private keys with the same number of exponents have a group property, namely they
	 * are members of {@code Zq^n} group, and division is considered as a group operation.
	 *
	 * <p>The exponent number of the returned private key is the minimum of exponent number of the
	 * keys being multiplied.
	 *
	 * @param other the other
	 * @return the "quotient" key
	 * @throws GeneralCryptoLibException the other key is {@code null} or uses a different group.
	 */
	public ElGamalPrivateKey divide(ElGamalPrivateKey other) throws GeneralCryptoLibException {
		Validate.notNull(other, "Other key");
		return multiply(other.invert());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(exponents);
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
		ElGamalPrivateKey other = (ElGamalPrivateKey) obj;
		if (!ObjectArrays.constantTimeEquals(exponents, other.exponents)) {
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
		memento.exponents = new BigInteger[exponents.length];
		for (int i = 0; i < exponents.length; i++) {
			memento.exponents[i] = exponents[i].getValue();
		}
		return memento;
	}

	/**
	 * Memento for JSON serialization.
	 */
	static class Memento {
		@JsonProperty("zpSubgroup")
		public ZpSubgroup zpSubgroup;

		@JsonProperty("exponents")
		public BigInteger[] exponents;
	}
}
