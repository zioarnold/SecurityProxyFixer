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
        securityFixer.startSecurityFix();
    }

    private ObjectStore objectStoreSetUp() {
        Domain sourceDomain;
        Connection sourceConnection;
        ObjectStore objectStore = null;
        try {
            sourceConnection = Factory.Connection.getConnection(instance.getUriSource());
            Subject subject = UserContext.createSubject(Factory.Connection.getConnection(instance.getUriSource()),
                    instance.getSourceCPEUsername(), instance.getSourceCPEPassword(), instance.getJaasStanzaName());
            UserContext.get().pushSubject(subject);
            sourceDomain = Factory.Domain.fetchInstance(sourceConnection, null, null);
            logger.info("FileNet sourceDomain name: " + sourceDomain.get_Name());
            objectStore = Factory.ObjectStore.fetchInstance(sourceDomain, instance.getSourceCPEObjectStore(), null);
            logger.info("Object Store source: " + objectStore.get_DisplayName());
            logger.info("Connected to Source CPE successfully: " + sourceConnection.getURI() + " " + sourceConnection.getConnectionType());
        } catch (EngineRuntimeException exception) {
            logger.error("Unable to establish connection to: " + instance.getUriSource(), exception);
            System.exit(-1);
        }
        return objectStore;
    }
}
