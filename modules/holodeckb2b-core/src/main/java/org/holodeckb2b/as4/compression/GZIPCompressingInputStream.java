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

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

/**
 * Is an {@link InputStream} implementation with on the fly GZIP compression.
 * <p>It uses the compression of the {@link DeflaterInputStream} and adds the GZIP header and trailer.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class GZIPCompressingInputStream extends DeflaterInputStream {

    // GZIP header magic number.
    private final static int GZIP_MAGIC = 0x8b1f;

    // The GZIP header
    private final static byte[] GZIP_HEADER = {
        (byte) GZIP_MAGIC, // Magic number (short)
        (byte) (GZIP_MAGIC >> 8), // Magic number (short)
        Deflater.DEFLATED, // Compression method (CM)
        0, // Flags (FLG)
        0, // Modification time MTIME (int)
        0, // Modification time MTIME (int)
        0, // Modification time MTIME (int)
        0, // Modification time MTIME (int)
        0, // Extra flags (XFLG)
        0 // Operating system (OS)
    };
    
    // The GZIP trailer (empty when starting)
    private byte[] GZIP_TRAILER = new byte[8];

    // Enumeration that indicates the parts of the GZIP
    enum Part {
        HEADER, BODY, TRAILER
    }

    // Indicator of which part we are now processing
    private Part part = null;
    
    // Position within part (only applicable for header and trailer)
    private int position = 0;

    /**
     * Creates a new {@link GZIPCompressingInputStream} from an uncompressed {@link InputStream}.
     *
     * @param in The uncompressed {@link InputStream}.
     */
    public GZIPCompressingInputStream(InputStream in) {
        super(new CheckedInputStream(in, new CRC32()), new Deflater(Deflater.DEFAULT_COMPRESSION, true));
        part = Part.HEADER;
    }

    /**
     * Reads compressed data into a byte array. This method uses {@link DeflaterInputStream#read(byte[], int, int)} to
     * do the actual compression. Before it starts with the compressed data it returns the GZIP header and after the
     * all compressed data is read the GZIP trialer is returned. 
     * 
     * @param b     buffer into which the data is read  
     * @param off   starting offset of the data within b
     * @param len   maximum number of compressed bytes to read into b
     * @return      the actual number of bytes read, or -1 if the end of the uncompressed input stream is reached
     * @throws IOException  if an I/O error occurs or if this input stream is already closed
     */
    @Override
    public int read(byte b[], int off, int len) throws IOException {
        // The number of bytes read 
        int count = 0;
        
        if (part == Part.HEADER) {
            // Read bytes from the header
            count = Math.min(len, GZIP_HEADER.length - position);
            System.arraycopy(GZIP_HEADER, position, b, off, count);
                        
            // Advance the position as "count" bytes have already been read.
            position += count;
            if (position == GZIP_HEADER.length)
                // We have read the complete header, moving to next part
                part = Part.BODY;
        }
        
        if (part == Part.BODY && count < len) {
            // Read bytes of compressed data if there is still room in the buffer
            int i = len - count;
            int r = super.read(b, off + count, i);
            if (r < i)  {
                // Nothing read from body part, moving to trailer
                createTrailer();
                part = Part.TRAILER;
                position = 0;
            }
            if (r >= 0)
                count += r; // increase counter of read bytes
        }

        if (part == Part.TRAILER && count < len) {
            // Read bytes from the trailer if there still is space
            int i = Math.min(len - count, GZIP_TRAILER.length - position);
            if (i > 0) {
                System.arraycopy(GZIP_TRAILER, position, b, off + count, i);
                // Advance the position as "count" bytes have already been read.
                position += i;
                // And also increase counter of number of bytes read
                count += i;
            } 
        }
        
        // If we did not read anything we should return -1
        return (count > 0 ? count : -1) ;
    }

    /**
     * Create the GZIP trailer for the currently read and compressed data
     *     
     * @throws IOException If an I/O error is produced.
     */
    private void createTrailer() throws IOException {
        writeInt((int) ((CheckedInputStream) this.in).getChecksum().getValue(), GZIP_TRAILER, 0); // CRC-32 of uncompr. data
        writeInt(def.getTotalIn(), GZIP_TRAILER, 4); // Number of uncompr. bytes        
    }

    /**
     * Writes an integer in Intel byte order to a byte array, starting at a given offset.
     *     
     * @param i         The integer to write.
     * @param buf       The byte array to write the integer to.
     * @param offset    The offset from which to start writing.
     * @throws IOException If an I/O error is produced.
     */
    private void writeInt(int i, byte[] buf, int offset) throws IOException {
        writeShort(i & 0xffff, buf, offset);
        writeShort((i >> 16) & 0xffff, buf, offset + 2);
    }

    /**
     * Writes a short integer in Intel byte order to a byte array, starting at a given offset.
     *     
     * @param s         The short to write.
     * @param buf       The byte array to write the integer to.
     * @param offset    The offset from which to start writing.
     * @throws IOException If an I/O error is produced.
     */
    private void writeShort(int s, byte[] buf, int offset) throws IOException {
        buf[offset] = (byte) (s & 0xff);
        buf[offset + 1] = (byte) ((s >> 8) & 0xff);
    }
}
