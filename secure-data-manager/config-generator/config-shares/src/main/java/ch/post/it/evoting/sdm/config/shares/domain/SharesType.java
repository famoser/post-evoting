/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.sdm.config.shares.domain;

public enum SharesType {

	ADMIN_BOARD("admin"),
	ELECTORAL_BOARD("electoral");

	private final String type;

	SharesType(final String type) {
		this.type = type;
	}

	public static SharesType fromString(final String type) {
		if (type == null || "".equals(type)) {
			throw new IllegalArgumentException("Type can't be empty");
		}
		for (SharesType t : SharesType.values()) {
			if (type.equals(t.getType())) {
				return t;
			}
		}

		throw new IllegalArgumentException("Wrong value for parameter type");
	}

	String getType() {
		return type;
	}
}
