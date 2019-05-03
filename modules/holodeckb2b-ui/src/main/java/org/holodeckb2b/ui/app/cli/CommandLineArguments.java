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

/**
 * Utility class for parsing the command line arguments.
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
 */
public class CommandLineArguments {
	/**
	 * Enumeration of all actions that can be used as argument when invoking the app
	 */
	enum Action {
		LIST_PMODES("listPModes", false, "Lists all loaded P-Modes"),
		PRINT_PMODE("printPMode", true, "Print details of P-Mode with specified id");
		
		String  optionFlag;
		boolean parameterRequired;
		String  description; 

		Action(String optionFlag, boolean valueRequired, String description) {
			this.optionFlag = optionFlag;
			this.parameterRequired = valueRequired;
			this.description = description;	
		}
	}

	/**
	 * The requested action to execute and its parameter 
	 */
	private Action	curAction;
	private String  parameter;
		
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
			if (action.optionFlag.equals(clArgs[0])) {
				curAction = action;				
				if (action.parameterRequired && clArgs.length == 2)
					parameter = clArgs[1];
				else if (action.parameterRequired) 
					throw new IllegalArgumentException("Missing required parameter for action " + action.optionFlag);
			}
		}
		if (curAction == null) 
			throw new IllegalArgumentException("Argument " + clArgs[0] + " not recognized as valid action.");					
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
	 * @return The value for the action's parameter
	 */
	public String getParameter() {
		return parameter;
	}
	
	/**
	 * Prints a simple "usage" message to the console.
	 */
	public static void printUsage() {
		StringBuilder sb = new StringBuilder();
		sb.append(System.lineSeparator()).append("Use one of following arguments to execute an action: ")
		  .append(System.lineSeparator());
		for (Action option : Action.values()) 
			sb.append(option.optionFlag).append('\t').append(option.description).append(System.lineSeparator());
		System.out.println(sb.toString());
	}

}


