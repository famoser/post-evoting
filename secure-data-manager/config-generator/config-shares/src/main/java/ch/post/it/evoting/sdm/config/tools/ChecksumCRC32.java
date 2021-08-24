/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

package ch.post.it.evoting.sdm.config.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChecksumCRC32 {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChecksumCRC32.class);

	private ChecksumCRC32() {
	}

	public static boolean doChecksum(final File fileToCheck, final long expectedSize, final long expectedChecksum) {
		try (FileInputStream fis = new FileInputStream(fileToCheck); CheckedInputStream cis = new CheckedInputStream(fis, new CRC32())) {

			if (expectedSize != fileToCheck.length()) {
				return false;
			}
			byte[] buf = new byte[4096];
			int readBytes;
			while ((readBytes = cis.read(buf)) >= 0) {
				LOGGER.trace("Read {} bytes", readBytes);
			}
			return expectedChecksum == cis.getChecksum().getValue();
		} catch (IOException ioe) {
			LOGGER.info("Error calculating crc32 of {}", fileToCheck.getAbsolutePath(), ioe);
			return false;
		}
	}

}
