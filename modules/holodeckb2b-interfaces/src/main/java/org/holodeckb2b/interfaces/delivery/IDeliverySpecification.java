/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
package org.holodeckb2b.interfaces.delivery;


import java.util.Map;

/**
 * Is used to specify how a message unit should be delivered to the business application.
 * <p>Holodeck B2B itself does not deliver the message unit, the actual delivery is done by a <i>Delivery Method</i>.
 * The delivery specification tells Holodeck B2B how to instantiate the <i>Delivery Method</i> for the integration with
 * a specific back-end application.
 * <p>
 * The delivery specification consists of the {@link IDeliveryMethod} that should be used, the settings needed to 
 * configure it and whether the delivery should be done synchronously in the message processing pipeline or can be done
 * asynchronously. The delivery specification is part of the P-Mode that governs the message exchange and multiple
 * specifications can be specified for the different message unit types, e.g. specific for <i>Receipts</i>.
 * <p>Holodeck B2B will create one {@link IDeliveryMethod} instance for each unique Delivery Specification identifier. 
 * This allows for re-use of <i>Delivery Specifications</i> in P-Modes. How such re-use is implemented is left up to the
 * component(s) responsible for handling the P-Modes in an Holodeck B2B instance.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IDeliveryMethod
 */
public interface IDeliverySpecification {

    /**
     * Returns the id of this delivery specification. This id is used by Holodeck B2B to determine whether it must
     * create and configure a new {@link IDeliveryMethod} instance. If no id is returned Holodeck B2B will always create
     * a new instance.
     * <p><b>NOTE: </b>Configurations should take care of uniquely identifying delivery specifications as Holodeck B2B
     * will only use the id to check for equivalence.
     *
     * @return  The id of this delivery specification or <code>null</code> if this specification has no id.
     */
    String getId();

    /**
     * @deprecated Implementations should implement {@link #getDeliveryMethod()}. This method will be removed in the 
     * 			   next version!
     */
    @Deprecated
    default String getFactory() { return null; }

    /**
     * Gets the class of the {@link IDeliveryMethod} implementation that should be used for the actual message delivery.
     *
     * @return  Class object of the {@link IDeliveryMethod} implementation to use
     * @since 6.0.0
     */
    default Class<? extends IDeliveryMethod> getDeliveryMethod() { return null; }

    /**
     * Returns the settings that should be used to configure the <i>Delivery Method</i> for a specific back-end 
     * integration.
     * <p>Note that Holodeck B2B does not process or check these parameters and just passes them on to the <i>Delivery
     * Method</i> instance. They are not used to check for equivalence of <i>Delivery Specifications</i> and therefore 
     * specifications with the same id must have the same parameters to ensure consistency.  
     *
     * @return The settings to use for this specific instance of the <i>Delivery Method</i>.
     */
    Map<String, ?> getSettings();
    
    /**
     * Indicates whether the delivery of the message units should be performed in parallel with the message processing 
     * pipe line. 
     * <p>
     * When the delivery is done asynchronously Holodeck B2B will already create a <i>Receipt</i> before the message is
     * actually delivered to the back-end. Therefore asynchronous delivery should be combined with a recovery process
     * to handle failed deliveries and ensure information is provided to the back-end / user.
     * <p><b>NOTE: </b>As asynchronous delivery is a feature of a <i>Delivery Method</i> it can only be enabled if the 
     * configured one supports it.    
     *  
     * @return <code>true</code> if the message delivery should be performed asynchronously, <code>false</code> if not
     * @since 6.0.0
     */
    default boolean performAsyncDelivery() { return false; }
}
