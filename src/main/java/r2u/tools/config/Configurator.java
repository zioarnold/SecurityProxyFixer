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
}
