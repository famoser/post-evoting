/*
 * (c) Copyright 2021 Swiss Post Ltd.
 */
package ch.post.it.evoting.votingserver.certificateregistry.services.infrastructure.log;

/**
 * Constants for logging in election information.
 */
public class CertificateRegistryLogConstants {

	/**
	 * Track id - additional information.
	 */
	public static final String INFO_TRACK_ID = "#request_id";

	/**
	 * Error description - additional information .
	 */
	public static final String INFO_ERR_DESC = "#err_desc";

	/**
	 * Error description - additional information - additional information.
	 */
	public static final String CERT_SN = "#cert_sn";

	/**
	 * Non-public constructor
	 */
	private CertificateRegistryLogConstants() {
	}
}
