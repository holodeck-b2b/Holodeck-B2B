/*******************************************************************************
 * Copyright (C) 2019 The Holodeck B2B Team, Sander Fieten
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.general.IAgreement;
import org.holodeckb2b.interfaces.pmode.ILeg;
import org.holodeckb2b.interfaces.pmode.ILeg.Label;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Text;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.core.Validate;

/**
 * Is a generic {@link IPMode} implementation that uses XML documents to persist the P-Mode. This implementation
 * supports the parameters needed for the configuration of the extensions defined in the AS4 Profile like Reception
 * Awareness, AS4 Compression and multi-hop. The structure of the XML documents is defined in the
 * <a href="http://holodeck-b2b.org/schemas/2014/10/pmode">http://holodeck-b2b.org/schemas/2014/10/pmode</a> XSD which
 * is contained in <code>src/main/resources/xsd/pmode.xsd</code>.
 * <p>Instances of this class can be created either manually, by reading XML representation or using another {@link
 * IPMode} object. The latter allows the source to act as a "profile" which configures most parameters and can be
 * adjusted to create a specfic and complete P-Mode.
 *
 * @author Bram Bakx (bram at holodeck-b2b.org)
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @see IPMode
 * @since 5.0.0
 */
@Root(name = "PMode", strict = false)
@Namespace(reference="http://holodeck-b2b.org/schemas/2014/10/pmode")
public class PMode implements IPMode, Serializable {
	private static final long serialVersionUID = 4506841875705558821L;

    @Attribute (name = "useStrictHeaderValidation", required = false)
    private boolean useStrictHeaderValidation = false;

    static class PModeId implements Serializable {
		private static final long serialVersionUID = -6518809112297907991L;

		/**
         * Identifies the PMode uniquely
         */
        @Text
        String  id;
        /**
         * Specifies if the PMode ID should be included in the actual message
         */
        @Attribute(required = false)
        Boolean include = false;
    }

    @Element (name = "id", required = true)
    private PModeId pmodeId = new PModeId();

    @Element (name = "mep", required = true)
    private String mep;

    @Element (name = "mepBinding", required = true)
    private String mepBinding;

    @Element (name = "Initiator", required = false)
    private PartnerConfig initiator;

    @Element (name = "Responder", required = false)
    private PartnerConfig responder;

    @Element (name = "Agreement", required = false)
    private Agreement agreement;

    @ElementList (entry = "Leg", type = Leg.class , required = true, inline = true)
    private ArrayList<Leg> legs;

    /**
     * Default constructor to create a new empty P-Mode object.
     */
    public PMode() {};

    /**
     * Constructs a new P-Mode based on the parameters set in given P-Mode.
     *
     * @param srcPMode  The P-Mode to copy parameters from
     */
    public PMode(final IPMode srcPMode) {
        if (srcPMode != null) {
        	this.pmodeId.id = srcPMode.getId();
            this.pmodeId.include = srcPMode.includeId();
            this.useStrictHeaderValidation = srcPMode.useStrictHeaderValidation();
            this.mep = srcPMode.getMep();
            this.mepBinding = srcPMode.getMepBinding();
            this.agreement = srcPMode.getAgreement() != null ? new Agreement(srcPMode.getAgreement()) : null;
            this.initiator = srcPMode.getInitiator() != null ? new PartnerConfig(srcPMode.getInitiator()) : null;
            this.responder = srcPMode.getResponder() != null ? new PartnerConfig(srcPMode.getResponder()) : null;
            List<? extends ILeg> sourceLegs = srcPMode.getLegs();
            if (!Utils.isNullOrEmpty(sourceLegs)) {
                this.legs = new ArrayList<>(sourceLegs.size());
                sourceLegs.forEach(l -> this.legs.add(new Leg(l)));
            }
        }
    }

    /**
     * Creates a new <code>PMode</code> object based from a XML Document in the given input stream. The XML document in
     * the stream must conform to the XML schema definition given in <code>pmode.xsd</code>
     *
     * @param  is      Input stream that contains the P-Mode XML document
     * @return         New <code>PMode</code> instance representing the P-Mode contained in the XML document
     * @throws Exception    When the input stream cannot be read or does not contain a valid P-Mode XML document.
     */
    public static PMode createFromXML(final InputStream is) throws Exception {
    	// When the P-Mode cannot be read from the stream the Persister.read() may throw a InvocationTargetException
        // that will contain a StorageException exception that describes the actual error. Therefore we catch
        // InvocationTargetException and only throw the StorageException
        try {
            return new Persister(new AnnotationStrategy()).read(PMode.class, is, false);
        } catch (final InvocationTargetException ex) {
            // Only throw the target exception when it really is an exception
            final Throwable t = ex.getTargetException();
            if (t != null && t instanceof Exception)
                throw (Exception) t;
            else
                throw ex;
        }
    }

    /**
     * Writes the XML representation of this <code>PMode</code> object to the given output stream.
     *
     * @param  os      		The output stream t write the P-Mode XML document to
     * @throws Exception    When an error occurs writing the XML document to the stream
     */
    public void writeAsXMLTo(final OutputStream os) throws Exception {
    	// When the P-Mode cannot be written to the stream the Persister.read() may throw a InvocationTargetException
    	// that will contain a StorageException exception that describes the actual error. Therefore we catch
    	// InvocationTargetException and only throw the StorageException
    	try {
    		new Persister(new AnnotationStrategy()).write(this, os);
    	} catch (final InvocationTargetException ex) {
    		// Only throw the target exception when it really is an exception
    		final Throwable t = ex.getTargetException();
    		if (t != null && t instanceof Exception)
    			throw (Exception) t;
    		else
    			throw ex;
    	}
    }

    /**
     * Is responsible for solving dependencies child elements/objects may have on the P-Mode id. Currently this applies
     * to the identification of the delivery specifications included in the P-Mode. Because the Holodeck B2B Core
     * requires each delivery specification to have a unique id to enable reuse each delivery specification included in
     * the P-Mode is given an id combined of the P-Mode id, current time and type of delivery, for example the default
     * delivery specification defined on the Leg will have «P-Mode id»+"-"+«hhmmss» +"-defaultDelivery" as id.
     * <p>The objects containing the {@link DeliveryConfiguration}s are responsible for including these in the given
     * <code>Map</code> using the type of delivery as key and the object as value.
     *
     * @param dependencies  A <code>Map</code> containing all {@link DeliveryConfiguration} objects that have to be
     *                      assigned an id. The key of the entry MUST be a <code>String</code> containing the type
     *                      of delivery, e.g. "defaultDelivery".
     */
    @Commit
    public void solveDepencies(final Map dependencies) {
        if (dependencies == null)
            return;

        for(final Object k : dependencies.keySet()) {
            final Object dep = dependencies.get(k);
            if (k instanceof String && dep != null && dep instanceof DeliveryConfiguration)
                ((DeliveryConfiguration) dep).setId(this.pmodeId.id
                                                    + "-" + new SimpleDateFormat("HHmmss").format(new Date())
                                                    + "-" + k);
        }
    }

    /**
     * Is responsible for checking that no more than two legs are specified and assigning the leg labels if they are
     * not explicitly declared
     *
     * @throws IllegalArgumentException	when more than two legs are specified
     */
    @Validate
    public void checkLegs() {
    	if (Utils.isNullOrEmpty(legs))
    		return;

		if (legs.size() > 2)
			throw new IllegalArgumentException("P-Mode cannot contain more than 2 Legs");

		Leg l1 = legs.get(0);
		if (legs.size() == 1) {
			if (l1.getLabel() == null)
				l1.setLabel(Label.REQUEST);
			return;
		}
		Leg l2 = legs.get(1);
		if (l1.getLabel() == null && l2.getLabel() == null) {
			l1.setLabel(Label.REQUEST);
			l2.setLabel(Label.REPLY);
		} else if (l1.getLabel() != null && l2.getLabel() == null)
			l2.setLabel(l1.getLabel() == Label.REQUEST ? Label.REPLY : Label.REQUEST);
		else if (l1.getLabel() == null && l2.getLabel() != null)
			l1.setLabel(l2.getLabel() == Label.REPLY ? Label.REQUEST : Label.REPLY);
		else if (l1.getLabel() == l2.getLabel())
			throw new IllegalArgumentException("The Leg labels MUST be different between legs");
    }

    @Override
    public String getId() {
        return pmodeId.id;
    }

    public void setId(final String id) {
        this.pmodeId.id = id;
    }

    @Override
    public Boolean includeId() {
        return pmodeId.include;
    }

    public void setIncludeId(final Boolean includeId) {
        this.pmodeId.include = includeId;
    }

    @Override
    public IAgreement getAgreement() {
        return agreement;
    }

    public void setAgreement(final Agreement agreement) {
        this.agreement = agreement;
    }

    @Override
    public String getMep() {
        return mep;
    }

    public void setMep(final String mep) {
        this.mep = mep;
    }

    @Override
    public String getMepBinding() {
        return mepBinding;
    }

    public void setMepBinding(final String mepBinding) {
        this.mepBinding = mepBinding;
    }

    @Override
    public PartnerConfig getInitiator() {
        return initiator;
    }

    public void setInitiator(final PartnerConfig initiator) {
        this.initiator = initiator;
    }

    @Override
    public PartnerConfig getResponder() {
        return responder;
    }

    public void setResponder(final PartnerConfig responder) {
        this.responder = responder;
    }

    @Override
    public List<? extends ILeg> getLegs() {
        return legs;
    }

    @Override
	public Leg getLeg(Label label) {
        for (Leg l : legs)
            if (l.getLabel() == label)
                return l;
        return null;
    }

    public void setLegs(final Collection<Leg> legs) {
    	if (!Utils.isNullOrEmpty(legs)) {
    		this.legs = new ArrayList<>(legs.size());
    		legs.forEach(l -> this.legs.add(l));
    		checkLegs();
    	} else
    		this.legs = null;
    }

    public void addLeg(final Leg leg) {
        if (this.legs == null)
            this.legs = new ArrayList<>();

        this.legs.add(leg);
        checkLegs();
    }

    @Override
    public boolean useStrictHeaderValidation() {
        return useStrictHeaderValidation;
    }

    public void shouldUseStrictHeaderValidation(final boolean useStrictValidation) {
        this.useStrictHeaderValidation = useStrictValidation;
    }
}
