/*
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.storage.payloads;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.FileUtils;
import org.holodeckb2b.interfaces.storage.StorageException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PayloadContentTest {
	private static final Path TESTDIR = TestUtils.getTestResource("plc-storage");

	@BeforeAll
	static void setupTest() throws IOException {
		if (!Files.exists(TESTDIR))
			Files.createDirectories(TESTDIR);
	}

	@AfterAll
	static void cleanup() throws IOException {
		FileUtils.cleanDirectory(TESTDIR);
		Files.delete(TESTDIR);
	}

	@Test
	void testReading() throws IOException {
		final File testfile = TestDataHelper.createTestFile(TESTDIR);
		final PayloadContent content = new PayloadContent("readtest1", testfile);

		try (InputStream fis = new FileInputStream(testfile);
			 InputStream cis = assertDoesNotThrow(() -> content.getContent())) {
			HB2BTestUtils.assertEqual(fis, cis);
		}
	}

	@Test
	void testParallelReading() throws IOException {
		final File testfile = TestDataHelper.createTestFile(TESTDIR);
		final PayloadContent content = new PayloadContent("readtest2", testfile);

		InputStream cis1 = assertDoesNotThrow(() -> content.getContent());
		byte[] buf1 = new byte[20];
		assertEquals(20, cis1.read(buf1));

		InputStream cis2 = assertDoesNotThrow(() -> content.getContent());
		byte[] buf2 = new byte[20];
		assertEquals(20, cis2.read(buf2));

		assertArrayEquals(buf1, buf2);

		cis1.close(); cis2.close();
	}

	@Test
	void testReadNotFound() {
		assertNull(assertDoesNotThrow(() ->
					new PayloadContent("readtest3", TESTDIR.resolve("doesnotexist").toFile()).getContent()));
	}

	@Test
	void testReadWhileWriting() {
		final File testfile = TESTDIR.resolve(UUID.randomUUID().toString()).toFile();
		final PayloadContent content = new PayloadContent("readtest4", testfile);

		OutputStream cos = assertDoesNotThrow(() -> content.openStorage());

		assertNull(assertDoesNotThrow(() -> content.getContent()));

		assertDoesNotThrow(() -> cos.close());
	}

	@Test
	void testReadAfterWriting() {
		final File testfile = TESTDIR.resolve(UUID.randomUUID().toString()).toFile();
		final PayloadContent content = new PayloadContent("readtest4", testfile);

		try (OutputStream cos = content.openStorage()) {
			for (int i = 0; i < 200; i++)
				cos.write(i);
		} catch (Exception e) {
			fail(e);
		}

		assertDoesNotThrow(() -> content.getContent());
	}

	@Test
	void testWriting() {
		final File testfile = TESTDIR.resolve(UUID.randomUUID().toString()).toFile();
		final PayloadContent content = new PayloadContent("writetest1", testfile);
		final byte[] data = TestDataHelper.createRandomData();

		assertDoesNotThrow(() -> {
			OutputStream cos = content.openStorage();
			cos.write(data);
			cos.close();
		});

		try (InputStream fis = new FileInputStream(testfile);
			 InputStream bais = new ByteArrayInputStream(data)) {
			HB2BTestUtils.assertEqual(fis, bais);
		} catch (IOException e) {
			fail(e);
		}
	}

	@Test
	void resumedWriting() {
		final File testfile = TESTDIR.resolve(UUID.randomUUID().toString()).toFile();
		final PayloadContent content = new PayloadContent("writetest1", testfile);
		final byte[] data = TestDataHelper.createRandomData();

		assertDoesNotThrow(() -> {
			OutputStream cos = content.openStorage();
			cos.write(Arrays.copyOfRange(data, 0, 50));
		});

		assertDoesNotThrow(() -> {
			OutputStream cos = content.openStorage();
			cos.write(Arrays.copyOfRange(data, 50, data.length));
			cos.close();
		});

		try (InputStream fis = new FileInputStream(testfile);
			 InputStream bais = new ByteArrayInputStream(data)) {
			HB2BTestUtils.assertEqual(fis, bais);
		} catch (IOException e) {
			fail(e);
		}
	}

	@Test
	void testRejectWritingClosed() {
		final File testfile = TESTDIR.resolve(UUID.randomUUID().toString()).toFile();
		final PayloadContent content = new PayloadContent("writetest1", testfile);
		final byte[] data = TestDataHelper.createRandomData();

		assertDoesNotThrow(() -> {
			OutputStream cos = content.openStorage();
			cos.write(data);
			cos.close();
		});

		assertThrows(StorageException.class, () -> content.openStorage());
	}

	@Test
	void testRejectWritingExisting() throws IOException {
		final File testfile = TestDataHelper.createTestFile(TESTDIR);
		final PayloadContent content = new PayloadContent("writetest1", testfile);

		assertThrows(StorageException.class, () -> content.openStorage());
	}
}
