/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.holodeckb2b.common.workerpool;

import java.util.Map;

/**
 * Simple task used in test of the Worker pool
 * 
 * @author Sander Fieten <sander@holodeck-b2b.org>
 */
public class TestTask extends AbstractWorkerTask {

    String[]    pnames;
    String[]    pvalues;

    int         runs = 0;
    
    @Override
    public void setParameters(Map<String, ?> parameters) {
        pnames = parameters.keySet().toArray(new String[0]);
        pvalues = parameters.values().toArray(new String[0]);
    }

    @Override
    public void doProcessing() {
        System.out.println("TestTask is running for the " + runs + " time! Parameter " + pnames[runs%pnames.length] + "=" + pvalues[runs%pvalues.length]);
        
        runs = runs + 1;
    }
    
}
