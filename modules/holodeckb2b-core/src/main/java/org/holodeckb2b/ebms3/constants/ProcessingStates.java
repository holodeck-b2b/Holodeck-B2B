/*
 * Copyright (C) 2013 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.ebms3.constants;

/**
 * Constants for describing the processing state of a message.
 * 
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public final class ProcessingStates {
    
    /**
     * The message unit has been submitted to the Holodeck B2B core. This processing state
     * only applies to user messages as only these can be submitted to the core. Signal messages
     * are <i>created</i> during message processing and therefor start in {@see #CREATED} state.
     */
    public static final String SUBMITTED = "SUBMITTED";
    
    /**
     * The message unit has been created by Holodeck B2B. This processing state only
     * applies to signal messages as these are created by Holodeck B2B in response to
     * received user messages.
     */
    public static final String CREATED = "CREATED";

    /**
     * The message unit is waiting to be pulled by another MSH. 
     */
    public static final String AWAITING_PULL = "WAITING FOR PULL";
    
    /**
     * The message unit is ready to be pushed to another MSH. Because Holodeck B2B
     * currently first stores messages to the database before starting the send
     * process this is state is introduced.
     */
    public static final String READY_TO_PUSH = "READY TO PUSH";
    
    /**
     * The message unit has just been received by Holodeck B2B and is waiting to
     * be further processed
     */
    public static final String RECEIVED = "RECEIVED";
    
    /**
     * The message unit is currently being processed by Holodeck B2B.
     */
    public static final String PROCESSING = "PROCESSING";
    
    /**
     * The user message is waiting for a receipt
     */
    public static final String AWAITING_RECEIPT = "AWAITING RECEIPT";
    
    /**
     * The message unit is successfully delivered
     */
    public static final String DELIVERED = "DELIVERED";

    /**
     * The signal message unit is successfully processed
     */
    public static final String DONE = "DONE";
    
    /**
     * The signal message unit is processed but an Error with severity <i>warning</i> was reported
     */
    public static final String PROC_WITH_WARNING = "PROCESSED WITH WARNING";
    
    /**
     * The user message is ready to be delivered to the business application
     */
    public static final String READY_FOR_DELIVERY = "READY FOR DELIVERY";
    
    /**
     * The user message is currently being delivered to the business application
     */
    public static final String OUT_FOR_DELIVERY = "OUT FOR DELIVERY";
    
    /**
     * Holodeck B2B has tried to deliver the user message to the business application,
     * but the attempt failed. Now wait for retry or if no retries possible failure.
     */
    public static final String DELIVERY_FAILED = "DELIVERY FAILED";
   
    /**
     * A problem occurred when the message unit was sent out.  
     */
    public static final String TRANSPORT_FAILURE = "TRANSPORT FAILURE";
    
    /**
     * Processing of the message unit failed
     */
    public static final String FAILURE = "FAILURE";
    
    /**
     * The message unit is a duplicate of an already delivered message unit. 
     */
    public static final String DUPLICATE = "DUPLICATE";
    
    // This class should not be instantiated!
    private ProcessingStates() {}
}
