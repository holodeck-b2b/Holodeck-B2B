/*
 * Copyright (C) 2017 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.persistency.managers;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.holodeckb2b.common.messagemodel.util.CompareUtils;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.messagemodel.IEbmsError;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;
import org.holodeckb2b.interfaces.messagemodel.IPayload;
import org.holodeckb2b.interfaces.messagemodel.IUserMessage;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.persistency.entities.IUserMessageEntity;
import org.holodeckb2b.interfaces.processingmodel.ProcessingState;
import org.holodeckb2b.persistency.entities.ErrorMessageEntity;
import org.holodeckb2b.persistency.entities.ReceiptEntity;
import org.holodeckb2b.persistency.entities.UserMessageEntity;
import org.holodeckb2b.persistency.jpa.UserMessage;
import org.holodeckb2b.persistency.test.TestData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Is the test class for the {@link IQueryManager} implementation of the default persistency provider.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  3.0.0
 */
public class QueryManagerTest {

    private static QueryManager    queryManager;

    public QueryManagerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws PersistenceException {
        TestData.createTestSet();
        queryManager = new QueryManager();
    }

    private void assertSorted(List<? extends IMessageUnit> resultSet) {
        boolean sorted = true; Date last = null;
        for (IMessageUnit mu : resultSet)
            sorted &= last == null || mu.getCurrentProcessingState().getStartTime().before(last);
        assertTrue(sorted);
    }

    @Test
    public void getMessageUnitsInState() throws PersistenceException {
        // First test query on one state and one result
        List<IUserMessageEntity> usrMsgOnlyResult = queryManager.getMessageUnitsInState(IUserMessage.class,
                                                                IMessageUnit.Direction.OUT,
                                                                new ProcessingState[] { ProcessingState.SUBMITTED });
        assertFalse(Utils.isNullOrEmpty(usrMsgOnlyResult));
        assertEquals(1 , usrMsgOnlyResult.size());
        assertTrue(usrMsgOnlyResult.get(0) instanceof IUserMessageEntity);

        // Test on one state with multiple results
        List<IMessageUnitEntity>   result =  queryManager.getMessageUnitsInState(IMessageUnit.class,
                                                                IMessageUnit.Direction.OUT,
                                                                new ProcessingState[] { ProcessingState.DELIVERED });
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(3 , result.size());
        assertSorted(result);

        // Test no result
        result = queryManager.getMessageUnitsInState(IMessageUnit.class,
                                                     IMessageUnit.Direction.IN,
                                                     new ProcessingState[] { ProcessingState.FAILURE });
        assertTrue(Utils.isNullOrEmpty(result));

        // Test with multiple states
        result = queryManager.getMessageUnitsInState(IMessageUnit.class,
                                                     IMessageUnit.Direction.IN,
                                                     new ProcessingState[] { ProcessingState.FAILURE,
                                                                             ProcessingState.RECEIVED,
                                                                             ProcessingState.DONE
                                                                           });
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(4 , result.size());
        assertSorted(result);
    }

    @Test
    public void isAlreadyProcessed() throws PersistenceException {
        // Then a delivered User Message
        assertTrue(queryManager.isAlreadyProcessed(TestData.userMsg5.getMessageId()));

        // And one that is not yet delivered
        assertFalse(queryManager.isAlreadyProcessed(TestData.userMsg3.getMessageId()));
    }

    @Test
    public void getNumberOfTransmissions() throws PersistenceException {
        // User Message sent once
        assertEquals(1,
                     queryManager.getNumberOfTransmissions(new UserMessageEntity(new UserMessage(TestData.userMsg6))));
        // User Message sent 2x
        assertEquals(2,
                     queryManager.getNumberOfTransmissions(new UserMessageEntity(new UserMessage(TestData.userMsg2))));
        // User Message not sent
        assertEquals(0,
                     queryManager.getNumberOfTransmissions(new UserMessageEntity(new UserMessage(TestData.userMsg5))));

    }

    @Test
    public void getMessageUnitsWithId() throws PersistenceException {
        // Just one result
        Collection<IMessageUnitEntity>   result =  queryManager.getMessageUnitsWithId(TestData.userMsg1.getMessageId());

        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(1 , result.size());

        // Test no result
        result = queryManager.getMessageUnitsWithId("non-existing-msgid");
        assertTrue(Utils.isNullOrEmpty(result));

        // Test with multiple results of same type
        result = queryManager.getMessageUnitsWithId(TestData.receipt2.getMessageId());
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(3 , result.size());

        // Test with multiple results of different type
        result = queryManager.getMessageUnitsWithId(TestData.error4.getMessageId());
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(2 , result.size());
    }

    @Test
    public void getMessageUnitsForPModesInState() throws PersistenceException {
        Set<String> pmodeIds = new HashSet<>();

        // Just one result
        pmodeIds.add(TestData.userMsg5.getPModeId());
        List<IMessageUnitEntity>   result =  queryManager.getMessageUnitsForPModesInState(IUserMessage.class
                                                                                         , pmodeIds
                                                                                         , ProcessingState.DELIVERED);
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(1 , result.size());
        assertTrue(result.get(0) instanceof IUserMessageEntity);

        // No result
        result =  queryManager.getMessageUnitsForPModesInState(IUserMessage.class, pmodeIds,
                                                                ProcessingState.READY_TO_PUSH);
        assertTrue(Utils.isNullOrEmpty(result));

        // Multiple results from one P-Mode
        result =  queryManager.getMessageUnitsForPModesInState(IMessageUnit.class, pmodeIds, ProcessingState.DELIVERED);
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(3 , result.size());
        assertSorted(result);

        // One result from multiple P-Modes
        pmodeIds.add(TestData.error3.getPModeId());
        result =  queryManager.getMessageUnitsForPModesInState(IMessageUnit.class, pmodeIds, ProcessingState.CREATED);
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(1 , result.size());
        assertEquals(TestData.error3.getMessageId(), result.get(0).getMessageId());

        // Multiple result from multiple P-Modes
        pmodeIds.add(TestData.userMsg2.getPModeId());
        result =  queryManager.getMessageUnitsForPModesInState(IUserMessage.class, pmodeIds, ProcessingState.DELIVERED);
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(2 , result.size());
        assertEquals(TestData.userMsg2.getMessageId(), result.get(0).getMessageId());
        assertEquals(TestData.userMsg5.getMessageId(), result.get(1).getMessageId());
    }

    @Test
    public void getMessageUnitsWithLastStateChangedBefore() throws PersistenceException {
        // Test no result
        Collection<IMessageUnitEntity>   result =  queryManager.getMessageUnitsWithLastStateChangedBefore(daysBack(15));
        assertTrue(Utils.isNullOrEmpty(result));

        // Test one result
        result = queryManager.getMessageUnitsWithLastStateChangedBefore(daysBack(10));
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(1 , result.size());
        IMessageUnit mu = result.iterator().next();
        assertEquals(TestData.userMsg1.getMessageId(), mu.getMessageId());

        // Check boundary case with max allowed time equal to time stamp of state
        result = queryManager.getMessageUnitsWithLastStateChangedBefore(mu.getCurrentProcessingState().getStartTime());
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(1 , result.size());
        assertEquals(TestData.userMsg1.getMessageId(), result.iterator().next().getMessageId());
        // And now just off
        Calendar currentStateTime = Calendar.getInstance();
        currentStateTime.setTime(mu.getCurrentProcessingState().getStartTime());
        currentStateTime.add(Calendar.MINUTE, -1);
        result = queryManager.getMessageUnitsWithLastStateChangedBefore(currentStateTime.getTime());
        assertTrue(Utils.isNullOrEmpty(result));

        // Multiple results
        result = queryManager.getMessageUnitsWithLastStateChangedBefore(daysBack(6));
        assertFalse(Utils.isNullOrEmpty(result));
        assertEquals(5 , result.size());
    }

    private Date daysBack(int d) {
        Calendar currentTime = Calendar.getInstance();
        currentTime.add(Calendar.DAY_OF_YEAR, -d);
        return currentTime.getTime();
    }

    @Test
    public void ensureCompletelyLoaded() throws PersistenceException {
        // First get a stored user message from the database
        Collection<IMessageUnitEntity>   result =  queryManager.getMessageUnitsWithId(TestData.userMsg1.getMessageId());
        assertFalse(Utils.isNullOrEmpty(result));

        UserMessageEntity userMessage = (UserMessageEntity) result.iterator().next();
        // The User Message should not be completely loaded just after the query
        assertFalse(userMessage.isLoadedCompletely());

        queryManager.ensureCompletelyLoaded(userMessage);
        // Now it should be
        assertTrue(userMessage.isLoadedCompletely());
        // Also check that all info is indeed available
        assertNotNull(userMessage.getSender());
        assertTrue(CompareUtils.areEqual(TestData.userMsg1.getSender(), userMessage.getSender()));
        assertNotNull(userMessage.getReceiver());
        assertTrue(CompareUtils.areEqual(TestData.userMsg1.getReceiver(), userMessage.getReceiver()));
        assertFalse(Utils.isNullOrEmpty(userMessage.getPayloads()));
        IPayload retrievedPayload = userMessage.getPayloads().iterator().next();
        IPayload originalPayload = TestData.userMsg1.getPayloads().iterator().next();
        assertEquals(originalPayload.getContainment(), retrievedPayload.getContainment());
        assertFalse(Utils.isNullOrEmpty(retrievedPayload.getProperties()));
        assertTrue(CompareUtils.areEqual(retrievedPayload.getProperties().iterator().next(),
                                         originalPayload.getProperties().iterator().next()));
        assertEquals(originalPayload.getDescription().getText(), retrievedPayload.getDescription().getText());
        assertEquals(originalPayload.getDescription().getLanguage(), retrievedPayload.getDescription().getLanguage());
        assertFalse(Utils.isNullOrEmpty(userMessage.getMessageProperties()));
        assertTrue(CompareUtils.areEqual(TestData.userMsg1.getMessageProperties().iterator().next(),
                                         userMessage.getMessageProperties().iterator().next()));

        // Test also the Receipt (should be already loaded on query)
        result =  queryManager.getMessageUnitsWithId(TestData.receipt1.getMessageId());
        assertFalse(Utils.isNullOrEmpty(result));
        ReceiptEntity receipt = (ReceiptEntity) result.iterator().next();
        assertTrue(receipt.isLoadedCompletely());
        assertNull(receipt.getContent());

        // Test the Error (needs loading)
        result =  queryManager.getMessageUnitsWithId(TestData.error3.getMessageId());
        assertFalse(Utils.isNullOrEmpty(result));
        ErrorMessageEntity error = (ErrorMessageEntity) result.iterator().next();
        assertFalse(error.isLoadedCompletely());

        queryManager.ensureCompletelyLoaded(error);
        assertTrue(error.isLoadedCompletely());
        // Also check that all info is indeed available
        assertFalse(Utils.isNullOrEmpty(error.getErrors()));
        IEbmsError originalError = TestData.error3.getErrors().iterator().next();
        IEbmsError retrievedError = error.getErrors().iterator().next();
        assertEquals(originalError.getErrorCode(), retrievedError.getErrorCode());
        assertEquals(originalError.getErrorDetail(), retrievedError.getErrorDetail());
    }
}
