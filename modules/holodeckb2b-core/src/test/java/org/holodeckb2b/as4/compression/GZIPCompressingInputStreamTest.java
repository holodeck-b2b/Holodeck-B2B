/*
 * Copyright (C) 2015 The Holodeck B2B Team, Sander Fieten
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
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
            File out = new File(this.getClass().getClassLoader().getResource("compression/compressed.gz").getPath());
            if (out.exists())   
                out.delete();
        } catch (Exception e) 
        {}
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testCompression() {
        GZIPCompressingInputStream cfis = null;
        FileOutputStream fos = null;
        FileInputStream ucf = null;
        GZIPInputStream cf = null;
        
        try {
            File in = new File(this.getClass().getClassLoader().getResource("compression/uncompressed.xml").getPath());
            cfis = new GZIPCompressingInputStream(new FileInputStream(in));
            
            File out = new File(this.getClass().getClassLoader().getResource("compression/").getPath() + "compressed.gz");
            fos = new FileOutputStream(out);
            
            byte[] buffer = new byte[512];
            int r = 0;
            while ( (r = cfis.read(buffer, 0, 512)) > 0 ) {
                fos.write(buffer, 0, r);
            }
            
            ucf = new FileInputStream(in);
            cf = new GZIPInputStream(new FileInputStream(out));
            
            byte[] buffer2 = new byte[512];
            int r2 = 0;
            while ( ((r = cfis.read(buffer, 0, 512)) > 0) && ((r2 = cfis.read(buffer2, 0, 512)) > 0)) {
                assertEquals(r, r2);
                assertArrayEquals(buffer, buffer2);
            }
            
            assertEquals(r, r2);
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GZIPCompressingInputStreamTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        } catch (IOException ex) {
            Logger.getLogger(GZIPCompressingInputStreamTest.class.getName()).log(Level.SEVERE, null, ex);
            fail();
        } finally {
            try {
                cfis.close();
                fos.close();
            } catch (IOException ex) {
                Logger.getLogger(GZIPCompressingInputStreamTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        
    }
}
