package r2u.tools.utils;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import r2u.tools.conn.DBConnector;
import r2u.tools.conn.FNConnector;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

public class JSONParser {
    Logger logger;

    public void parseJson(String json, Logger logger) throws IOException, URISyntaxException, SQLException {
        URI uri = Paths.get(json).toUri();
        JSONObject jsonObject = getJSON(new URI(uri.toString()).toURL());
        this.logger = logger;
        String sourceCPE = jsonObject.getString("sourceCPE"),
                sourceCPEObjectStore = jsonObject.getString("sourceCPEObjectStore"),
                sourceCPEUsername = jsonObject.getString("sourceCPEUsername"),
                sourceCPEPassword = jsonObject.getString("sourceCPEPassword"),
                jaasStanzaName = jsonObject.getString("jaasStanzaName"),
                documentClass = jsonObject.getString("documentClass"),
                query = jsonObject.getString("query"),
                phase = jsonObject.getString("phase"),
                dbHostname = jsonObject.getString("dbHostname"),
                dbUsername = jsonObject.getString("dbUsername"),
                dbPassword = jsonObject.getString("dbPassword");

        //Converto in arraylist di stringhe e poi in un hashmap rispettivamente String|String o String|Boolean
        ArrayList<String> documentList = objectClassDocumentConverter(jsonObject.getJSONArray("objectClasses")
                .getJSONObject(0)
                .getJSONArray("Document").toList());
        HashMap<String,Boolean> documentMap = convertArrayList2HashMap(documentList);

        ArrayList<String> netCoServCoList = objectClassDocumentConverter(jsonObject.getJSONArray("objectClasses")
                .getJSONObject(0)
                .getJSONArray("NetCoServCo").toList());
        HashMap<String, String> netCoServCo = netCoServCoConverter(netCoServCoList);

        if (sourceCPE.isEmpty()) {
            System.out.println("SourceCPE is empty. Aborting!");
            System.exit(-1);
        }
        if (sourceCPEObjectStore.isEmpty()) {
            System.out.println("sourceCPEObjectStore is empty. Aborting!");
            System.exit(-1);
        }
        if (sourceCPEUsername.isEmpty()) {
            System.out.println("sourceCPEUsername is empty. Aborting!");
            System.exit(-1);
        }
        if (sourceCPEPassword.isEmpty()) {
            System.out.println("sourceCPEPassword is empty. Aborting!");
            System.exit(-1);
        }
        if (jaasStanzaName.isEmpty()) {
            System.out.println("jaasStanzaName is empty. Aborting!");
            System.exit(-1);
        }
        if (documentClass.isEmpty()) {
            System.out.println("documentClass is empty. Aborting!");
            System.exit(-1);
        }
        if (documentList.isEmpty()) {
            System.out.println("document is empty. Aborting!");
            System.exit(-1);
        }
        if (phase.isEmpty()) {
            System.out.println("Phase is empty. Aborting!");
            System.exit(-1);
        }
        if (dbHostname.isEmpty()) {
            System.out.println("databaseURL is empty. Aborting!");
            System.exit(-1);
        }
        if (dbUsername.isEmpty()) {
            System.out.println("dbUsername is empty. Aborting!");
            System.exit(-1);
        }
        if (dbPassword.isEmpty()) {
            System.out.println("dbPassword is empty. Aborting!");
            System.exit(-1);
        }
        if (netCoServCo.isEmpty()) {
            System.out.println("netCoServCo is empty. Aborting!");
            System.exit(-1);
        }

        FNConnector fnConnector = new FNConnector(
                sourceCPE,
                sourceCPEObjectStore,
                sourceCPEUsername,
                sourceCPEPassword,
                jaasStanzaName,
                documentClass,
                netCoServCo,
                documentMap,
                query,
                phase,
                logger
        );
        DBConnector.initLogger(logger);
        DBConnector.initDB(dbHostname, dbUsername, dbPassword);
        fnConnector.initWork();
        DBConnector.disconnect();
    }

    private static JSONObject getJSON(URL url) throws IOException {
        String string = IOUtils.toString(url, StandardCharsets.UTF_8);
        return new JSONObject(string);
    }

    private static HashMap<String, String> netCoServCoConverter(ArrayList<String> documentList){
        HashMap<String, String> netCoServCoMap = new HashMap<>();
        for (String document : documentList){
            netCoServCoMap.put(document.split("=")[0], document.split("=")[1]);
        }
        return netCoServCoMap;
    }

    private static HashMap<String, Boolean> convertArrayList2HashMap(ArrayList<String> documentList){
        HashMap<String, Boolean> documentMap = new HashMap<>();
        for (String document : documentList){
            documentMap.put(document.split("=")[0], Boolean.valueOf(document.split("=")[1]));
        }
        return documentMap;
    }

    private static ArrayList<String> objectClassDocumentConverter(List<Object> secProx) {
        ArrayList<String> securityProxies = new ArrayList<>();
        //Converto oggetti in stringhe
        for (Object secProxy : secProx) {
            securityProxies.add(secProxy.toString());
        }
        return securityProxies;
    }
}
