/*******************************************************************************
 * Copyright (C) 2024 The Holodeck B2B Team, Sander Fieten
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
 ******************************************************************************/
package org.holodeckb2b.common.pmode;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.pmode.IKeyDerivationMethod;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;

/**
 * Contains the parameters related to the key derivation for message level encryption.
 *
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since  7.0.0
 */
public class KeyDerivationConfig implements IKeyDerivationMethod, Serializable {
    private static final long serialVersionUID = -72832467861156436L;

	@Element (name = "Algorithm", required = false)
    private String algorithm;

    @Element (name = "DigestAlgorithm", required = false)
    private String digestAlgorithm;

    @ElementList(entry = "Parameter", inline = true, required = false)
    private Collection<Parameter>    parameters;

    /**
     * Default constructor creates a new and empty <code>KeyDerivationConfig</code> instance.
     */
    public KeyDerivationConfig() {}

    /**
     * Creates a new <code>KeyDerivationConfig</code> instance using the parameters from the provided {@link
     * IKeyDerivationMethod} object.
     *
     * @param source The source object to copy the parameters from
     */
    public KeyDerivationConfig(final IKeyDerivationMethod source) {
        this.algorithm = source.getAlgorithm();
        this.digestAlgorithm = source.getDigestAlgorithm();
        setParameters(source.getParameters());
    }

    @Override
    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String getDigestAlgorithm() {
        return digestAlgorithm;
    }

    public void setDigestAlgorithm(final String algorithm) {
        this.digestAlgorithm = algorithm;
    }

    @Override
    public Map<String, ?> getParameters() {
		if (!Utils.isNullOrEmpty(parameters)) {
	        HashMap<String, String> map = new HashMap<>(parameters.size());
	        parameters.forEach(p -> map.put(p.getName(), p.getValue()));
	        return map;
		} else
			return null;
	}

	public void setParameters(final Map<String, ?> sourceSettings) {
	    if (!Utils.isNullOrEmpty(sourceSettings)) {
	        this.parameters = new ArrayList<>(sourceSettings.size());
	        sourceSettings.forEach((n, v) -> this.parameters.add(new Parameter(n, v.toString())));
	    }
	}

	public void addParameter(final String name, final Object value) {
	    if (this.parameters == null)
	        this.parameters = new ArrayList<>();
	    this.parameters.add(new Parameter(name, value.toString()));
	}
}
