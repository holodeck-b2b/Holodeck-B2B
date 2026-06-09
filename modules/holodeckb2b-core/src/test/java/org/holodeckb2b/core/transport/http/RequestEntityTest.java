/*
 * Copyright (C) 2026 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.transport.http;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.axiom.om.OMOutputFormat;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.kernel.MessageFormatter;
import org.junit.jupiter.api.Test;

class RequestEntityTest {

	@Test
	void testRegular() {
		String message = "Hello World!";

		MessageFormatter mf = mock(MessageFormatter.class);
		when(mf.getContentType(any(), any(), any())).thenReturn("text/plain");
		assertDoesNotThrow(() ->
			doAnswer(i -> {
				OutputStream os = i.getArgument(2);
				os.write(message.getBytes());
				return null;
			}).when(mf).writeTo(any(), any(), any(OutputStream.class), anyBoolean())
		);

		RequestEntity e = assertDoesNotThrow(() ->
							new RequestEntity(new MessageContext(), mf, new OMOutputFormat(), false, false, false));

		assertTrue(e.isRepeatable());
		assertFalse(e.isChunked());
		assertFalse(e.isCompressed());
		assertEquals("text/plain", e.getContentType().getValue());
		assertEquals(message.length(), e.getContentLength());

		byte[] content = assertDoesNotThrow(() -> {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				e.writeTo(baos);
				return baos.toByteArray();
			}});

		assertArrayEquals(message.getBytes(), content);
	}

	@Test
	void testCompressed() {
		String message = "Hello World!";
		byte[] compressed = assertDoesNotThrow(() -> {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
					GZIPOutputStream zos = new GZIPOutputStream(baos)) {
				zos.write(message.getBytes());
				zos.finish();
				return baos.toByteArray();
			}
		});

		MessageFormatter mf = mock(MessageFormatter.class);
		when(mf.getContentType(any(), any(), any())).thenReturn("text/plain");
		assertDoesNotThrow(() ->
			doAnswer(i -> {
				OutputStream os = i.getArgument(2);
				os.write(message.getBytes());
				return null;
			}).when(mf).writeTo(any(), any(), any(OutputStream.class), anyBoolean())
		);

		RequestEntity e = assertDoesNotThrow(() ->
								new RequestEntity(new MessageContext(), mf, new OMOutputFormat(), false, true, false));

		assertTrue(e.isRepeatable());
		assertFalse(e.isChunked());
		assertTrue(e.isCompressed());
		assertEquals("text/plain", e.getContentType().getValue());
		assertEquals(compressed.length, e.getContentLength());

		byte[] content = assertDoesNotThrow(() -> {
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				e.writeTo(baos);
				return baos.toByteArray();
			}});

		assertArrayEquals(compressed, content);
	}

	/*
	 * Because the chunked transfer encoding is done by the Apache Http Components the actual chunking cannot be tested
	 * here and we can only check that the parameter is correctly set and the content length is set to -1.
	 */
	@Test
	void testChunked() {
		MessageFormatter mf = mock(MessageFormatter.class);
		when(mf.getContentType(any(), any(), any())).thenReturn("text/plain");

		RequestEntity e = assertDoesNotThrow(() ->
								new RequestEntity(new MessageContext(), mf, new OMOutputFormat(), true, false, false));

		assertTrue(e.isChunked());
		assertEquals(-1, e.getContentLength());
	}

}
