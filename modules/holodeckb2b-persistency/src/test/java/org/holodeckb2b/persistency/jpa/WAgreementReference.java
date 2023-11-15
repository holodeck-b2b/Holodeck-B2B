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
package org.holodeckb2b.persistency.jpa;

import java.io.Serializable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.holodeckb2b.persistency.jpa.AgreementReference;

/**
 * JPA Entity wrapper for testing the {@link AgreementReference} embedable
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 */
@Entity
@Table(name="TST_W_AGREEMENTREF")
public class WAgreementReference implements Serializable {

    @Id
    @GeneratedValue
    protected long id;

    @Embedded
    protected AgreementReference   e;

    public WAgreementReference() {
        e = new AgreementReference();
    }
}
