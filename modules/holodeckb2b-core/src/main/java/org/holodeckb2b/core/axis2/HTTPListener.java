/*
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.core.axis2;

/**
 * Is a proxy to the {@link org.holodeckb2b.core.transport.http.HTTPListener} for backward compatibility with
 * previous version 8 of Holodeck B2B.
 *
 * @author Sander Fieten (sander at chasquis-consulting.com)
 * @since 5.0.0
 * @deprecated since 8.2.0. Use {@link org.holodeckb2b.core.transport.http.HTTPListener} instead
 */
@Deprecated
public class HTTPListener extends org.holodeckb2b.core.transport.http.HTTPListener {

}
