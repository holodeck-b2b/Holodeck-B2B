package org.holodeckb2b.common.pmode;

import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.customvalidation.MessageValidationError;
import org.simpleframework.xml.convert.Converter;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.OutputNode;

/**
 * Helper class to convert between the {@link MessageValidationError.Severity} enumeration and the String value in 
 * the XML document.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class ValidationLevelConverter implements Converter<MessageValidationError.Severity> {

	@Override
	public MessageValidationError.Severity read(InputNode node) {
		String value = null;
		try {
			value = node.getValue();
		} catch (Exception e) {				
		}
		MessageValidationError.Severity level;

		if (Utils.isNullOrEmpty(value))
			level = null;
		else
			switch (value.toUpperCase()) {
			case "INFO":
				level = MessageValidationError.Severity.Info; break;
			case "WARN":
				level = MessageValidationError.Severity.Warning; break;
			case "FAILURE":
				level = MessageValidationError.Severity.Failure; break;
			default:
				level = null;
			}
		return level;
	}

	@Override
	public void write(OutputNode node, MessageValidationError.Severity level) {
		String value;
		switch (level) {
		case Info:
			value = "INFO"; break;
		case Warning:
			value = "WARN";	break;
		case Failure:
			value = "FAILURE"; break;
		default:
			value = "NONE";
		}
		node.setValue(value);
	}
}