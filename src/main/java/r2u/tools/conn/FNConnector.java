package r2u.tools.conn;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.util.UserContext;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import r2u.tools.config.Configurator;
import r2u.tools.constants.Constants;
import r2u.tools.worker.SecurityFixer;

import javax.security.auth.Subject;

public class FNConnector {
    private final Configurator instance = Configurator.getInstance();
    private final static Logger logger = Logger.getLogger(FNConnector.class.getName());

    public FNConnector() {
    }

    public void initWork() {
        SecurityFixer securityFixer = new SecurityFixer();
        ObjectStore objectStore = null;
        int indexAttempt = 1, maxAttempts = 5;
        while (objectStore == null) {
            objectStore = objectStoreSetUp(indexAttempt);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logger.error("SOMETHING WRONG WITH THREAD.SLEEP. Aborting!", e);
                System.exit(-1);
            }
            if (indexAttempt == maxAttempts) {
                break;
            }
            indexAttempt++;
        }
        if (objectStore != null) {
            instance.setObjectStore(objectStore);
            if (instance.getTestConnection().equalsIgnoreCase("no")) {
                long startTime, endTime;
                logger.info("Starting security fix...");
                startTime = System.currentTimeMillis();
                securityFixer.startSecurityFix();
                endTime = System.currentTimeMillis();
                logger.info("Security fixer terminated within: " + DurationFormatUtils.formatDuration(endTime - startTime, Constants.dateTimeFormat, true));
                UserContext.get().popSubject();
            }
        } else {
            logger.error("AFTER " + indexAttempt + " ATTEMPTS TO ESTABLISH THE CONNECTION TO: " + instance.getUriSource() + " PROGRAM IS ABORTED!");
            System.exit(-1);
        }
    }

    /**
     * Funzione atto a configurare la connessione verso object store d'interesse
     *
     * @param indexAttempt indice di tentativi
     * @return restituisce oggetto ObjectStore oppure null quando non si riesce.
     */
    private ObjectStore objectStoreSetUp(int indexAttempt) {
        Domain sourceDomain;
        Connection sourceConnection;
        logger.info("Trying to establish connection to: " + instance.getUriSource() + "; Attempt number: " + indexAttempt);
        try {
            sourceConnection = Factory.Connection.getConnection(instance.getUriSource());
            Subject subject = UserContext.createSubject(Factory.Connection.getConnection(instance.getUriSource()),
                    instance.getSourceCPEUsername(), instance.getSourceCPEPassword(), instance.getJaasStanzaName());
            UserContext.get().pushSubject(subject);
            sourceDomain = Factory.Domain.fetchInstance(sourceConnection, null, null);
            logger.info("FileNet sourceDomain name: " + sourceDomain.get_Name());
            ObjectStore objectStore = Factory.ObjectStore.fetchInstance(sourceDomain, instance.getSourceCPEObjectStore(), null);
            logger.info("Object Store source: " + objectStore.get_DisplayName());
            logger.info("Connected to Source CPE successfully: " + sourceConnection.getURI() + " " + sourceConnection.getConnectionType());
            return objectStore;
        } catch (EngineRuntimeException exception) {
            if (exception.getExceptionCode().getErrorId().equals("FNRCA0031")) {
                logger.error("CONNECTION TIMEOUT, PLEASE CHECK THE WSDL: " + instance.getUriSource(), exception);
            } else {
                logger.error("UNMANAGED ERROR IS CAUGHT: " + instance.getUriSource(), exception);
            }
            return null;
        }
    }
}
