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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.activation.DataHandler;

/**
 * Is an implementation of {@link DataHandler} and uses the <i>decorator</i> pattern to add GZip functionality to the
 * contained <code>DataHandler</code> object.
 * <p>The (de)compression of the data is done when the data is written in the {@link #writeTo(java.io.OutputStream)} 
 * method. The specified MIME Content-Type defines whether the data from the source DataHandler should be compressed
 * or decompressed; when the content type is <i>"application/gzip"</i> it will be compressed, otherwise it will be 
 * decompressed.
 * <p>For decompression of the data the {@link DataHandler#getInputStream()} is used, so the source <code>DataHandler
 * </code> MUST implement this method to ensure correct decompression.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CompressionDataHandler extends DataHandler {
    
    /**
     * The source DataHandler containing the actual data
     * 
     */
    private DataHandler source = null;
    
    /**
     * The resulting data type of the <code>writeTo()</code> method. Determines whether this handler will compress or
     * decompress the data contained in the source.
     */
    private String      resultContentType = null;
    
    /**
     * This constructor should be used to create a facade to a {@link DataHandler} for decompressing the contained data.
     * The specified MIME type is not used by this class itself but only to inform using classes about the expected 
     * content.
     * 
     * @param source    The {@link DataHandler} that contains the compressed data
     * @param mimeType  The MIME type of the decompressed data.
     */
    public CompressionDataHandler(DataHandler source, String mimeType) {
        super(source.getDataSource());
        this.source = source;
        this.resultContentType = mimeType;
    }
   
    /**
     * This constructor should be used to create a facade to a {@link DataHandler} for compressing the contained data.
     * 
     * @param source    The {@link DataHandler} that contains the uncompressed data
     */
    public CompressionDataHandler(DataHandler source) {
        super(source.getDataSource());
        this.source = source;
        this.resultContentType = CompressionFeature.COMPRESSED_CONTENT_TYPE;
    }

    /**
     * Sets the MIME Type that defines the output of the {@link #writeTo(java.io.OutputStream)} method. When this type
     * is equal to "application/gzip" the data from the contained DataHandler will be compressed, otherwise the data
     * will be decompressed.
     * 
     * @param mimeType  The MIME type of the output from this DataHandler
     */
    public void setContentType(String mimeType) {
        this.resultContentType = mimeType;
    }
    
    /**
     * Gets the MIME Type that defines the output of the {@link #writeTo(java.io.OutputStream)} method. When this type
     * is equal to "application/gzip" the data from the contained DataHandler will be compressed, otherwise the data
     * will be decompressed.
     * 
     * @return The MIME type of the output from this DataHandler
     */
    @Override
    public String getContentType() {
        return resultContentType;
    }

    /**
     * Gets the transfer encoding to use for the data contained in this DataHandler. As this is used only when the data
     * is written to the MIME package the encoding is set fixed to "binary"
     * 
     * @return Fixed value "binary"
     */
    public String getTransferEncoding() {
        return "binary";
    }

    /**
     * Writes the data to the given <code>OutputStream</code>. Depending on the set content type the data will be either 
     * compressed (content type equals "application/gzip") or decompressed (all other values).
     * 
     * @param out           The output stream to write the data to
     * @throws IOException  When (de)compressing of the content fails
     */
    @Override
    public void writeTo(OutputStream out) throws IOException {
        if (CompressionFeature.COMPRESSED_CONTENT_TYPE.equalsIgnoreCase(resultContentType))
            compress(out);
        else
            decompress(out);
    }
    
    /**
     * Reads the data from the attachment. Depending on the set content type the data will be either compressed 
     * (content type equals "application/gzip") or decompressed (all other values).
     * 
     * @return An input stream to read the data from the attachment
     * @throws IOException  When (de)compressing of the content fails     
     */
    @Override
    public InputStream getInputStream() throws IOException {
        if (CompressionFeature.COMPRESSED_CONTENT_TYPE.equalsIgnoreCase(resultContentType))
            return new GZIPCompressingInputStream(super.getInputStream());
        else
            return new GZIPInputStream(super.getInputStream());
    }
    
    
    /**
     * Writes the data GZip compressed to the given output stream.
     * 
     * @param out           The {@link OutputStream} to write the compressed data to
     * @throws IOException  When an error occurs while writing the data to the stream
     */
    private void compress(OutputStream out) throws IOException {
        // Writing compressed data is easy, we only need to wrap the given output stream in a GZIPOutputStream to get
        // compression.
        GZIPOutputStream gzOutputStream = new GZIPOutputStream(out);
        source.writeTo(gzOutputStream);
        gzOutputStream.finish();
    }
    
    /**
     * Writes the uncompressed data to the given output stream.
     * 
     * @param out           The {@link OutputStream} to write the uncompressed data to
     * @throws IOException  When an error occurs while writing the data to the stream
     */
    private void decompress(OutputStream out) throws IOException {
        try (GZIPInputStream gzInputStream = new GZIPInputStream(source.getInputStream()))
        {            
            byte[]  buffer = new byte[2048];
            int     r = 0;
            while ((r = gzInputStream.read(buffer)) > 0)
                out.write(buffer, 0, r);
        }
    }
}
