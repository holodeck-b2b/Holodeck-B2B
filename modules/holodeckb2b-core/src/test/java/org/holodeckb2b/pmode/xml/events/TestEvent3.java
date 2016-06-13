/*
 * Copyright (C) 2016 The Holodeck B2B Team, Sander Fieten
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
package org.holodeckb2b.pmode.xml.events;

import java.util.Date;
import java.util.UUID;
import org.holodeckb2b.interfaces.messagemodel.IMessageUnit;

/**
 *
 * @author Sander Fieten <sander at holodeck-b2b.org>
 */
public class TestEvent3 implements TestEvent2 {
    
    private IMessageUnit msgUnit = null;
    
    public TestEvent3() {};
    
    public TestEvent3(IMessageUnit mu) {
        msgUnit = mu;
    }
    
    @Override
    public String getId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public Date getTimestamp() {
        return new Date();
    }

    @Override
    public String getMessage() {
        return "Event #1";
    }

    @Override
    public IMessageUnit getSubject() {
        return msgUnit;
    }
    
}
