/**
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
package org.holodeckb2b.ui.app.cli;

import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.common.VersionInfo;

/**
 * Utility class for parsing the command line arguments.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class CommandLineArguments {
	/**
	 * Represents the options that can be supplied for a command. Consists of the command line flag, an indication
	 * whether the option is required (not really an option then ;-) ) and a short description used when printing help
	 * instruction.
	 */
	enum Option {
		PORT("-p", false, "The RMI port used by the Holodeck B2B instance"),
		PMODE_ID("-id", true , "The identifier of the P-Mode to print"),
		FORMAT("-format", false , "The level of details that should be included (simple [default] or detailed)"),
		CERT_ALIAS("-alias", true , "The alias of the certificate to print"),
		CERT_TYPE("-type", true , "The type of the certificate to print (private, partner or trusted)"),
		MESSAGE_ID("-messageId", true , "The MessageId of the message unit"),
		FROM("-from", false, "The time stamp from which to start listing the message units. Default current time"),
		MAX("-max", false, "Max number of message units to list. Default 10"),
		// Since 9.0.0
		CORE_ID("-coredId", true, "The CoreId of the message unit");

		String flag, description;
		boolean isRequired;

		Option(String flag, boolean required, String description) {
			this.flag = flag;
			this.isRequired = required;
			this.description = description;
		}
	}

	/**
	 * Enumeration of all actions that can be used as argument when invoking the app. Each command contains the name
	 * of the command, the list of available options and a short description used for printing a help instruction.
	 */
	enum Action {
		LIST_PMODES("listPModes", new Option[] { Option.PORT}, "Lists all loaded P-Modes"),
		PRINT_PMODE("printPMode", new Option[] { Option.PORT, Option.PMODE_ID } , "Prints the details of P-Mode with specified id"),
		LIST_CERTS("listCerts", new Option[] { Option.PORT, Option.CERT_TYPE, Option.FORMAT }, "Lists all certificates of a specific type"),
		PRINT_CERT("printCert", new Option[] { Option.PORT, Option.CERT_ALIAS, Option.CERT_TYPE } , "Prints the details of  a certificate"),
		MSG_STATUS("msgStatus", new Option[] { Option.PORT, Option.MESSAGE_ID } , "Gets the current processing state of a message unit"),
		STATUS_LIST("statusList", new Option[] { Option.PORT, Option.MESSAGE_ID } , "Lists of processing states a message unit was and is in"),
		HISTORY("history", new Option[] { Option.PORT, Option.FROM, Option.MAX } , "Provides overview of message units in descending order of latest proc state's start time"),
		// Since 9.0.0
		RESUME("resume", new Option[] { Option.PORT, Option.CORE_ID } , "Resume processing of a suspended outgoing User Message"),
		RESEND("resend", new Option[] { Option.PORT, Option.CORE_ID } , "Resend a User Message"),
		REDELIVER("redeliver", new Option[] { Option.PORT, Option.CORE_ID } , "Redeliver a receiver message unit to the back-end");

		String   name;
		Option[] options;
		String   description;

		Action(String name, Option[] options, String description) {
			this.name = name;
			this.options = options;
			this.description = description;
		}
	}


	/**
	 * The requested action to execute and its parameter
	 */
	private Action				 curAction;
	private Map<Option, String>  parameters;

	/**
	 * Parses the arguments provided to the current invocation of the application.
	 *
	 * @param clArgs	The arguments provided on invocation
	 * @throws IllegalArgumentException  When a required option is not provided or when an unknown argument is provided
	 */
	public CommandLineArguments(String[] clArgs) throws IllegalArgumentException {
		if (clArgs.length == 0)
			throw new IllegalArgumentException("No action specified");

		for (Action action : Action.values()) {
			if (action.name.equals(clArgs[0]))
				curAction = action;
		}
		if (curAction == null)
			throw new IllegalArgumentException(clArgs[0] + " not recognized as valid action.");

		// Parse the remainder of command line to find parameters
		parameters = new HashMap<>(curAction.options.length);
		for (int i = 1; i < clArgs.length - 1; i++) {
			Option o = null;
			for (int j = 0; j < curAction.options.length && o == null; j++)
				if (curAction.options[j].flag.equals(clArgs[i]))
					// Found a valid option of this command
					o = curAction.options[j];
			if (o != null)
				parameters.put(o, clArgs[++i]);
		}
		// Now check that all required options were provided
		boolean arp = true;
		for (int i = 0; i < curAction.options.length && arp; i++)
			arp &= !curAction.options[i].isRequired || parameters.containsKey(curAction.options[i]);
		if (!arp)
			throw new IllegalArgumentException("Not all required options for action " + curAction.name + "provided!");
	}

	/**
	 * Gets the action that was specified as argument when invoking the application.
	 *
	 * @return The requested action to execute
	 */
	public Action getAction() {
		return curAction;
	}

	/**
	 * Gets the value for the action's parameter as specified on the command line when invoking the application.
	 *
	 * @param  opt	The parameter's flag which value to get, expressed as {@link Option}
	 * @return The value for the action's parameter
	 */
	public String getParameter(final Option opt) {
		return parameters.get(opt);
	}

	/**
	 * Prints a simple "usage" message to the console.
	 */
	public static void printUsage() {
		System.out.println("Holodeck B2B " + VersionInfo.fullVersion + " - Monitoring Tool");
		System.out.println();
		System.out.println("Usage: nc1701a <action> [options...]");
		System.out.println();
		System.out.println("Available actions:");
		for (Action a : Action.values())
			System.out.printf("\t%-20s %s%n", a.name, a.description);
		System.out.println();
		System.out.println("Available options for actions:");
		for (Action a : Action.values()) {
			System.out.println();
			System.out.println(a.name);
			for (Option o : a.options)
				System.out.printf("\t%-20s %s%n", !o.isRequired ? "[" + o.flag + "]" : o.flag, o.description);
		}
	}

}


