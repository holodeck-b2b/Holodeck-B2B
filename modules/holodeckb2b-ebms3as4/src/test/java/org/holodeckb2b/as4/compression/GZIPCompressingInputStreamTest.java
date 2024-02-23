/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.as4.compression;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.holodeckb2b.common.testhelpers.HB2BTestUtils;
import org.holodeckb2b.commons.testing.TestUtils;
import org.holodeckb2b.commons.util.Utils;
import org.junit.jupiter.api.Test;


/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class GZIPCompressingInputStreamTest {

    @Test
    void testCompression() {
        final File uncompressed = TestUtils.getTestResource("compression/uncompressed.jpg").toFile();

        //Compress
        byte[] compressed = null;
        try (GZIPCompressingInputStream cis = new GZIPCompressingInputStream(new FileInputStream(uncompressed));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        	Utils.copyStream(cis, baos);
        	compressed = baos.toByteArray();
        } catch (final IOException ex) {
	        fail();
        }

       //Check decompressed content
        try (GZIPInputStream decompressed = new GZIPInputStream(new ByteArrayInputStream(compressed));
        	 FileInputStream fis = new FileInputStream(uncompressed)) {
        	HB2BTestUtils.assertEqual(fis, decompressed);
        } catch (IOException e) {
        	fail();
		}
    }
}
