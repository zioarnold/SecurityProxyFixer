package r2u.tools.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converters {
    /**
     * Funzione di conversione di una hashmap in array di interi
     *
     * @param netCoServCo hashmap che gli viene passato per la conversione
     * @return uno array di interi contenente le system_id
     */
    public static ArrayList<Integer> getIntegerFromHashMap(HashMap<String, String> netCoServCo) {
        ArrayList<Integer> systemIds = new ArrayList<>();
        for (Map.Entry<String, String> pair : netCoServCo.entrySet()) {
            systemIds.add(Integer.valueOf(pair.getKey()));
        }
        return systemIds;
    }

    /**
     * Funzione atto a convertire un HashMap in una lista di stringhe, infatti elabora il campo globale
     * 'readOnlyDocuments' che contiene in se la mappatura delle classi documentali e le guid
     *
     * @return restituisce la lista delle classi documentali non processati per via di READ_ONLY delimitato da punto e virgola affiancato da GUID
     */
    public static ArrayList<String> getStringsFromHashMap(HashMap<String, String> readOnlyDocuments) {
        ArrayList<String> docClassIDs = new ArrayList<>();
        for (Map.Entry<String, String> pair : readOnlyDocuments.entrySet()) {
            docClassIDs.add(pair.getKey() + ";" + pair.getValue());
        }
        return docClassIDs;
    }

    public static HashMap<String, String> netCoConverter(ArrayList<String> netCo) {
        HashMap<String, String> netCoMap = new HashMap<>();
        for (String net : netCo) {
            netCoMap.put(net.split("=")[0], net.split("=")[1]);
        }
        return netCoMap;
    }

    public static HashMap<String, Boolean> convertArrayList2HashMap(ArrayList<String> documentList) {
        HashMap<String, Boolean> documentMap = new HashMap<>();
        for (String document : documentList) {
            documentMap.put(document.split("=")[0], Boolean.valueOf(document.split("=")[1]));
        }
        return documentMap;
    }

    public static ArrayList<String> objectClassDocumentConverter(List<Object> secProx) {
        ArrayList<String> securityProxies = new ArrayList<>();
        for (Object secProxy : secProx) {
            securityProxies.add(secProxy.toString());
        }
        return securityProxies;
    }
}
