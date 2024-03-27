package r2u.tools.conn;

import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.util.UserContext;
import r2u.tools.worker.SecurityFixer;

import javax.security.auth.Subject;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.logging.Logger;

public class FNConnector {
    private final String uriSource;
    private final String objectStoreSource;
    private final String sourceCPEUsername;
    private final String sourceCPEPassword;
    private final String jaasStanzaName;
    private final String objectClass; //Document,CustomObject etc
    private final HashMap<String, String> netCoServCo;
    private final HashMap<String, Boolean> documentMap;
    private final String query;
    private final String phase;

    private final Logger logger;

    public FNConnector(String uriSource,
                       String objectStoreSource,
                       String sourceCPEUsername,
                       String sourceCPEPassword,
                       String jaasStanzaName,
                       String objectClass,
                       HashMap<String, String> netCoServCo,
                       HashMap<String, Boolean> documentMap,
                       String query,
                       String phase,
                       Logger logger) {
        this.uriSource = uriSource;
        this.objectStoreSource = objectStoreSource;
        this.sourceCPEUsername = sourceCPEUsername;
        this.sourceCPEPassword = sourceCPEPassword;
        this.jaasStanzaName = jaasStanzaName;
        this.netCoServCo = netCoServCo;
        this.documentMap = documentMap;
        this.objectClass = objectClass;
        this.query = query;
        this.phase = phase;
        this.logger = logger;
    }

    public void initWork() throws SQLException {
        SecurityFixer securityFixer = new SecurityFixer(logger);
        switch (phase) {
            default:
                logger.info("Only 4 commands are available for Phase: 1, 2, 3, All. Aborting!");
                System.exit(-1);
                break;
            case "1":
                // TODO: crea delle security_proxy mappate su json (Security Proxies)
                break;
            case "2":
                // TODO: assegnare i gruppi ldap...
                break;
            case "3":
                securityFixer.startSecurityFix(getObjectStoreSource(), query, objectClass, netCoServCo, documentMap);
                break;
            case "All":
                // TODO : fare tutto
                securityFixer.startSecurityFix(getObjectStoreSource(), query, objectClass, netCoServCo, documentMap);
                break;
        }
    }

    private ObjectStore getObjectStoreSource() {
        Domain sourceDomain;
        Connection sourceConnection;
        ObjectStore objectStoreSource = null;
        try {
            sourceConnection = Factory.Connection.getConnection(uriSource);
            Subject subject = UserContext.createSubject(Factory.Connection.getConnection(uriSource), sourceCPEUsername, sourceCPEPassword, jaasStanzaName);
            UserContext.get().pushSubject(subject);
            sourceDomain = Factory.Domain.fetchInstance(sourceConnection, null, null);
            logger.info("FileNet sourceDomain name: " + sourceDomain.get_Name());
            objectStoreSource = Factory.ObjectStore.fetchInstance(sourceDomain, this.objectStoreSource, null);
            logger.info("Object Store source: " + objectStoreSource.get_DisplayName());
            logger.info("Connected to Source CPE successfully:" + sourceConnection.getURI() + " " + sourceConnection.getConnectionType());
        } catch (EngineRuntimeException exception) {
            logger.info(exception.getMessage());
            exception.printStackTrace();
            System.exit(-1);
        }
        return objectStoreSource;
    }
}
