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
package org.holodeckb2b.ebms3.pulling;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.holodeckb2b.common.exceptions.DatabaseException;
import org.holodeckb2b.ebms.axis2.Axis2Sender;
import org.holodeckb2b.ebms3.persistency.entities.PullRequest;
import org.holodeckb2b.ebms3.persistent.dao.EntityProxy;
import org.holodeckb2b.ebms3.persistent.dao.MessageUnitDAO;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.pmode.IPModeSet;
import org.holodeckb2b.interfaces.pmode.IPullRequestFlow;
import org.holodeckb2b.interfaces.pmode.IUserMessageFlow;
import org.holodeckb2b.interfaces.workerpool.IWorkerTask;
import org.holodeckb2b.interfaces.workerpool.TaskConfigurationException;

/**
 * Is responsible for starting the send process of Pull Request message units. The ebMS specific handlers in the Axis2 
 * handler chain will then take over and do the actual message processing. This worker is only to create kick-off the 
 * process.
 * <p>The worker will check for which P-Modes it has to send the pull requests. This can be a fixed list of P-Modes or
 * all P-Modes except some specific ones. This is specified during the initialization of the worker by the parameters
 * named <i>pmodes</i> and <code>include?</code> that specify the list of P-Mode [ids] and whether the list
 * is inclusive or exclusive. 
 * <p>For each P-Mode that can execute a pull a new {@link PullRequest} is created to start the messaging process.
 *
 * @author Sander Fieten
 */
public class PullWorker implements IWorkerTask {

    /**
     * The name of the parameter containing a list of P-Mode ids for which this worker should or should not execute 
     * the pull requests. Whether the P-Modes are included or excluded depends on the value of the {@link #PARAM_INCLUDE}
     * parameter.
     */
    public static final String PARAM_PMODES = "pmodes";
    
    /**
     * The name of the parameter that indicates whether pull requests should be send for the given P-Modes or if only
     * pull requests must be send for all other P-Modes.
     */
    public static final String PARAM_INCLUDE = "include?";
    
    /**
     * The name of this pull worker
     */
    private String  name;
    
    /**
     * The collection of P-Modes for which this worker should or should not do the pulling
     */
    private ArrayList<String> pmodes = new ArrayList<String>();
    
    /**
     * Indication whether the listed P-Modes should be included or excluded from pulling by this worker
     */
    private boolean inclusive = true;

    /**
     * Log facility, default log name is the class name
     */
    private Log log = LogFactory.getLog(PullWorker.class.getName());

    
    /**
     * Sets the name of this pull worker and updates the log accordingly.
     * 
     * @param   name    The new name to use
     */
    @Override
    public void setName(String name) {
        if (name == null || name.isEmpty())
            this.name = this.getClass().getName();
        else 
            this.name = name;
        
        // Also update log to reflect new name
        log = LogFactory.getLog(name);
    }
    
    /**
     * Gets the name of this pull worker
     * 
     * @return  The name of the pull worker
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the parameters of this pull worker. The pull worker has two optional parameters, listed below. When
     * passing the parameters to the worker the indicated constants should be used as parameter name.<ol>
     * <li><i>{@link #PARAM_PMODES} ("pmodes")</i>, <code>Collection&lt;String&gt;</code> : A collection of P-Mode ids 
     *      this pull worker should or should not do the pulling for;</li>
     * <li><i>{@link #PARAM_INCLUDE} ("include?")</i>, <code>Boolean</code> : Indicates whether the set of P-Mode ids 
     *      given in the second parameter should be included (value=<code>Boolean.TRUE</code>) or excluded (value=
     *      <code>Boolean.FALSE</code>) in the pulling. Defaults is to include.</li>
     * </ol> 
     */
    @Override
    public void setParameters(Map<String, ?> parameters) throws TaskConfigurationException {
        
        try {
            Object p = parameters.get(PARAM_PMODES);
            if (p != null) {
                this.pmodes.clear();
                this.pmodes.addAll((Collection<? extends String>) p);
            }
        } catch (ClassCastException cce) {
            log.error("Parameter [" + PARAM_PMODES + "] has wrong content!");
            throw new TaskConfigurationException("Parameter [" + PARAM_PMODES + "] has wrong content!");
        }
        
        try {
            Boolean include = (Boolean) parameters.get(PARAM_INCLUDE);
            inclusive = (include != null ? include.booleanValue() : true);
        } catch (ClassCastException cce) {
            log.error("Parameter [" + PARAM_INCLUDE + "] has wrong content!");
            throw new TaskConfigurationException("Parameter [" + PARAM_INCLUDE + "] has wrong content!");
        }
        
        // When only given P-Modes should be pulled, the list of P-Modes must not be empty
        if (inclusive && (pmodes == null || pmodes.isEmpty())) {
            log.error("Wrong configuration! List of P-Modes to pull for is empty.");
            throw new TaskConfigurationException("Wrong configuration! List of P-Modes to pull for is empty.");
        }
    }
    
    /**
     * Looks for message units that are for sending and kicks off the send process for each of them. To prevent a
     * message from being send twice the send process is only started if the processing state can be successfully
     * changed.
     */
    @Override
    public void run() {
            log.debug("Getting list of P-Modes for which pull requests should be send");
            Collection<IPMode>  pullForPModes = getPModesToPull();
            
            // Trigger Pull operation for each P-Mode
            for(IPMode p : pullForPModes) {
                log.debug("Start Pull operation for P-Mode [" + p.getId() +"]");
                // Get the MPC from the P-Mode
                log.debug("Get MPC to pull from");
                String mpc = null;
                ILeg leg = p.getLegs().iterator().next(); // currently only One-Way P-Modes, so only one leg
                // First check PullRequest flow (sub-channel) and then UserMessage flow
                Collection<IPullRequestFlow> flows = leg.getPullRequestFlows();
                if (flows != null && !flows.isEmpty()) {
                    IPullRequestFlow prf = flows.iterator().next();
                    mpc = prf.getMPC() != null ? prf.getMPC() : null;
                }
                if (mpc == null || mpc.isEmpty()) {
                    log.debug("No MPC defined in PullRequest flow, check UserMessage flow");
                    IUserMessageFlow flow = leg.getUserMessageFlow();
                    mpc = flow != null && flow.getBusinessInfo() != null ? flow.getBusinessInfo().getMpc() : null;
                }    
                
                if (mpc == null || mpc.isEmpty())
                    // No MPC defined in P-Mode, use default MPC
                    mpc = EbMSConstants.DEFAULT_MPC;
                log.debug("Using [" + mpc + "] as MPC for pull request");
                
                try {
                    log.debug("Create the PullRequest signal");
                    EntityProxy<PullRequest> pullRequest = MessageUnitDAO.createOutgoingPullRequest(p.getId(), mpc);
                    log.debug("PullRequest created [" + pullRequest.entity.getMessageId() 
                                + "] for P-Mode [" + p.getId() + "] and MPC=" + mpc);
                    Axis2Sender.sendMessage(pullRequest, log);
                } catch (DatabaseException ex) {
                    log.error("Could not create PullRequest for P-Mode [" + p.getId() + "] and MPC=" + mpc);
                }
            }
    }

    /**
     * Gets the set of P-Modes for which this worker should do the pull operation. The returned set will only include
     * P-Modes that need pulling. So if specific P-Modes were given for pulling but they are not configured for pulling
     * they will not be returned.
     * 
     * @return  Collection of {@link IPMode} objects for the P-Mode that should be pulled by this worker
     */
    private Collection<IPMode>  getPModesToPull() {
        ArrayList<IPMode>   pmodesToPull = new ArrayList<IPMode>();
            
        log.debug("Check if given P-Modes are to be pulled or not");
        if(inclusive) {
            log.debug("P-Modes to pull specified in parameter, check existence in configured P-Mode");
            final IPModeSet     curPModeSet = HolodeckB2BCoreInterface.getPModeSet();
            for(String pid : pmodes) {
                IPMode p = curPModeSet.get(pid);
                ILeg leg = p.getLegs().iterator().next();
                // The P-Mode should also be configured for pulling
                if (EbMSConstants.ONE_WAY_PULL.equalsIgnoreCase(p.getMepBinding()) 
                   && leg.getProtocol() != null 
                   && leg.getProtocol().getAddress() != null
                   ) 
                    pmodesToPull.add(p);
                else
                    log.warn("A unknown P-Mode or one that does not need pulling was specified for pulling!"
                             + "[id=" + pid + "]");
            }            
        } else {
            log.debug("Should pull for all P-Modes except specified");
            for(IPMode p : HolodeckB2BCoreInterface.getPModeSet().getAll()) {
                // Check if P-Mode is included at all
                if (pmodes.contains(p.getId())) 
                    log.debug("P-Mode [id=" + p.getId() + "] is excluded from pulling by this pull worker");
                else {
                    // Check if this P-Mode uses pulling
                    ILeg leg = p.getLegs().iterator().next();
                    if (EbMSConstants.ONE_WAY_PULL.equalsIgnoreCase(p.getMepBinding()) 
                       && leg.getProtocol() != null 
                       && leg.getProtocol().getAddress() != null
                       ) 
                        pmodesToPull.add(p);
                }
            }            
        }
        log.info("Pulling for " + pmodesToPull.size() + " P-Modes");
        
        return pmodesToPull;
    }    
}
