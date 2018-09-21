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
package org.holodeckb2b.pmode.xml;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persister;

/**
 * Is an implementation of {@link IPMode} that uses XML documents to persist the P-Mode.
 * <p>This P-Mode implementation supports the parameters needed for the extensions defined in the AS4 Profile like
 * Reception Awareness, compression and multi-hop. The structure of the XML documents is defined in the
 * <a href="http://holodeck-b2b.org/schemas/2014/10/pmode">http://holodeck-b2b.org/schemas/2014/10/pmode</a> XSD which
 * is contained in <code>pmode.xsd</code>.
 *
 * @author Bram Bakx (bram at holodeck-b2b.org)
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IPMode
 */
@Root (name="PMode", strict=false)
public class PMode implements IPMode {

    @Attribute (name = "useStrictHeaderValidation", required = false)
    private boolean useStrictHeaderValidation = false;

    @Element (name = "id", required = true)
    private PModeId pmodeId;

    @Element (name = "mep", required = true)
    private String mep;

    @Element (name = "mepBinding", required = true)
    private String mepBinding;

    @Element (name = "Initiator", required = false)
    private TradingPartnerConfiguration initiator;

    @Element (name = "Responder", required = false)
    private TradingPartnerConfiguration responder;

    @Element (name = "Agreement", required = false)
    private Agreement agreement;

    @ElementList (entry = "Leg", type = Leg.class , required = true, inline = true)
    private ArrayList<Leg> legs;


    /**
     * Is responsible for solving dependencies child elements/objects may have on the P-Mode id. Currently this applies
     * to the identification of the delivery specifications included in the P-Mode. Because the Holodeck B2B Core
     * requires each delivery specification to have a unique id to enable reuse each delivery specification included in
     * the P-Mode is given an id combined of the P-Mode id, current time and type of delivery, for example the default
     * delivery specification defined on the Leg will have «P-Mode id»+"-"+«hhmmss» +"-defaultDelivery" as id.
     * <p>The objects containing the {@link DeliverySpecification}s are responsible for including these in the given
     * <code>Map</code> using the type of delivery as key and the object as value.
     *
     * @param dependencies  A <code>Map</code> containing all {@link DeliverySpecification} objects that have to be
     *                      assigned an id. The key of the entry MUST be a <code>String</code> containing the type
     *                      of delivery, e.g. "defaultDelivery".
     */
    @Commit
    public void solveDepencies(final Map dependencies) {
        if (dependencies == null)
            return;

        for(final Object k : dependencies.keySet()) {
            final Object dep = dependencies.get(k);
            if (k instanceof String && dep != null && dep instanceof DeliverySpecification)
                ((DeliverySpecification) dep).setId(this.pmodeId.id
                                                    + "-" + new SimpleDateFormat("HHmmss").format(new Date())
                                                    + "-" + k);
        }
    }

    /**
     * Gets the P-Mode <code>id</code>.
     *
     * @return The PMode <code>id</code>
     */
    @Override
    public String getId() {
        return pmodeId.id;
    }

    /**
     * Gets the P-Mode <code>id</code> include parameter.
     *
     * @return The PMode <code>id</code> include parameter.
     */
    @Override
    public Boolean includeId() {
        return pmodeId.include;
    }

    /**
     * Gets the PMode <code>mep</code>.
     *
     * @return The PMode <code>mep</code>.
     */
    @Override
    public String getMep() {
        return mep;
    }

    /**
     * Gets the PMode <code>mepBinding</code>.
     *
     * @return The PMode <code>mepBinding</code>.
     */
    @Override
    public String getMepBinding() {
        return mepBinding;
    }

    /**
     * Gets the PMode <code>legs</code>.
     * @return The PMode <code>legs</code>.
     */
    @Override
    public ArrayList<Leg> getLegs() {
        return this.legs;
    }

    /**
     * Gets the leg with the specified label from the P-Mode.
     * <p>Note that the requested leg can be found directly based on its assigned label <b>or</b> if the legs have not
     * been assigned labels on their sequence in the list, with the first being the <i>REQUEST</i> leg and the second
     * the <i>REPLY</i>.
     *
     * @return  The specified leg if it exists in the P-Mode, or<br>
     *          <code>null</code> if there is no leg with the given label
     */
    @Override
    public ILeg getLeg(final Label label) {
        if (Utils.isNullOrEmpty(legs))
            return null;

        ILeg    leg = null;
        for(final ILeg l : legs) {
            if (l.getLabel() == label) {
                leg = l; break;
            }
        }

        if (leg == null) {
            // Leg not found based on label, get based on sequence
            if (label == Label.REQUEST)
                leg = legs.get(0);
            else if (legs.size() > 1)
                leg = legs.get(1);
        }

        return leg;
    }

    /**
     *
     * @return The PMode <code>initiator</code>
     */
    @Override
    public TradingPartnerConfiguration getInitiator() {
        return this.initiator;
    }


    /**
     *
     * @return The PMode <code>responder</code>
     */
    @Override
    public TradingPartnerConfiguration getResponder() {
        return this.responder;
    }


    /**
     *
     * @return The PMode <code>agreement</code>
     */
    @Override
    public Agreement getAgreement() {
        return this.agreement;
    }

    /**
     * Gets the setting for whether Holodeck B2B should perform a strict validation of the ebMS header meta-data
     * as specified in the ebMS Specifications for messages processed under this P-Mode.
     * <p>For Holodeck B2B to be able to process a message unit it does not need to conform to all the requirements as
     * stated in the ebMS Specifications, for example the formatting of values is mostly irrelevant to Holodeck B2B.
     * Therefore two validation modes are offered, <i>basic</i> and <i>strict</i>.
     * <p>Note that there is also a global setting for the validation mode ({@link
     * IConfiguration#useStrictHeaderValidation()}. This P-Mode setting can only be used to make the validation more
     * strict, not more relaxed, i.e. if the global setting is to use strict validation the P-Mode setting is ignored.
     *
     * @return <code>true</code> if a strict validation of the ebMS header meta-data should be performed for message
     *         units which processing is governed by this P-Mode,<br>
     *         <code>false</code> if a basic validation is enough
     * @since 4.0.0
     */
    @Override
    public boolean useStrictHeaderValidation() {
        return useStrictHeaderValidation;
    }

    /**
     * Creates a new <code>PMode</code> object based from a XML Document in the given file. The XML document in the
     * file must conform to the XML schema definition given in <code>pmode.xsd</code>
     *
     * @param  xsdFile      A handle to file that contains the meta data
     * @return              A <code>PMode</code> for the message meta
     *                      data contained in the given file
     * @throws Exception    When the specified file is not found, readable or
     *                      does not contain a valid P-Mode XML document.
     */
    public static PMode createFromFile(final File xmlFile) throws Exception {
        if( !xmlFile.exists() || !xmlFile.canRead())
            // Given file must exist and be readable to be able to read PMode
            throw new Exception("Specified XML file '" + xmlFile.getAbsolutePath()
                                    + "' not found or no permission to read!");

        // When the P-Mode can not be read from the file the Persister.read() may throw a InvocationTargetException
        // that will contain a PersistenceException exception that describes the actual error. Therefor we catch
        // InvocationTargetException and only throw the PersistenceException
        try {
            return new Persister().read(PMode.class, xmlFile);
        } catch (final InvocationTargetException ex) {
            // Only throw the target exception when it really is an exception
            final Throwable t = ex.getTargetException();
            if (t != null && t instanceof Exception)
                throw (Exception) t;
            else
                throw ex;
        }
    }
}
