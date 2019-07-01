package org.holodeckb2b.ui.api;

import java.rmi.RemoteException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.holodeckb2b.common.messagemodel.MessageUnit;
import org.holodeckb2b.common.pmode.PMode;
import org.holodeckb2b.common.util.Utils;
import org.holodeckb2b.interfaces.core.HolodeckB2BCoreInterface;
import org.holodeckb2b.interfaces.persistency.PersistenceException;
import org.holodeckb2b.interfaces.persistency.dao.IQueryManager;
import org.holodeckb2b.interfaces.persistency.entities.IMessageUnitEntity;
import org.holodeckb2b.interfaces.pmode.IPMode;
import org.holodeckb2b.interfaces.security.ICertificateManager.CertificateUsage;
import org.holodeckb2b.interfaces.security.SecurityProcessingException;
import org.holodeckb2b.persistency.managers.QueryManager;
import org.holodeckb2b.security.CertificateManager;

/**
 * Implements the {@link CoreInfo} interface to supply the UI app with information from the Holodeck B2B instance. 
 * 
 * @author Sander Fieten (sander at holodeck-b2b.org)
 * @since HB2B_NEXT_VERSION
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
	public X509Certificate getCertificate(String alias, CertType type) throws RemoteException {
		try {
			switch (type) {
			case PrivateKey :
				return 
				((CertificateManager) HolodeckB2BCoreInterface.getCertificateManager()).getPrivateKeyCertificate(alias);
			case EncryptionKey :
				return 
					HolodeckB2BCoreInterface.getCertificateManager().getCertificate(CertificateUsage.Encryption, alias);
			case TrustedCert :
				return 
					HolodeckB2BCoreInterface.getCertificateManager().getCertificate(CertificateUsage.Validation, alias);			
			}
		} catch (SecurityProcessingException re) {
			log.error("Could not retrieve a certificate from Core! Request: {}/{}. Error: {}", type.name(), alias, 
						re.getMessage());			
			throw new RemoteException("Error while retrieving certificate", re);
		}
		return null;
	}

	@Override
	public X509Certificate[] getTrustedCertificates() throws RemoteException {
		Collection<X509Certificate> trustedCerts;
		try {
			trustedCerts = HolodeckB2BCoreInterface.getCertificateManager().getValidationCertificates();
		} catch (SecurityProcessingException re) {
			log.error("Could not retrieve the collection of trusted certificate from Core! Error: {}", re.getMessage());
			throw new RemoteException("Error while retrieving certificates", re);			
		}
		if (!Utils.isNullOrEmpty(trustedCerts)) {
			X509Certificate[] result = new X509Certificate[trustedCerts.size()];
			int i = 0;
			for(X509Certificate c : trustedCerts)
				result[i++] = c;
			return result;
		} else
			return null;
	}

	@Override
	public MessageUnit[] getMessageUnitInfo(String messageId) throws RemoteException {
		return getMessageUnits((QueryExecutor) qm -> qm.getMessageUnitsWithId(messageId));
	}
	
	@Override
	public MessageUnit[] getMessageUnitLog(Date upto, int max) throws RemoteException {		
		return getMessageUnits((QueryExecutor) qm -> ((QueryManager) qm).getMessageUnitHistory(upto, max));
	}	
	
	interface QueryExecutor {
		Collection<IMessageUnitEntity> getMessageUnits(IQueryManager qm) throws PersistenceException;
	}
	
	private MessageUnit[] getMessageUnits(QueryExecutor q) throws RemoteException
	{
		IQueryManager qManager = HolodeckB2BCoreInterface.getQueryManager();		
		try {
			Collection<IMessageUnitEntity> msgUnits = q.getMessageUnits(qManager);
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
