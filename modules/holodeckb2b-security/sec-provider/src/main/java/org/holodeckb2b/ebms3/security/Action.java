/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.security;

import org.holodeckb2b.security.trust.DefaultCertManager;

/**
 * Enumerates the actions that the security provider can be perform on a message. This is used to indicate which access
 * to is required to the keystores of the {@link DefaultCertManager} and to signal during which action the processing
 * failed.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public enum Action {
    SIGN, ENCRYPT, VERIFY, DECRYPT, USERNAME_TOKEN
}
