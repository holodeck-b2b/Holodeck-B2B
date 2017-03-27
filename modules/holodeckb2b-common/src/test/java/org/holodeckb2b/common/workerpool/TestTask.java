/**
 * Copyright (C) 2014 The Holodeck B2B Team, Sander Fieten
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.common.workerpool;

import java.util.Map;

/**
 * Simple task used in test of the Worker pool
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
public class TestTask extends AbstractWorkerTask {

    String[]    pnames;
    String[]    pvalues;

    int         runs = 0;

    @Override
    public void setParameters(final Map<String, ?> parameters) {
        pnames = parameters.keySet().toArray(new String[0]);
        pvalues = parameters.values().toArray(new String[0]);
    }

    @Override
    public void doProcessing() {
        System.out.println("TestTask is running for the " + runs + " time! Parameter " + pnames[runs%pnames.length] + "=" + pvalues[runs%pvalues.length]);

        runs = runs + 1;
    }

}
