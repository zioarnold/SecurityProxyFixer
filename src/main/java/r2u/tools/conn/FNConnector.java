package r2u.tools.conn;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.util.UserContext;
import org.apache.log4j.Logger;
import r2u.tools.config.Configurator;
import r2u.tools.worker.SecurityFixer;

import javax.security.auth.Subject;

public class FNConnector {
    private final Configurator instance = Configurator.getInstance();
    private final static Logger logger = Logger.getLogger(FNConnector.class.getName());

    public FNConnector() {
    }

    public void initWork() {
        SecurityFixer securityFixer = new SecurityFixer();
        instance.setObjectStore(objectStoreSetUp());
        if (instance.getObjectStore() == null) {
            for (int i = 1; i < 5; i++) {
                logger.info("Retrying to establish connection to: " + instance.getUriSource() + "; Attempt number: " + i);
                objectStoreSetUp();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    logger.error("SOMETHING WRONG WITH THREAD.SLEEP. Aborting!", e);
                    System.exit(-1);
                }
            }
        }
        if (instance.getObjectStore() != null) {
            securityFixer.startSecurityFix();
        } else {
            logger.error("UNABLE TO ESTABLISH CONNECTION TO: " + instance.getUriSource() + " ABORTING!!!");
            System.exit(-1);
        }
    }

    private ObjectStore objectStoreSetUp() {
        Domain sourceDomain;
        Connection sourceConnection;
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
