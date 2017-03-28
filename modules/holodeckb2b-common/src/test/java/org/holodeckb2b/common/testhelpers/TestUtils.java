package org.holodeckb2b.common.testhelpers;

import org.apache.axiom.om.OMElement;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.holodeckb2b.common.mmd.xml.MessageMetaData;
import org.holodeckb2b.interfaces.general.EbMSConstants;
import org.holodeckb2b.interfaces.general.IProperty;

import javax.xml.namespace.QName;
import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

//import org.holodeckb2b.module.HolodeckB2BCore;

/**
 * Created at 15:22 19.02.17
 *
 * @author Timur Shakuov (t.shakuov at gmail.com)
 */
public class TestUtils {
    /**
     * Get filled mmd document for testing
     * @return
     */
    public static MessageMetaData getMMD(String resource, Object testInstance) {
        final String mmdPath =
                testInstance.getClass().getClassLoader()
                        .getResource(resource).getPath();
        final File f = new File(mmdPath);
        MessageMetaData mmd = null;
        try {
            mmd = MessageMetaData.createFromFile(f);
        } catch (final Exception e) {
            fail("Unable to test because MMD could not be read correctly!");
        }
        return mmd;
    }

    public static boolean eventContainsMsg(List<LoggingEvent> events, Level logLevel, String msg) {
        boolean flag = false;
        for(LoggingEvent e : events) {
            if(e.getLevel().equals(logLevel)) {
                if(e.getRenderedMessage().trim().equals(msg)) {
                    flag = true;
                }
            }
        }
        return flag;
    }
}
