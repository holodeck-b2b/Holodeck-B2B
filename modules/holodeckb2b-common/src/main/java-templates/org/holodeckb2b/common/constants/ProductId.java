/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.common.constants;

/**
 * Defines constants that identify the Holodeck B2B product name and version.
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 * @since HB2B_NEXT_VERSION (previously in <code>org.holodeckb2b.ebms3.constants</code>)
 */
public interface ProductId {

    public static final String FULL_NAME = "Holodeck B2B";

    public static final String MAJOR_VERSION = "${hb2b.majorVersion}";
    public static final String MINOR_VERSION = "${hb2b.minorVersion}";
    public static final String PATCH_VERSION = "${hb2b.incrementalVersion}";
}
