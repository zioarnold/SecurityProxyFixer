package r2u.tools.utils;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import r2u.tools.config.Configurator;
import r2u.tools.conn.FNConnector;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

public class JSONParser {
    private final static Logger logger = Logger.getLogger(JSONParser.class.getName());

    public void parseJson(String json) throws IOException, URISyntaxException {
        URI uri = Paths.get(json).toUri();
        JSONObject jsonObject = getJSON(new URI(uri.toString()).toURL());

        String sourceCPE = jsonObject.getString("sourceCPE"),
                sourceCPEObjectStore = jsonObject.getString("sourceCPEObjectStore"),
                sourceCPEUsername = jsonObject.getString("sourceCPEUsername"),
                sourceCPEPassword = jsonObject.getString("sourceCPEPassword"),
                jaasStanzaName = jsonObject.getString("jaasStanzaName"),
                documentClass = jsonObject.getString("documentClass"),
                query = jsonObject.getString("query"),
                isMassive = jsonObject.getString("isMassive");

        //Converto in arraylist di stringhe e poi in un hashmap rispettivamente String|String o String|Boolean
        HashMap<String, Boolean> documentMap = Converters.convertArrayList2StringBooleanHashMap(
                Converters.listObject2ArrayListStringConverter(
                        jsonObject.getJSONArray("objectClasses")
                                .getJSONObject(0)
                                .getJSONArray("Document").toList()
                )
        );

        HashMap<String, String> netCo = Converters.netCoConverter(
                Converters.listObject2ArrayListStringConverter(
                        jsonObject.getJSONArray("objectClasses")
                                .getJSONObject(0)
                                .getJSONArray("NetCo").toList()
                )
        );

        ArrayList<String> servCo = Converters.listObject2ArrayListStringConverter(
                jsonObject.getJSONArray("objectClasses")
                        .getJSONObject(0)
                        .getJSONArray("ServCo").toList()
        );

        ArrayList<String> documentClassList = Converters.listObject2ArrayListStringConverter(
                jsonObject.getJSONArray("objectClasses")
                        .getJSONObject(0)
                        .getJSONArray("docClassList").toList()
        );

        if (sourceCPE.isEmpty()) {
            logger.error("SourceCPE is empty. Aborting!");
            System.exit(-1);
        }
        if (sourceCPEObjectStore.isEmpty()) {
            logger.error("sourceCPEObjectStore is empty. Aborting!");
            System.exit(-1);
        }
        if (sourceCPEUsername.isEmpty()) {
            logger.error("sourceCPEUsername is empty. Aborting!");
            System.exit(-1);
        }
        if (sourceCPEPassword.isEmpty()) {
            logger.error("sourceCPEPassword is empty. Aborting!");
            System.exit(-1);
        }
        if (jaasStanzaName.isEmpty()) {
            logger.error("jaasStanzaName is empty. Aborting!");
            System.exit(-1);
        }
        if (documentClass.isEmpty()) {
            logger.error("documentClass is empty. Aborting!");
            System.exit(-1);
        }
        if (documentMap.isEmpty()) {
            logger.error("document is empty. Aborting!");
            System.exit(-1);
        }
        if (netCo.isEmpty()) {
            logger.error("netCo is empty. Aborting!");
            System.exit(-1);
        }
        if (servCo.isEmpty()) {
            logger.error("servCo is empty. Aborting!");
            System.exit(-1);
        }
        if (documentClassList.isEmpty()) {
            logger.error("documentClassList is empty. Aborting!");
            System.exit(-1);
        }
        if (isMassive.isEmpty()) {
            logger.error("isMassive is empty. Aborting!");
            System.exit(-1);
        }

        Configurator instance = Configurator.getInstance();
        instance.setUriSource(sourceCPE);
        instance.setSourceCPEObjectStore(sourceCPEObjectStore);
        instance.setSourceCPEUsername(sourceCPEUsername);
        instance.setSourceCPEPassword(sourceCPEPassword);
        instance.setJaasStanzaName(jaasStanzaName);
        instance.setDocumentClass(documentClass);
        instance.setNetCo(netCo);
        instance.setServCo(servCo);
        instance.setDocumentMap(documentMap);
        instance.setDocumentClassList(documentClassList);
        instance.setQuery(query);
        instance.setMassive(Boolean.parseBoolean(isMassive));
        FNConnector fnConnector = new FNConnector();
        fnConnector.initWork();
    }

    private static JSONObject getJSON(URL url) throws IOException {
        return new JSONObject(IOUtils.toString(url, StandardCharsets.UTF_8));
    }
}
