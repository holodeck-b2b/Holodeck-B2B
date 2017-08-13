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
package org.holodeckb2b.persistency.test.wrappers;

import java.io.Serializable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.holodeckb2b.persistency.jpa.Service;

/**
 * JPA Entity wrapper for testing the {@link Service} embedable
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity
@Table(name="TST_W_SERVICE")
public class WService implements Serializable {

    @Id
    //@GeneratedValue
    public long id;

    @Embedded
    public Service   e;

    public WService() {
        e = new Service();
    }
}
