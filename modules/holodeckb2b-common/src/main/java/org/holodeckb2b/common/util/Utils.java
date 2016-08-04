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
package org.holodeckb2b.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.apache.tika.Tika;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.holodeckb2b.common.exceptions.ObjectSerializationException;

/**
 * Is a container for some generic helper methods.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public final class Utils {

    /*
     * Apache Tika mime type detector for detecting the mime type of payloads
     */
    private static final class SingletonHolder {
      static final Tika mimeTypeDetector = new Tika ();
    }  

    /**
     * Transform a {@link Date} object to a {@link String} formatted according to
     * the specification of the <code>dateTime</code> datatype of XML schema.<br>
     * See <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">section 3.2.7 of the XML
     * Specification</a> for details.
     *
     * @param   date  The date as Calendar object to convert to String.
     * @return  The date as an <code>xs:dateTime</code> formatted String
     *          or <code>null</code> when date object was <code>null</code>
     */
    public static String toXMLDateTime(final Date date) {
        if (date == null)
            return null;

        final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }

    /**
     * Parses a {@link String} for XML dateTime (see <a href="http://www.w3.org/TR/xmlschema-2/#dateTime">section 3.2.7
     * of the XML Specification</a>) and return a {@link Date} object when a valid date is found.
     *
     * @param   xmlDateTimeString   The String that should contain the <code>xs:dateTime</code> formatted date
     * @return  A {@link Date} object for the parsed date
     * @throws  ParseException on date time parsing error
     */
    public static Date fromXMLDateTime(final String xmlDateTimeString)
            throws ParseException {
        String s = xmlDateTimeString;
        String f = null;

        // If there is no text, there is no date
        if (s == null || s.isEmpty())
            return null;

        // Check whether UTC is specified as timezone using "Z" and replace with "+00:00" if yes
        if (s.indexOf("Z") > 0)
            s = s.replace("Z", "+00:00");

        // Check if value contains fractional seconds
        int i = s.indexOf(".");
        if (i > 0) {
            // Contains fractional seconds, limit to milliseconds (3 digits)
            // Get start of timezone which is indicated by either "+" or "-"
            // Because "-" also occurs in the date part, only start looking for it from start of the time part
            int z = Math.max(s.indexOf("+"), s.indexOf("-", s.indexOf("T")));
            z = (z == -1 ? s.length() : z); // It's possible that no timezone was included, then fractional seconds are last part of string
            // Limit the number of digits to extract to 3 but use less if no more available
            final int S = Math.min(z-i-1, 3);
            s = s.substring(0, i + S + 1) + s.substring(z);
            // Remove ':' from timezone
            i = s.indexOf(":", i + S + 1);
            s = s.substring(0, i) + s.substring(i+1);
            // Set format
            f = "yyyy-MM-dd'T'HH:mm:ss." + "SSS".substring(0, S)  +"Z";
        } else {
            // No fractional seconds, just remove the ':' from the timezone indication (when it is there)
            if (s.length() > 22 ) {
                s = s.substring(0, 22) + s.substring(23);
                // Set format
                f = "yyyy-MM-dd'T'HH:mm:ssZ";
            } else {
                // Only set format
                f = "yyyy-MM-dd'T'HH:mm:ss";
            }
        }

        return new SimpleDateFormat(f).parse(s);
    }

    /**
     * Determines the mime type of the given file. It can not be guaranteed
     * that this method will return the correct mime type. Therefor it is
     * RECOMMENDED that the producer of the file supplies the [correct] mime type
     * together with it.
     * <p>Current implementation is based on <i>Apache Tika</i> which scans
     * file contents to detect mime type.
     *
     * @param   f   The file to determine the mime type for
     * @return      The detected mime type for the given file
     * @throws  IOException When the given file can not be accessed for mime type detection
     */
    public static String detectMimeType(final File f) throws IOException {
        return SingletonHolder.mimeTypeDetector.detect(f).toString();
    }

    /**
     * Determines extension to use for the given mime type.
     * <p>Current implementation is based on <i>Apache Tika</i>. If the given mime type is not recognized no extension
     * will be returned
     *
     * @param   mimeType   The mime type to get the extensio for
     * @return             The default extension for the given mime type<br>or <code>null</code> when the mime type is
     *                     not recognized
     */
    public static String getExtension(final String mimeType) {
        if (mimeType == null || mimeType.isEmpty())
            return null;

        final MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
        MimeType  tikaMimeType = null;
        try {
            tikaMimeType = allTypes.forName(mimeType);
        } catch (final MimeTypeException ex) {
            // Can not detect the mime type of the given file, so no extension
            return null;
        }

        return tikaMimeType.getExtension();
    }

    /**
     * Serializes an object to an array of bytes.
     *
     * @param obj   The object to serialize
     * @return      The serialized object as an array of bytes
     * @throws ObjectSerializationException    When an error occurs during the
     *                                          serialization process
     */
    public static byte[] serialize(final Object obj) throws ObjectSerializationException {
        // No object results is no bytes
        if (obj == null)
            return null;

        try (final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final ObjectOutput out = new ObjectOutputStream(bos))
        {
            out.writeObject(obj);
            out.close();
            return bos.toByteArray();
        } catch (final Exception ex) {
            throw new ObjectSerializationException("Object [" + obj.getClass().getName() + "/" + obj.hashCode() + "] could not be serialized", ex);
        }
    }

    /**
     * Deserializes an object from an array of bytes.
     *
     * @param data      The array of bytes that represents the serialized object
     * @return          The deserialized object when it could be read succesfully
     * @throws ObjectSerializationException When an error occurs during the
     *                                      deserialization process
     */
    public static Object deserialize(final byte[] data) throws ObjectSerializationException {
        // If there are no bytes, there is no object
        if (data == null || data.length <= 0)
            return null;

        Object result = null;
        try (final ByteArrayInputStream stream = new ByteArrayInputStream(data);
             final ObjectInputStream is = new ObjectInputStream(stream)) 
        {
            result = is.readObject();
        } catch (final Exception ex) {
            throw new ObjectSerializationException("Deserializing an object failed!", ex);
        }

        return result;
    }

    /**
     * Ensures that the given file name will not conflict with an existing file. If there exists a file with the given
     * name a numerical suffix will be added to the name until no duplicate exists.
     *
     * @param baseName      The file name to check for possible duplicates.
     * @return              The base name added with a numerical suffix if necessary to prevent duplicates
     * @throws IOException  When a parent directory does not exist (anymore)
     */
    public static String preventDuplicateFileName(final String baseName) throws IOException {
        if (baseName == null || baseName.isEmpty())
            return null;

        // Split the given path into name and extension part (if possible)
        String nameOnly = baseName;
        String ext = "";
        final int startExt = baseName.lastIndexOf(".");
        if (startExt > 0) {
            nameOnly = baseName.substring(0, startExt);
            ext = baseName.substring(startExt);
        }

        Path targetPath = Paths.get(baseName);
        File f = null; int i = 1;
        while (f == null) {
            try {
                f = Files.createFile(targetPath).toFile();
            } catch (final FileAlreadyExistsException faee) {
                // Okay, the file already exists, try with increased sequence number
                targetPath = Paths.get(nameOnly + "-" + i++ + ext);
            }
        }

        return targetPath.toString();
    }

    /**
     * Sorts an array of files so that the filenames are in alphabetical order. The sort operational is done in the
     * array itself so there is no return value.
     *
     * @param array The array to be sorted.
     */
    public static void sortFiles(final File array[]) {
        if (array != null && array.length > 1)
            Arrays.sort (array, new Comparator <File>(){
                public int compare (File aO1, File aO2) {
                    return aO1.getName ().compareTo (aO2.getName ());
                }
            });
    }

    /**
     * Get the key from a HashMap by value rather then by key.
     * The HashMap must contain a one-to-one relationship. If not it simply
     * returns the first item found.
     * Note: we don't use a BiMap with a one-to-one relation here.
     * @param map The HashMap containing key and value pairs.
     * @param value The value to search the corresponding key value for.
     * @return The key value corresponding to the provided value or null if
     * nothing is found
     */
    public static String getKeyByValue(final Map<String, String> map, final String value) {
        for (final Entry<String, String> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Compares two strings
     *
     * @param s     The first input string
     * @param p     The second input string
     * @return      -2 when both strings are non-empty and their values are different,<br>
     *              -1 when both strings are empty,<br>
     *              0  when both strings are non-empty but equal,<br>
     *              1  when only the first string is non-empty,<br>
     *              2  when only the second string is non-empty
     */
    public static int compareStrings(final String s, final String p) {
        if (s == null || s.isEmpty()) {
            if (p != null && !p.isEmpty()) {
                return 2;
            } else {
                return -1;
            }
        } else if (p != null) {
            if (s.equals(p)) {
                return 0;
            } else {
                return -2;
            }
        } else {
            return 1;
        }
    }

    /**
     * Check whether the given String is non-empty and returns its value if true, otherwise the supplied default will
     * be returned.
     *
     * @param value         The String to check
     * @param defaultValue  The default value to use if the given string is <code>null</code> or empty
     * @return      <code>value</code> if it is a non-empty string,<br>
     *              <code>defaultValue</code> otherwise
     */
    public static String getValue(final String value, final String defaultValue) {
        return (value != null && !value.isEmpty() ? value : defaultValue);
    }

    /**
     * Checks whether the given String is <code>null</code> or is an empty string, i.e. does not contain any other
     * characters then whitespace.
     *
     * @param s     The string to check
     * @return      <code>true</code> if <code>s == null || s.trim().isEmpty() == true</code>,<br>
     *              <code>false</code> otherwise
     */
    public static boolean isNullOrEmpty(final String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * Checks whether the given Collection is <code>null</code> or is empty.
     *
     * @param c     The Collection to check
     * @return      <code>true</code> if <code>c == null || c.isEmpty() == true</code>,<br>
     *              <code>false</code> otherwise
     */
    public static boolean isNullOrEmpty(final Collection<?> c) {
        return c == null || c.isEmpty();
    }

    /**
     * Checks whether the given Map is <code>null</code> or is empty.
     *
     * @param m     The Map to check
     * @return      <code>true</code> if <code>m == null || m.isEmpty() == true</code>,<br>
     *              <code>false</code> otherwise
     */
    public static boolean isNullOrEmpty(final Map<?,?> m) {
        return m == null || m.isEmpty();
    }

    /**
     * Checks whether the given Iterator is <code>null</code> or does not contain any more objects.
     *
     * @param i     The Iterator to check
     * @return      <code>true</code> if <code>i == null || i.hasNext() == true</code>,<br>
     *              <code>false</code> otherwise
     */
    public static boolean isNullOrEmpty(final Iterator<?> i) {
        return i == null || !i.hasNext();
    }

    /**
     * Gets the root cause of the exception by traversing the exception stack and returning the
     * last available exception in it.
     *
     * @param t     The {@link Throwable} object to get the root cause for. May not be <code>null</code>
     *              because otherwise it crashes with an ArrayIndexOutOfBoundsException
     * @return      The root cause (note that this can be the throwable itself)
     */
    public static Throwable getRootCause(final Throwable t) {
        final List<Throwable> exceptionStack = getCauses(t);
        return exceptionStack.get(exceptionStack.size() - 1);
    }

    /**
     * Gets the exception stack of an exception, i.e. the list of all exception that where registered as causes.
     *
     * @param t     The {@link Throwable} object to get the exception stack for
     * @return      A list of {@link Throwable} object with the first item being the exception itself and the last
     *              item the root cause.
     */
    public static List<Throwable> getCauses(final Throwable t) {
        final List<Throwable> exceptionStack = new ArrayList<>();
        Throwable i = t;
        while (i != null) {
            exceptionStack.add(i);
            i = i.getCause();
        }

        return exceptionStack;
    }

    /**
     * Compare any 2 objects in a <code>null</code> safe manner. If both passed
     * objects are <code>null</code> they are interpreted as being equal. If only
     * one object is <code>null</code> they are different. If both objects are
     * non-<code>null</code> than the {@link #equals(Object)} method is invoked on
     * them.
     * 
     * @param o1
     *        First object. May be <code>null</code>.
     * @param o2
     *        Second object. May be <code>null</code>.
     * @return <code>true</code> if both are <code>null</code> or if both are
     *         equal.
     */
    public static <T> boolean nullSafeEqual (final T o1, final T o2) {
        return o1 == null ? o2 == null : o1.equals (o2);
    }

    /**
     * Compare any 2 {@link String}s in a <code>null</code> safe manner. If both passed
     * objects are <code>null</code> they are interpreted as being equal. If only
     * one object is <code>null</code> they are different. If both objects are
     * non-<code>null</code> than the {@link String#equalsIgnoreCase(String)} method is invoked on
     * them.
     * 
     * @param s1
     *        First String. May be <code>null</code>.
     * @param s2
     *        Second String. May be <code>null</code>.
     * @return <code>true</code> if both are <code>null</code> or if both are
     *         equal ignoring the case.
     */
    public static boolean nullSafeEqualIgnoreCase (final String s1, final String s2) {
        return s1 == null ? s2 == null : s1.equalsIgnoreCase (s2);
    }
}
