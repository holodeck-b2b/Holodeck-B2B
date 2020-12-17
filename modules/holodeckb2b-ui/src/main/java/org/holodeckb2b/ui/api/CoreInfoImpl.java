package org.holodeckb2b.ui.api;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.commons.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.IQueryManager;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.persistency.managers.QueryManager;
import org.holodeckb2b.security.trust.DefaultCertManager;

/**
 * Implements the {@link CoreInfo} interface to supply the UI app with information from the Holodeck B2B instance. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since 5.0.0
 */
public class CoreInfoImpl implements CoreInfo {
	private static final Logger	log = LogManager.getLogger(CoreInfoImpl.class);
	
	@Override
	public String getHostName() throws RemoteException {
		return HolodeckB2BCoreInterface.getConfiguration().getHostName();
	}
	
	@Override
	public PMode[] getPModes() throws RemoteException {
		Collection<IPMode> pmodes = HolodeckB2BCoreInterface.getPModeSet().getAll();
		PMode[] pmodeArray = new PMode[pmodes.size()];
		int i = 0;
		for(IPMode p : pmodes)
			pmodeArray[i++] = new PMode(p);
		return pmodeArray;
	}

	@Override
	public Map<String, X509Certificate> getCertificates(CertType type) throws RemoteException {
		try {
			DefaultCertManager certMgr = ((DefaultCertManager) HolodeckB2BCoreInterface.getCertificateManager());
			switch (type) {
			case Private :
				return certMgr.getPrivateKeyCertificates();
			case Partner :
				return certMgr.getPartnerCertificates();
			case Trusted :
				return certMgr.getTrustedCertificates();			
			}
		} catch (ClassCastException cce) {
			log.warn("The default Certificate Manager is not installed. Cannot get certificate information!");			
			throw new RemoteException("Error while retrieving certificate", cce);
		} catch (SecurityProcessingException re) {
			log.error("Could not retrieve {} certificates from Core! Error: {}", type.name(), re.getMessage());			
			throw new RemoteException("Error while retrieving certificate", re);
		}
		return null;
	}

	@Override
	public MessageUnit[] getMessageUnitInfo(String messageId) throws RemoteException {
		return getMessageUnits(qm -> qm.getMessageUnitsWithId(messageId));
	}
	
	@Override
	public MessageUnit[] getMessageUnitLog(Date upto, int max) throws RemoteException {		
		return getMessageUnits(qm -> qm.getMessageUnitHistory(upto, max));
	}	
	
	interface QueryExecutor {
		Collection<IMessageUnitEntity> executeQuery(QueryManager qm) throws PersistenceException;
	}
	
	private MessageUnit[] getMessageUnits(QueryExecutor q) throws RemoteException
	{
		IQueryManager qManager = HolodeckB2BCoreInterface.getQueryManager();		
		try {
			Collection<IMessageUnitEntity> msgUnits = q.executeQuery((QueryManager) qManager);
			if (!Utils.isNullOrEmpty(msgUnits)) {
				MessageUnit[] result = new MessageUnit[msgUnits.size()];
				int i = 0;
				for(IMessageUnitEntity m : msgUnits) {
					qManager.ensureCompletelyLoaded(m);				
					result[i++] = MessageUnit.copyOf(m);
				}
				return result;
			} else
				return null;
		} catch (PersistenceException pe) {
			log.error("Could not retrieve message unit(s) from Core! Error: {}", pe.getMessage());
			throw new RemoteException("Error retrieving message unit info", pe);
		}			
	}
}
