package r2u.tools.utils;

import com.filenet.api.core.Document;
import r2u.tools.config.Configurator;

public class Assigner {
    private static final Configurator instance = Configurator.getInstance();

    public static void assignNetCoSecurityProxy(String netco_servco, Document fetchedDocument) {
        SecurityProxySetup.securityProxySetUp(netco_servco, fetchedDocument, instance.getObjectStore());
    }

    public static void assignServCoSecurityProxy(String netco_servco, Document fetchedDocument) {
        SecurityProxySetup.securityProxySetUp(netco_servco, fetchedDocument, instance.getObjectStore());
    }
}
