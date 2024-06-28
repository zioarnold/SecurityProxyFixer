package r2u.tools.config;

import com.filenet.api.core.ObjectStore;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Classe configuratore che contiene vari campi accessibili ovunque
 */
public class Configurator {
    private static Configurator instance = null;
    private String uriSource;
    private String sourceCPEObjectStore;
    private String sourceCPEUsername;
    private String sourceCPEPassword;
    private String jaasStanzaName;
    private String documentClass;
    private String query;
    private HashMap<String, String> netCo;
    private ArrayList<String> servCo, documentClassList;
    private HashMap<String, Boolean> documentMap;
    private ObjectStore objectStore;
    private boolean isMassive;
    private String testConnection;
    private boolean reWorkOldIds;
    private boolean reWorkNulls;
    private boolean count;
    private boolean removeOldPermissions;
    private ArrayList<String> ldapGroupToRemove;

    private Configurator() {

    }

    public static synchronized Configurator getInstance() {
        if (instance == null) {
            instance = new Configurator();
        }
        return instance;
    }

    public String getUriSource() {
        return uriSource;
    }

    public void setUriSource(String uriSource) {
        this.uriSource = uriSource;
    }

    public String getSourceCPEObjectStore() {
        return sourceCPEObjectStore;
    }

    public void setSourceCPEObjectStore(String sourceCPEObjectStore) {
        this.sourceCPEObjectStore = sourceCPEObjectStore;
    }

    public String getSourceCPEUsername() {
        return sourceCPEUsername;
    }

    public void setSourceCPEUsername(String sourceCPEUsername) {
        this.sourceCPEUsername = sourceCPEUsername;
    }

    public String getSourceCPEPassword() {
        return sourceCPEPassword;
    }

    public void setSourceCPEPassword(String sourceCPEPassword) {
        this.sourceCPEPassword = sourceCPEPassword;
    }

    public String getJaasStanzaName() {
        return jaasStanzaName;
    }

    public void setJaasStanzaName(String jaasStanzaName) {
        this.jaasStanzaName = jaasStanzaName;
    }

    public void setDocumentClass(String documentClass) {
        this.documentClass = documentClass;
    }

    public void setNetCo(HashMap<String, String> netCo) {
        this.netCo = netCo;
    }

    public void setServCo(ArrayList<String> servCo) {
        this.servCo = servCo;
    }

    public void setDocumentMap(HashMap<String, Boolean> documentMap) {
        this.documentMap = documentMap;
    }

    public String getDocumentClass() {
        return documentClass;
    }

    public HashMap<String, String> getNetCo() {
        return netCo;
    }

    public ArrayList<String> getServCo() {
        return servCo;
    }

    public HashMap<String, Boolean> getDocumentMap() {
        return documentMap;
    }

    public ObjectStore getObjectStore() {
        return objectStore;
    }

    public void setObjectStore(ObjectStore objectStore) {
        this.objectStore = objectStore;
    }

    public ArrayList<String> getDocumentClassList() {
        return documentClassList;
    }

    public void setDocumentClassList(ArrayList<String> documentClassList) {
        this.documentClassList = documentClassList;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setMassive(boolean isMassive) {
        this.isMassive = isMassive;
    }

    public boolean isMassive() {
        return isMassive;
    }

    public void setTestConnection(String testConnection) {
        this.testConnection = testConnection;
    }

    public String getTestConnection() {
        return testConnection;
    }

    public void setReWorkOldIds(boolean reWorkOldIds) {
        this.reWorkOldIds = reWorkOldIds;
    }

    public boolean getReWorkOldIds() {
        return reWorkOldIds;
    }

    public void setReWorkNulls(boolean reWorkNulls) {
        this.reWorkNulls = reWorkNulls;
    }

    public boolean isReWorkNulls() {
        return reWorkNulls;
    }

    public void setCount(boolean count) {
        this.count = count;
    }

    public boolean isCount() {
        return count;
    }

    public void setRemoveOldPermissions(boolean removeOldPermissions) {
        this.removeOldPermissions = removeOldPermissions;
    }

    public boolean isRemoveOldPermissions() {
        return removeOldPermissions;
    }

    public void setLdapGroupToRemove(ArrayList<String> ldapGroupToRemove) {
        this.ldapGroupToRemove = ldapGroupToRemove;
    }

    public ArrayList<String> getLdapGroupToRemove() {
        return ldapGroupToRemove;
    }
}
