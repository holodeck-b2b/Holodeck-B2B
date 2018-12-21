/**
 * 
 */
package org.holodeckb2b.interfaces.events;

import org.holodeckb2b.interfaces.eventprocessing.IMessageProcessingEvent;

/**
 * Is a generic <i>message processing event</i> to indicate that a problem occurred during the processing of an outgoing 
 * message unit. All events that indicate failures in processing of the message extend this interface, so it can be used 
 * as a generic filter when configuring event handling. This event however is also implemented by the Core to inform the 
 * back-end or extensions about errors that occur during processing of the message and for which no specific event is 
 * defined. 
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  HB2B_NEXT_VERSION
 */
public interface ISendMessageProcessingFailure extends IMessageProcessingEvent {

}
