package r2u.tools.utils;

import com.filenet.api.collection.StringList;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import org.apache.log4j.Logger;

import java.util.Iterator;

public class DataFetcher {
    private final static Logger logger = Logger.getLogger(DataFetcher.class.getName());

    /**
     * Restituisce gli elementi in base al parametro che gli viene passato
     *
     * @param buAllChronicleIdRef variabile che contiene uno o più elementi bu_all_chronid_ref
     * @param objectStore         contiene i dati dell'object store
     * @return il risultato quando si trova il system_id
     */
    public static Iterator<?> fetchSystemIdByBUALLChronicleIdRef(StringList buAllChronicleIdRef, ObjectStore objectStore) {
        if (buAllChronicleIdRef.size() == 1) {
            String querySource = "SELECT * FROM acq_anagrafica_bu WHERE Id = " + buAllChronicleIdRef.get(0).toString();
            SearchSQL searchSQL = new SearchSQL();
            searchSQL.setQueryString(querySource);
            return new SearchScope(objectStore).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
        } else if (buAllChronicleIdRef.size() > 1) {
            StringBuilder boAll = new StringBuilder();
            for (Object o : buAllChronicleIdRef) {
                boAll.append(o.toString()).append(" OR Id = ");
            }
            String querySource = "SELECT * FROM acq_anagrafica_bu WHERE Id = " + boAll.substring(0, boAll.length() - 9);
            SearchSQL searchSQL = new SearchSQL();
            searchSQL.setQueryString(querySource);
            return new SearchScope(objectStore).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
        }
        return null;
    }

    /**
     * Funzione atto a ricercare su Relazioni SDI (acq_relation) il GUID del padre, passando il GUID del figlio (quindi allegato)
     *
     * @param tailId      variabile di coda su quale si effettua la ricerca presso la relazione sdi
     * @param objectStore contiene i dati dell'object store
     * @return restituisce il GUID del padre quando si processa l'allegato
     */
    public static Iterator<?> fetchHeadIdByRelationTailId(String tailId, ObjectStore objectStore) {
        String querySource = "SELECT [head_chronicle_id] FROM [acq_relation] WHERE [tail_chronicle_id] = " + tailId;
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(querySource);
        return new SearchScope(objectStore).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }

    /**
     * Funzione atto a ricercare la system_id in base alla variabile bo_bu
     *
     * @param boBuChronicleIdRef variabile che contiene in sé GUID su quale effettuare una query.
     * @param objectStore        contiene i dati dell'object store
     * @return restituisce il system_id in base alla variabile bo_bu_chronid_ref
     */
    public static Iterator<?> fetchSystemIdByBOBUChronicleIdRef(String boBuChronicleIdRef, ObjectStore objectStore) {
        String querySource = "SELECT [system_id] FROM [acq_anagrafica_bu] WHERE [id] = " + boBuChronicleIdRef;
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(querySource);
        return new SearchScope(objectStore).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }

    /**
     * Funzione atto a ricercare la security_proxy in base al criterio passatogli
     *
     * @param securityProxies variabile della security_proxy da ricercare
     * @param objectStore     contiene i dati dell'object store
     * @return restituisce un security_proxy
     */
    public static Iterator<?> fetchSecurityProxies(String securityProxies, ObjectStore objectStore) {
        String querySource = "SELECT * FROM [acq_security_proxy] WHERE [codice] = '" + securityProxies + "'";
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(querySource);
        return new SearchScope(objectStore).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }

    /**
     * Restituisce la lista delle classi documentali, folder o custom object passandogli per query o senza tramite docClass.
     *
     * @param docClass    una variabile configurabile su config.json, DEVE contenere almeno un dato.
     *                    I dati validi sono: Document, CustomObject o Folder.
     *                    Attualmente è gestito Document.
     * @param objectStore contiene i dati dell'object store
     * @return oggetto di tipo Iterator per poi lavorarli uno x uno.
     */
    public static Iterator<?> fetchRowsByClass(String docClass, ObjectStore objectStore) {
        logger.info("Fetching data by class: " + docClass);
        String querySource = "SELECT * FROM [" + docClass + "]";
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(querySource);
        return new SearchScope(objectStore).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }

    /**
     * Restituisce i dati a seconda del parametro query
     *
     * @param query       contiene una query che viene recepito da config.json
     * @param objectStore contiene i dati dell'object store
     * @return oggetto di tipo Iterator per poi lavorarli uno x uno.
     */
    public static Iterator<?> fetchDataByQuery(String query, ObjectStore objectStore) {
        logger.info("Fetching data by query: " + query);
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(query);
        return new SearchScope(objectStore).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }
}
