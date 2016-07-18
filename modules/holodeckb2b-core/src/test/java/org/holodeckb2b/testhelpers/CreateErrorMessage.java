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
package org.holodeckb2b.testhelpers;

import java.util.Date;
import javax.persistence.EntityManager;
import org.holodeckb2b.ebms3.constants.ProcessingStates;
import org.holodeckb2b.ebms3.persistency.entities.EbmsError;
import org.holodeckb2b.ebms3.persistency.entities.ErrorMessage;
import org.holodeckb2b.ebms3.persistency.entities.ProcessingState;
import org.holodeckb2b.ebms3.persistent.dao.JPAUtil;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class CreateErrorMessage {
    
    public static void main(String args[]) {
        EntityManager   em = JPAUtil.getEntityManagerToAlpha();
        
        ErrorMessage    newErrorMsg = new ErrorMessage();
        newErrorMsg.setMessageId("this-is-not-a-real-msg-id@just.for.test.holodeck");
        newErrorMsg.setRefToMessageId("this-is-a-fake-refto-msg-id@just.for.test.holodeck");
        newErrorMsg.setTimestamp(new Date());
        newErrorMsg.setPMode("PMODE-JUST-FOR-TEST-ERROR-BUNDLING");
        
        EbmsError       newError = new EbmsError();
        newError.setSeverity(IEbmsError.Severity.WARNING);
        newError.setCategory("test");
        newError.setErrorCode("EBMS:xxxx");
        newErrorMsg.addError(newError);
        
        ProcessingState state = new ProcessingState(ProcessingStates.CREATED);
        newErrorMsg.setProcessingState(state);
        
        em.getTransaction().begin();
        em.persist(newErrorMsg);
        em.getTransaction().commit();
        
        System.out.println("Added error message to database!");
    }
}
