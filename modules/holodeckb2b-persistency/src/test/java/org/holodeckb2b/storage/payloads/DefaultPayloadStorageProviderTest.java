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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.apache.axis2.description.Parameter;
import org.apache.commons.io.FileUtils;
import org.holodeckb2b.common.testhelpers.HolodeckB2BTestCore;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.core.config.InternalConfiguration;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.storage.IPayloadContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultPayloadStorageProviderTest {

	@BeforeAll
	static void setup() throws IOException {
		HolodeckB2BCoreInterface.setImplementation(new HolodeckB2BTestCore());
		InternalConfiguration config = (InternalConfiguration) HolodeckB2BCoreInterface.getConfiguration();
		config.setTempDirectory(TestUtils.getTestClassBasePath());
	}

	@BeforeEach
	void cleanup() throws IOException {
		InternalConfiguration config = (InternalConfiguration) HolodeckB2BCoreInterface.getConfiguration();
		Parameter custdirparam = config.getParameter("payload-directory");
		if (custdirparam != null)
			config.removeParameter(custdirparam);
		FileUtils.cleanDirectory(TestUtils.getTestClassBasePath().toFile());
	}

	@Test
	void testUseDefaultDir() {
		final DefaultPayloadStorageProvider provider = new DefaultPayloadStorageProvider();
		assertDoesNotThrow(() -> provider.init(HolodeckB2BCoreInterface.getConfiguration()));
		assertTrue(Files.isDirectory(TestUtils.getTestResource("pldata")));
	}

	@Test
	void testUseCustomDir() throws IOException {
		Path custdir = TestUtils.getTestResource("custom-pldata");
		Files.createDirectory(custdir);
		InternalConfiguration config = (InternalConfiguration) HolodeckB2BCoreInterface.getConfiguration();
		config.addParameter("payload-directory", custdir.toString());

		final DefaultPayloadStorageProvider provider = new DefaultPayloadStorageProvider();
		assertDoesNotThrow(() -> provider.init(config));

		IPayloadContent content = assertDoesNotThrow(() ->
										provider.createNewPayloadStorage(UUID.randomUUID().toString(), null, null));
		assertDoesNotThrow(() -> {
			OutputStream cos = content.openStorage();
			cos.write(TestDataHelper.createRandomData());
			cos.close();
		});

		assertTrue(Files.exists(custdir.resolve(content.getPayloadId())));
	}

	@Test
	void testCreateNewPayloadStorage() {
		final DefaultPayloadStorageProvider provider = new DefaultPayloadStorageProvider();
		assertDoesNotThrow(() -> provider.init(HolodeckB2BCoreInterface.getConfiguration()));

		final String payloadId = UUID.randomUUID().toString();

		IPayloadContent content = assertDoesNotThrow(() -> provider.createNewPayloadStorage(payloadId, null, null));

		assertDoesNotThrow(() -> {
			OutputStream cos = content.openStorage();
			cos.write(TestDataHelper.createRandomData());
			cos.close();
		});

		assertTrue(Files.exists(TestUtils.getTestResource("pldata").resolve(payloadId)));
	}

	@Test
	void testGetPayloadContent() {
		final DefaultPayloadStorageProvider provider = new DefaultPayloadStorageProvider();
		assertDoesNotThrow(() -> provider.init(HolodeckB2BCoreInterface.getConfiguration()));

		File testfile = assertDoesNotThrow(() -> TestDataHelper.createTestFile(TestUtils.getTestResource("pldata")));

		IPayloadContent content = assertDoesNotThrow(() -> provider.getPayloadContent(testfile.getName()));

		assertDoesNotThrow(() -> content.getContent());
	}

	@Test
	void testRemovePayloadContent() {
		final DefaultPayloadStorageProvider provider = new DefaultPayloadStorageProvider();
		assertDoesNotThrow(() -> provider.init(HolodeckB2BCoreInterface.getConfiguration()));

		File testfile = assertDoesNotThrow(() -> TestDataHelper.createTestFile(TestUtils.getTestResource("pldata")));

		assertDoesNotThrow(() -> provider.removePayloadContent(testfile.getName()));

		assertDoesNotThrow(() -> provider.removePayloadContent(UUID.randomUUID().toString()));
	}
}
