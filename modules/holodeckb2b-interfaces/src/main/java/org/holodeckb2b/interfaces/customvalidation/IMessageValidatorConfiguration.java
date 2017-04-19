/*
 * Copyright (C) 2017 The Holodeck B2B Team.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.holodeckb2b.interfaces.customvalidation;

import java.util.Map;

/**
 * Defines the interface of the configuration of a single {@link IMessageValidator}.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public interface IMessageValidatorConfiguration {

    /**
     * Returns the id of this validator configuration. This id is used by Holodeck B2B for logging purposes and
     * inclusion in <i>message processing events</i> and may also be used to determine whether it must create and
     * configure a new {@link IMessageValidator.Factory}.
     * <p><b>NOTE: </b>Configurations MUST be uniquely identified as Holodeck B2B will only use the id to check for
     * equivalence.
     *
     * @return  The id of this validator configuration
     */
    String getId();

    /**
     * Gets the class name of the factory class that should be used to create actual instances of the validator.
     *
     * @return  Class name of the {@link IMessageValidator.Factory} implementation to use for creating actual validators
     */
    String getFactory();

    /**
     * Returns the settings that should be used to configure the validators.
     * <p>NOTE: Holodeck B2B will not check these settings and just pass them to the factory class.
     *
     * @return The settings to use for this specific validator configuration
     */
    Map<String, ?> getSettings();
}
