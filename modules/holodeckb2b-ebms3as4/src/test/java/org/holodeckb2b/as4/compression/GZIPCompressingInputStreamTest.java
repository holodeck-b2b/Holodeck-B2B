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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import org.holodeckb2b.common.testhelpers.TestUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class GZIPCompressingInputStreamTest {

    public GZIPCompressingInputStreamTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        try {
            final File out = new File(TestUtils.getPath("compression/compressed.gz"));
            if (out.exists())
                out.delete();
        } catch (final Exception e)
        {}
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testCompression() {
        final File comF = new File(TestUtils.getPath("compression/") + "compressed.gz");
        final File decF = new File(TestUtils.getPath("compression/") + "decompressed.jpg");

        try {
            final File uncF = new File(TestUtils.getPath("compression/uncompressed.jpg"));
            final byte[] buffer = new byte[512];

            //Compress
            try (GZIPCompressingInputStream cfis = new GZIPCompressingInputStream(new FileInputStream(uncF));
                 FileOutputStream fos = new FileOutputStream(comF))
            {
              int r = 0;
              while ( (r = cfis.read(buffer, 0, 512)) > 0 ) {
                  fos.write(buffer, 0, r);
              }
            }

            // Decompress
            try (GZIPInputStream decfis = new GZIPInputStream(new FileInputStream(comF));
                 FileOutputStream fos = new FileOutputStream(decF))
            {
              int r = 0;
              while ( (r = decfis.read(buffer, 0, 512)) > 0 ) {
                  fos.write(buffer, 0, r);
              }
            }

            //Compare
            try (FileInputStream fis1 = new FileInputStream(uncF);
                FileInputStream fis2 = new FileInputStream(decF))
            {
              final byte[] buffer2 = new byte[512];
              int r = 0;
              int r2 = 0;
              while ( ((r = fis1.read(buffer, 0, 512)) > 0) & ((r2 = fis2.read(buffer2, 0, 512)) > 0)) {
                  assertEquals(r, r2);
                  assertArrayEquals(buffer, buffer2);
              }

              assertEquals(r, r2);
            }

        } catch (final IOException ex) {
            Logger.getLogger(GZIPCompressingInputStreamTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        } finally {
            if (comF.exists())
                comF.delete();
            if (decF.exists())
                decF.delete();
        }
    }
}
