package r2u.tools.worker;

import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.*;
import com.filenet.api.property.Properties;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.json.JSONArray;
import r2u.tools.conn.DBConnector;
import r2u.tools.constants.Constants;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

import static r2u.tools.constants.Constants.QUERY_SDI_MAPPINGS_BU_ENABLED;
import static r2u.tools.constants.Constants.QUERY_SDI_MAPPINGS_BU_NOME_BU_U;

public class SecurityFixer {
    private final Logger logger;

    public SecurityFixer(Logger logger) {
        this.logger = logger;
    }

    public void startSecurityFix(ObjectStore objectStoreSource, String query, String documentClass,
                                 HashMap<String, String> netCoServCo, HashMap<String, Boolean> documentMap) {
        long startTime, endTime;
        String[] doc = documentClass.split(",");
        logger.info("Starting security fix...");
        startTime = System.currentTimeMillis();
        for (String docClass : doc) {
            switch (docClass) {
                case "CustomObject":
                case "Folder":{
                    //TODO: Da capire se andra` usato
                }
                break;
                case "Document": {
                    logger.info("Working with docClass " + docClass);
                    Iterator<?> iterator = fetchRows(docClass, query, objectStoreSource);
                    while (iterator.hasNext()) {
                        try {
                            RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                            Properties properties = repositoryRow.getProperties();
                            String id = properties.getIdValue("ID").toString();
                            Document fetchedDocument = Factory.Document.fetchInstance(objectStoreSource, id, null);
                            //Recupero il system_id del documento in anagrafica
                            //Nonostante che sia stringa, a db risulta numeric.
                            int system_id = getSystemIdByChronicleIdRef(properties.getStringValue("bo_bu_chronid_ref"));
                            logger.info("Found document: " + id + " of class: " + fetchedDocument.getClassName()
                                    + " with system_id={" + system_id + "}");
                            //Controllo se la classe documentale e` mappata
                            if (documentMap.containsKey(fetchedDocument.getClassName())) {
                                //Controllo se bisogna lavorare sulla classe documentale quindi flag = true
                                if (documentMap.get(fetchedDocument.getClassName())){
                                    Properties fetchedDocumentProperties = fetchedDocument.getProperties();
                                    Id oldSecurityProxyId = fetchedDocumentProperties.getIdValue("security_proxy");
                                    logger.info("Working document: " + id + " of class: " + fetchedDocument.getClassName()
                                            + " with system_id={" + system_id + "}");
                                    //Se system_id e` presente quindi maggior di zero.
                                    if (system_id > 0) {
                                        // Verifico se nella mapping e` presente system_id trovato
                                        if (netCoServCo.containsKey(String.valueOf(system_id))) {
                                            //Se si trova, allora mi faccio dare il prefisso ossia il contenuto dopo =
                                            //Se non si trova e quindi system_id e` zero, non si fa nulla!
                                            String netco_servco = netCoServCo.get(String.valueOf(system_id));
                                            Iterator<?> fetchedSecurityProxies = fetchSecurityProxies(fetchedDocument.getClassName() + netco_servco,
                                                    objectStoreSource);
                                            //Se c'e`, allora sostituisco il valore del campo security_proxy della classe documentale con il riferimento del nuovo security_proxy.
                                            if (fetchedSecurityProxies.hasNext()) {
                                                RepositoryRow servCoNetCoRepository = (RepositoryRow) fetchedSecurityProxies.next();
                                                Properties servCoNetCoRepositoryProperties = servCoNetCoRepository.getProperties();
                                                Id servCoNetCoRepositoryPropertiesIdValue = servCoNetCoRepositoryProperties.getIdValue("ID");
                                                CustomObject servCoNetCoCustomObject = Factory.CustomObject.fetchInstance(objectStoreSource, servCoNetCoRepositoryPropertiesIdValue, null);
                                                ObjectReference netCoServCoSecurityProxy = servCoNetCoCustomObject.getObjectReference();
                                                //Verifico se nella classe documentale non c'e` popolato SecurityProxy
                                                //Se non c'e` popolato glielo inserisco
                                                if (oldSecurityProxyId == null || oldSecurityProxyId.equals("")){
                                                    fetchedDocumentProperties.putObjectValue("security_proxy", netCoServCoSecurityProxy);
                                                    logger.info("Filling security_proxy with new Id: " + servCoNetCoRepositoryPropertiesIdValue);
                                                    fetchedDocument.save(RefreshMode.REFRESH);
                                                    logger.info("saved!");
                                                }
                                                //Verifico se nella classe documentale e` gia` presente la security_proxy nuova
                                                //Per capirci: se nella classe documentale c'e` gia` la security proxy nuova
                                                //Allora non si fa nulla.
                                                //Per capirci bis: se nella classe documentale c'e` gia` la security proxy
                                                //Ma che son diversi tipo: acq_pon_security != acq_pon_netco_security
                                                //Allora gli impasto quella nuova.
                                                if (!Objects.requireNonNull(oldSecurityProxyId).equals(servCoNetCoRepositoryPropertiesIdValue)){
                                                    fetchedDocumentProperties.putObjectValue("security_proxy", netCoServCoSecurityProxy);
                                                    logger.info("Replacing old security_proxy Id:  " + oldSecurityProxyId +
                                                            " with new security_proxy Id: " + servCoNetCoRepositoryPropertiesIdValue);
                                                    fetchedDocument.save(RefreshMode.REFRESH);
                                                    logger.info("saved!");
                                                }
                                            }
                                        } else {
                                            logger.warning("There's no system_id mapped in config.json, check NetCoServCo.\n" +
                                                    "So the operation won't affect current document with\n" +
                                                    "Id: {" + id + "} symbolic_name: {" + fetchedDocument.getClassName() + "}");
                                        }
                                    } else {
                                        logger.warning("There's no system_id filled within: " + fetchedDocument.getClassName() +
                                                " id: {" + id + "}");
                                    }
                                }
                            } else {
                                logger.warning("No " + fetchedDocument.getClassName() + " mapped in config.json at objectClasses -> Document");
                            }
                        } catch (SQLException e) {
                            logger.severe("Impossible to execute statements, check getSystemIdByChronicleIdRef()");
                            throw new RuntimeException(e);
                        }
                    }
                }
                break;
            }
        }
        endTime = System.currentTimeMillis();
        logger.info("Security fixer terminated within: " + DurationFormatUtils.formatDuration(endTime - startTime, "HH:MM:SS", true));
    }

    private int getSystemIdByChronicleIdRef(String boBuChronicleIdRef) throws SQLException {
        String query = "select dv.U4E36_SYSTEM_ID as SYSTEM_ID" +
                " from DOCVERSION dv" +
                "    inner join CLASSDEFINITION cd" +
                "        on dv.OBJECT_CLASS_ID = cd.OBJECT_ID" +
                " where cd.SYMBOLIC_NAME = 'acq_anagrafica_bu'" +
                " and dv.OBJECT_ID = GUID_TO_RAW('" +
                boBuChronicleIdRef.replaceAll("\\{", "").replaceAll("}", "") + "')";
        logger.info("getSystemIdByChronicleIdRef() executing query: { " + query + " }");
        ResultSet resultSet = DBConnector.executeQuery(query);
        int system_id = 0;
        if (resultSet != null){
            while (resultSet.next()){
                system_id= resultSet.getInt("SYSTEM_ID");
            }
        }
        return system_id;
    }

    private static Iterator<?> fetchSecurityProxies(String securityProxies, ObjectStore objectStoreSource) {
        String querySource = Constants.QUERY_SECURITY_PROXIES + " '" + securityProxies + "'";
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(querySource);
        return new SearchScope(objectStoreSource).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }

    private static Iterator<?> fetchRows(String docClass, String query, ObjectStore objectStoreSource) {
        String querySource = "SELECT * FROM " + docClass;
        if (!query.isEmpty()) {
            querySource = query;
        }
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(querySource);
        return new SearchScope(objectStoreSource).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }
}
