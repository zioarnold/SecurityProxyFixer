package r2u.tools.utils;

import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.*;
import com.filenet.api.property.Properties;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.util.Id;
import org.apache.log4j.Logger;

import java.util.Iterator;

import static r2u.tools.constants.Constants.SECURITY_PROXY;

public class SecurityProxySetup {
    private final static Logger logger = Logger.getLogger(SecurityProxySetup.class.getName());

    /**
     * Metodo che imposta security_proxy in base alla variabile passatogli netco_servco
     *
     * @param netco_servco    variabile contenente  "_netco_security" oppure "_servco_security"
     * @param fetchedDocument documento attuale su cui si lavora
     * @param objectStore     variabile che contiene i dati sull'object store.
     */
    public static void securityProxySetUp(String netco_servco, Document fetchedDocument, ObjectStore objectStore) {
        //Vecchio ID del security proxy
        Id oldSecurityProxyId;
        //verifico se il campo non e` vuoto per assegnarlo
        //TODO: Da capire come gestire - se e` da gestire
        if (fetchedDocument.getProperties().getIdValue(SECURITY_PROXY) == null) {
            //e` vuoto quindi nulla da fare.
            logger.error("THERE'S NO SECURITY PROXY SET UP ON DOCUMENT: " + fetchedDocument.getClassName()
                    + "/" + fetchedDocument.getProperties().getIdValue("ID")
                    + " SECURITY_PROXY VALUE IS: " + fetchedDocument.getProperties().getIdValue(SECURITY_PROXY));
            return;
        } else {
            oldSecurityProxyId = fetchedDocument.getProperties().getIdValue(SECURITY_PROXY);
        }
        Iterator<?> securityProxyName = null;
        if (oldSecurityProxyId != null) {
            securityProxyName = DataFetcher.getSecurityProxyName(oldSecurityProxyId.toString(), objectStore);
        }
        Iterator<?> fetchedSecurityProxies = DataFetcher.getSecurityProxy(fetchedDocument.getClassName() + netco_servco,
                objectStore);

        String oldSecurityProxyName = "",/*Vecchio security_proxy*/ newSecurityProxyName /*Nuovo security_proxy*/;
        if (securityProxyName != null && securityProxyName.hasNext()) {
            RepositoryRow securityProxyRepository = (RepositoryRow) securityProxyName.next();
            Properties securityProxyProperties = securityProxyRepository.getProperties();
            oldSecurityProxyName = securityProxyProperties.getStringValue("codice");
        }
        //Se c'è, allora sostituisco il valore del campo security_proxy della classe documentale con il riferimento del nuovo security_proxy.
        if (fetchedSecurityProxies != null && fetchedSecurityProxies.hasNext()) {
            RepositoryRow securityProxyRepository = (RepositoryRow) fetchedSecurityProxies.next();
            Properties securityProxyProperties = securityProxyRepository.getProperties();
            //Nuovo ID del security proxy, sia netco o servco
            Id newSecurityProxyId = securityProxyProperties.getIdValue("ID");
            newSecurityProxyName = securityProxyProperties.getStringValue("codice");
            CustomObject securityProxyCustomObject = Factory.CustomObject.fetchInstance(objectStore, newSecurityProxyId, null);
            ObjectReference securityProxy = securityProxyCustomObject.getObjectReference();
            //Gestione del caso in cui, nel documento il campo security_proxy e` vuoto e quindi NullPointer.
            //Quindi lo si imposta.
            if (oldSecurityProxyName == null || oldSecurityProxyName.isEmpty()) {
                logger.warn("There's no security_proxy found on document id: " + fetchedDocument.getProperties().getIdValue("ID")
                        + " security_proxy is null, trying insert a new security_proxy id: " + newSecurityProxyId + " name: " + newSecurityProxyName);
                //HACK: Da vedere se lo imposta a null il campo.
//                fetchedDocument.getProperties().putObjectValue(SECURITY_PROXY, null);
                fetchedDocument.getProperties().putObjectValue(SECURITY_PROXY, securityProxy);
                fetchedDocument.save(RefreshMode.REFRESH);
                logger.info("saved!");
            }
            //Verifico se nella classe documentale e` gia` presente la security_proxy nuova
            //Per capirci: se nella classe documentale c'e` gia` la security proxy nuova
            //Allora non si fa nulla.
            //Per capirci bis: se nella classe documentale c'e` gia` la security proxy
            //Ma che son diversi tipo: acq_pon_security != acq_pon_netco_security
            //Allora gli impasto quella nuova.
            if (oldSecurityProxyName != null && !oldSecurityProxyName.equals(newSecurityProxyName)) {
                logger.info("Replacing old security_proxy Id: " + oldSecurityProxyId + " name: " + oldSecurityProxyName +
                        " with new security_proxy Id: " + newSecurityProxyId + " name: " + newSecurityProxyName);
                //HACK: Da vedere se lo imposta a null il campo.
//                fetchedDocument.getProperties().putObjectValue(SECURITY_PROXY, null);
                fetchedDocument.getProperties().putObjectValue(SECURITY_PROXY, securityProxy);
                fetchedDocument.save(RefreshMode.REFRESH);
                logger.info("saved!");
            } else {
                logger.info("There's already security_proxy set up. Moving towards next.");
            }
        } else {
            logger.error("THERE'S NO SECURITY_PROXY: " + fetchedDocument.getClassName() + netco_servco + " CREATED");
        }
    }
}
