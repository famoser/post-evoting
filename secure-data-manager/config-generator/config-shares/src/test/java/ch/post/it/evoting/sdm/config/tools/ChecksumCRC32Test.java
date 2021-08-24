/*
 * (c) Original Developers indicated in attribution.txt, 2021. All Rights Reserved.
 */

package ch.post.it.evoting.sdm.config.tools;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChecksumCRC32Test {

	@TempDir
	static Path tempDir;
	private static File testFile;

	@BeforeAll
	static void prepareTestFile() throws IOException {
		final Path subDir = Files.createDirectory(tempDir.resolve("sub/"));
		testFile = subDir.resolve("toCrc.dat").toFile();
		FileUtils.write(testFile, "test", StandardCharsets.UTF_8);
	}

	@Test
	void fileDoesNotExist() {
		assertFalse(ChecksumCRC32.doChecksum(new File("does not exist"), 0, 0));
	}

	@Test
	void fileIsAFolder() {
		assertFalse(ChecksumCRC32.doChecksum(tempDir.getRoot().toFile(), 0, 0));
	}

	@Test
	void lengthDoesNotMatch() {
		assertFalse(ChecksumCRC32.doChecksum(testFile, 100, 0));
	}

	@Test
	void crcDoesNotMatch() {
		assertFalse(ChecksumCRC32.doChecksum(testFile, 4, 0));
	}

	@Test
	void shouldReturnFalseWhenIOException() {
		File t = new File("nonExistingFile");
		assertFalse(ChecksumCRC32.doChecksum(t, 4, 3632233996L));
	}

	@Test
	void allMatch() {
		assertTrue(ChecksumCRC32.doChecksum(testFile, 4, 3632233996L));
	}
}
