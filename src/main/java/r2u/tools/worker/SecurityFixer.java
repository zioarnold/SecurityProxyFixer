package r2u.tools.worker;

import com.filenet.api.collection.StringList;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.*;
import com.filenet.api.property.Properties;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.util.Id;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import r2u.tools.config.Configurator;
import r2u.tools.constants.Constants;

import java.util.*;

public class SecurityFixer {
    /**
     * Questi sono i field globali per permettere di accedervi ai loro valori fuori dei metodi ma all'interno della classe stessa.
     */
    private final static Logger logger = Logger.getLogger(SecurityFixer.class.getName());
    private final static String SECURITY_PROXY = "security_proxy";
    Configurator instance = Configurator.getInstance();

    /**
     * Metodo che fa il lavoretto nel ricavare le classi documentali e se sono lavorabili su json, flag è true.
     * Chiamare altri metodi di smistamento per netco o servco.
     */
    public void startSecurityFix() {
        long startTime, endTime;
        String[] doc = instance.getDocumentClass().split(",");
        logger.info("Starting security fix...");
        startTime = System.currentTimeMillis();
        for (String docClass : doc) {
            switch (docClass) {
                case "CustomObject":
                case "Folder": {
                    //TODO: Da capire come gestire, se è il caso
                }
                break;
                case "Document": {
                    logger.info("Working with docClass " + docClass);
                    logger.info("Fetching data by query: " + instance.getQuery());
                    Iterator<?> iterator = fetchRows(docClass, instance.getQuery(), instance.getObjectStore());
                    if (iterator != null) {
                        while (iterator.hasNext()) {
                            try {
                                RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                                Properties properties = repositoryRow.getProperties();
                                Document fetchedDocument = Factory.Document.fetchInstance(instance.getObjectStore(), properties.getIdValue("ID").toString(), null);
                                //Controllo se la classe documentale e` mappata
                                if (instance.getDocumentMap().containsKey(fetchedDocument.getClassName())) {
                                    //Controllo se bisogna lavorare sulla classe documentale quindi flag = true
                                    if (instance.getDocumentMap().get(fetchedDocument.getClassName())) {
                                        switch (fetchedDocument.getClassName()) {
                                            //Gestione delle classi documentali che non siano gli allegati, per dire Allegato Pon (acq_all_pon)
                                            default: {
                                                processDefaultDocumentClasses(fetchedDocument);
                                            }
                                            break;
                                            //Gestione degli allegati
                                            case "acq_all_pon":
                                            case "acq_all_oda":
                                            case "acq_all_rda":
                                            case "acq_all_rdo": {
                                                logger.info("Found document: " + fetchedDocument.getProperties().getIdValue("ID") + " of class: " + fetchedDocument.getClassName());
                                                //Qui recupero l'ID del documento padre dato che, siamo in gestione degli allegati.
                                                //Quindi recupero l'ID del padre per poi recuperare il suo security proxy da impostare all'allegato
                                                //Restituisce una string dell'ID dell documento, se è vuota, allora non si fa nulla,
                                                //Se non è vuota - allora si salva il documento con security proxy del padre
                                                Iterator<?> relationIterator = fetchHeadIdByRelationTailId(fetchedDocument.getProperties().getIdValue("ID").toString());
                                                String headByRelationByTailId = "";
                                                if (relationIterator != null && relationIterator.hasNext()) {
                                                    repositoryRow = (RepositoryRow) relationIterator.next();
                                                    headByRelationByTailId = String.valueOf(repositoryRow.getProperties().getIdValue("head_chronicle_id"));
                                                }
                                                if (!headByRelationByTailId.isEmpty()) {
                                                    saveSecurityProxyToChildDocument(instance.getObjectStore(), fetchedDocument, headByRelationByTailId);
                                                } else {
                                                    logger.error("NO PARENT_ID HAS BEEN FOUND! ");
                                                }
                                            }
                                            break;
                                        }
                                    } else {
                                        logger.info(fetchedDocument.getClassName() + "is not set up to work on. Skipping...");
                                    }
                                } else {
                                    logger.error("NO " + fetchedDocument.getClassName() + " MAPPED IN config.json AT objectClasses -> Document");
                                }
                            } catch (Exception e) {
                                logger.error("SOMETHING WENT WRONG... ", e);
                                throw new RuntimeException(e);
                            }
                        }
                    } else {
                        logger.error("UNABLE TO FETCH ROWS FROM : " + instance.getObjectStore().get_DisplayName());
                    }
                }
                break;
            }
        }
        endTime = System.currentTimeMillis();
        logger.info("Security fixer terminated within: " + DurationFormatUtils.formatDuration(endTime - startTime, Constants.dateTimeFormat, true));
    }

    /**
     * Metodo di gestione delle classi documentali di default, esempio: "acq_pon" o simili.
     * Nella sua logica, lui determina a quale security_proxy associare che per questo sono state implementati dei metodi appositi.
     * Prima di assegnare la security_proxy, tale metodo verifica la presenza del bo_bu_chronid_ref.
     * <p>bo_bu_chronid_ref > 0</p>
     * Se è presente, allora recupera il suo system_id per poi passare al checkBoBuAssignNetCoServCo passandogli il documento e la variabile bo_bu_chronid_ref
     * <p>bo_bu_chronid_ref = 0</p>
     * Se non è presente, allora cerca di recuperare bu_all_chronid_ref che ricordo è una Lista di stringhe. Qui si aprono vari scenari:</br>
     * 1. Quando è soltanto uno solo - qui è semplicissimo. Si assegna o netco o servco.</br>
     * 2. Quando sono molti, allora verifica tutti gli system_id ricavati e né fa la comparizione con netCo - se sono uguali allora netco senza dubbio.
     * Ma quando almeno uno è diverso qui non si capisce e a log si presenta un WARNING.
     * Ma quando tutti non sono mappati - qui è semplice - servco senza dubbio.
     *
     * @param fetchedDocument documento su quale si sta lavora.
     */
    private void processDefaultDocumentClasses(Document fetchedDocument) {
        //Recupero il system_id del documento in anagrafica
        //Nonostante che sia stringa, a db risulta numeric.
        int bo_bu_chronid_ref = 0;
        if (fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref") != null) {
            Iterator<?> boBuChronidRefIterator = fetchSystemIdByBOBUChronicleIdRef(fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref"));
            if (boBuChronidRefIterator != null && boBuChronidRefIterator.hasNext()) {
                RepositoryRow repositoryRow = (RepositoryRow) boBuChronidRefIterator.next();
                bo_bu_chronid_ref = repositoryRow.getProperties().getInteger32Value("system_id");
            }
            logger.info("Found document: " + fetchedDocument.getProperties().getIdValue("ID") + " of class: " + fetchedDocument.getClassName()
                    + " with system_id={" + bo_bu_chronid_ref + "}");
        } else {
            logger.error("FOUND DOCUMENT: " + fetchedDocument.getProperties().getIdValue("ID") + " OF CLASS: " + fetchedDocument.getClassName()
                    + " WITH system_id = {" + bo_bu_chronid_ref + "} LOOKING FOR bu_all_chronid_ref");
        }
        //Se bo_bu_chronid_ref è presente quindi maggior di zero.
        if (bo_bu_chronid_ref > 0) {
            logger.info("Working document: " + fetchedDocument.getProperties().getIdValue("ID") + " of class: " + fetchedDocument.getClassName()
                    + " with system_id={" + bo_bu_chronid_ref + "}");
            // Verifico se nella mapping e` presente system_id trovato
            checkBoBuAssignNetCoServCo(fetchedDocument, bo_bu_chronid_ref);
        }
        ArrayList<Integer> buAllChronidRef = new ArrayList<>();
        if (bo_bu_chronid_ref == 0) {
            //Casistica in qui, system_id = 0 sul campo bo_bu_chronid_ref, si cerca di recuperare bu_all_chronid_ref
            Iterator<?> iterator = fetchSystemIdByBUALLChronicleIdRef(fetchedDocument.getProperties().getStringListValue("bu_all_chronid_ref"));
            while (iterator != null && iterator.hasNext()) {
                RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                buAllChronidRef.add(repositoryRow.getProperties().getInteger32Value("system_id"));
            }
            logger.info("Working document: " + fetchedDocument.getProperties().getIdValue("ID") + " of class: " + fetchedDocument.getClassName()
                    + " with system_id={" + buAllChronidRef + "}");
            ArrayList<Integer> systemIds = convertHashMapToArrayList(instance.getNetCo());
            // Se ho soltanto uno trovato - allora procedimento simile a quanto sopra
            if (buAllChronidRef.size() == 1) {
                int bu_all_chronid_ref = buAllChronidRef.get(0); //Tanto prendo il bu_all_chronid_ref di elemento trovato
                checkBoBuAssignNetCoServCo(fetchedDocument, bu_all_chronid_ref);
                //Se ho più di un risultato, allora
            }
            if (buAllChronidRef.size() > 1) {
                String netco_servco;
                //Verifico se le SYSTEM_ID (bu_all_chronid_ref) recuperate sono IDENTICI a SYSTEM_ID mappate in config.json sotto NetCoServCo,
                //Allora gli assegno NETCO_SECURITY. Esempio ho un documento con due soltanto system_id e tutte e due sono NETCO.
                if (systemIds.equals(buAllChronidRef)) {
                    netco_servco = instance.getNetCo().get(String.valueOf(buAllChronidRef.get(0)));
                    assignNetCoSecurityProxy(netco_servco, fetchedDocument);
                } else {
                    //Altrimenti uno x uno vedo le SYSTEM_ID.
                    int notFound = 0;
                    for (int i : buAllChronidRef) {
                        if (!instance.getNetCo().containsKey(String.valueOf(i))) {
                            //Incremento il contatore di bu_all_chronid_ref non trovati
                            notFound++;
                        }
                    }
                    //Se il contatore e` uguale alla dimensione di bu_all_chronid_ref recuperati prima - allora servco.
                    if (notFound == buAllChronidRef.size()) {
                        logger.info("BuAllChronidRef size == " + buAllChronidRef.size() + " and notFound == " + notFound);
                        netco_servco = instance.getServCo().get(0);
                        assignServCoSecurityProxy(netco_servco, fetchedDocument);
                    }
                    //Altrimenti, se si trova ad esempio uno di NETCO e gli altri SERVCO e/o non mappati - non si fa nulla.
                    else {
                        logger.error("POSSIBLE MAPPING OF SERVCO & NETCO DETECTED. THERE'S NOTHING TO DO!");
                    }
                }
            }
            //Caso in cui non trovo bo_bu_chronid_ref = 0 e bu_all_chronid_ref = 0
            //Per ora stampo msg di warning.
            if (buAllChronidRef.isEmpty()) {
                buALLandBoBu_empty(fetchedDocument, buAllChronidRef, bo_bu_chronid_ref);
            }
        }
    }

    /**
     * Metodo atto a risolvere la problematica qualora buAllChronidRef e bo_bu_chronid_ref risultino vuoti.
     *
     * @param fetchedDocument   documento su quale si sta attualmente lavorando
     * @param buAllChronidRef   una lista di system_id ossia bu_all_chronid_ref
     * @param bo_bu_chronid_ref variabile che contiene system_id
     */
    //TODO: Qualora bo_bu_chronid_ref e bu_all_chronid_ref sono vuoti... procedere al recupero di informazioni in qualche altra maniera.
    private void buALLandBoBu_empty(Document fetchedDocument, ArrayList<Integer> buAllChronidRef, int bo_bu_chronid_ref) {
        if (buAllChronidRef.isEmpty() && bo_bu_chronid_ref == 0) {
            logger.error("UNABLE TO PROCESS: " + fetchedDocument.getClassName()
                    + " DUE TO [bo_bu_chronid_ref]: " + fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref")
                    + " AND [bu_all_chronid_ref]: " + fetchedDocument.getProperties().getStringListValue("bu_all_chronid_ref") + " ARE NULL or EMPTY");
//            if (netCo.containsKey(String.valueOf(fetchedDocument.getProperties().getInteger32Value("system_id")))) {
//                String netco_servco = netCo.get(String.valueOf(fetchedDocument.getProperties().getInteger32Value("system_id")));
//                assignNetCoSecurityProxy(netco_servco, fetchedDocument);
//            } else {
//                assignServCoSecurityProxy(servCo.get(0), fetchedDocument);
//            }
        }
    }

    /**
     * Metodo che verifica la presenza su config.json del @param bo_bu_chronid_ref.
     * Se è presente allora al documento gli si assegna netco, diversamente servco.
     *
     * @param fetchedDocument   documento attuale su cui si lavora
     * @param bo_bu_chronid_ref in sostanza è un system_id, ed è importante per capire a quale security_proxy associare @fetchedDocument
     */
    private void checkBoBuAssignNetCoServCo(Document fetchedDocument, int bo_bu_chronid_ref) {
        String netco_servco;
        if (instance.getNetCo().containsKey(String.valueOf(bo_bu_chronid_ref))) {
            //Se si trova, allora mi faccio dare il affisso ossia il contenuto dopo =
            netco_servco = instance.getNetCo().get(String.valueOf(bo_bu_chronid_ref));
            assignNetCoSecurityProxy(netco_servco, fetchedDocument);
        } else {
            //Se non si trova bo_bu_chronid_ref mappato, allora gli si assegna servco_security a prescindere
            netco_servco = instance.getServCo().get(0);
            assignServCoSecurityProxy(netco_servco, fetchedDocument);
        }
    }

    private void assignNetCoSecurityProxy(String netco_servco, Document fetchedDocument) {
        securityProxySetUp(netco_servco, fetchedDocument, instance.getObjectStore());
    }

    private void assignServCoSecurityProxy(String netco_servco, Document fetchedDocument) {
        securityProxySetUp(netco_servco, fetchedDocument, instance.getObjectStore());
    }

    /**
     * Metodo che imposta security_proxy in base alla @param netco_servco
     *
     * @param netco_servco      variabile contenente {@code "_netco_security"} oppure {@code "_servco_security"}
     * @param fetchedDocument   documento attuale su cui si lavora
     * @param objectStoreSource variabile che contiene i dati sull'object store.
     */
    private static void securityProxySetUp(String netco_servco, Document fetchedDocument, ObjectStore objectStoreSource) {
        Id securityProxyIdValue = null;
        try {
            Id oldSecurityProxyId = fetchedDocument.getProperties().getIdValue(SECURITY_PROXY);
            Iterator<?> fetchedSecurityProxies = fetchSecurityProxies(fetchedDocument.getClassName() + netco_servco,
                    objectStoreSource);
            //Se c'è, allora sostituisco il valore del campo security_proxy della classe documentale con il riferimento del nuovo security_proxy.
            if (fetchedSecurityProxies != null && fetchedSecurityProxies.hasNext()) {
                RepositoryRow securityProxyRepository = (RepositoryRow) fetchedSecurityProxies.next();
                Properties securityProxyProperties = securityProxyRepository.getProperties();
                securityProxyIdValue = securityProxyProperties.getIdValue("ID");
                CustomObject securityProxyCustomObject = Factory.CustomObject.fetchInstance(objectStoreSource, securityProxyIdValue, null);
                ObjectReference securityProxy = securityProxyCustomObject.getObjectReference();
                //Verifico se nella classe documentale e` gia` presente la security_proxy nuova
                //Per capirci: se nella classe documentale c'e` gia` la security proxy nuova
                //Allora non si fa nulla.
                //Per capirci bis: se nella classe documentale c'e` gia` la security proxy
                //Ma che son diversi tipo: acq_pon_security != acq_pon_netco_security
                //Allora gli impasto quella nuova.
                if (!Objects.requireNonNull(oldSecurityProxyId).equals(securityProxyIdValue)) {
                    fetchedDocument.getProperties().putObjectValue(SECURITY_PROXY, securityProxy);
                    logger.info("Replacing old security_proxy Id:  " + oldSecurityProxyId +
                            " with new security_proxy Id: " + securityProxyIdValue);
                    fetchedDocument.save(RefreshMode.REFRESH);
                    logger.info("saved!");
                } else {
                    logger.info("There's already security_proxy existing on this document, security_proxy id: "
                            + securityProxyIdValue + " security_proxy id to be placed: "
                            + fetchedDocument.getProperties().getIdValue(SECURITY_PROXY));
                }
            } else {
                logger.error("THERE'S NO SECUROTY_PROXY: " + fetchedDocument.getClassName() + netco_servco + " CREATED");
            }
        } catch (Exception e) {
            logger.error("SOMETHING WENT WRONG ON SAVING [security_proxy]: {" + securityProxyIdValue + "} " +
                    "TO DOCUMENT: {" + fetchedDocument.getClassName() + "} id: {" + fetchedDocument.getProperties().getIdValue("Id") + "} ", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Funzione di conversione di una hashmap in array di interi
     *
     * @param netCoServCo hashmap che gli viene passato per la conversione
     * @return systemIds
     */
    private static ArrayList<Integer> convertHashMapToArrayList(HashMap<String, String> netCoServCo) {
        ArrayList<Integer> systemIds = new ArrayList<>();
        for (Map.Entry<String, String> pair : netCoServCo.entrySet()) {
            systemIds.add(Integer.valueOf(pair.getKey()));
        }
        return systemIds;
    }

    /**
     * Non fa altro che, salvare il security_proxy del padre al suo figlio.
     *
     * @param objectStoreSource      variabile che contiene i dati sull'object store.
     * @param fetchedDocument        documento attuale su cui si lavora
     * @param headByRelationByTailId variabile che contiene l'id del documento padre, quando si tratta di lavorare sugli allegati.
     */
    private void saveSecurityProxyToChildDocument(ObjectStore objectStoreSource, Document fetchedDocument,
                                                  String headByRelationByTailId) {
        ObjectReference netCoServCoSecurityProxy = null;
        try {
            Document document = Factory.Document.fetchInstance(objectStoreSource, headByRelationByTailId, null);
            logger.info("Looking for parent document based on current document Id:" +
                    fetchedDocument.getProperties().getIdValue("ID") + " found parent document id: " +
                    document.getProperties().getIdValue("ID"));
            Id securityProxy = document.getProperties().getIdValue(SECURITY_PROXY);
            CustomObject servCoNetCoCustomObject = Factory.CustomObject.fetchInstance(objectStoreSource, securityProxy, null);
            logger.info("Looking for security proxy of parent document id: " + document.getProperties().getIdValue("ID") +
                    "security proxy id found: " + securityProxy);
            netCoServCoSecurityProxy = servCoNetCoCustomObject.getObjectReference();
            fetchedDocument.getProperties().putObjectValue(SECURITY_PROXY, netCoServCoSecurityProxy);
            logger.info("Saving current document id: " + fetchedDocument.getProperties().getIdValue("ID") + " with a new security proxy value of parent document: " + securityProxy);
            fetchedDocument.save(RefreshMode.REFRESH);
            logger.info("Saved!");
        } catch (Exception exception) {
            logger.error("SOMETHING WENT WRONG ON SAVING security_proxy: {" + netCoServCoSecurityProxy + "} " +
                    "TO CHILD DOCUMENT : {" + fetchedDocument.getClassName() + "} id: {" + fetchedDocument.getProperties().getIdValue("ID") + "}", exception);
        }
    }

    /**
     * Restituisce gli elementi in base al parametro che gli viene passato
     *
     * @param buAllChronicleIdRef variabile che contiene uno o più elementi bu_all_chronid_ref
     * @return iterator - restituisce il risultato quando si trova il system_id
     */
    private Iterator<?> fetchSystemIdByBUALLChronicleIdRef(StringList buAllChronicleIdRef) {
        SearchScope searchScope = new SearchScope(instance.getObjectStore());
        if (buAllChronicleIdRef.size() == 1) {
            String querySource = "SELECT * FROM acq_anagrafica_bu WHERE Id = " + buAllChronicleIdRef.get(0).toString();
            SearchSQL searchSQL = new SearchSQL();
            searchSQL.setQueryString(querySource);
            return searchScope.fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
        } else if (buAllChronicleIdRef.size() > 1) {
            StringBuilder boAll = new StringBuilder();
            for (Object o : buAllChronicleIdRef) {
                boAll.append(o.toString()).append(" OR Id = ");
            }
            String querySource = "SELECT * FROM acq_anagrafica_bu WHERE Id = " + boAll.substring(0, boAll.length() - 9);
            SearchSQL searchSQL = new SearchSQL();
            searchSQL.setQueryString(querySource);
            return searchScope.fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
        }
        return null;
    }

    /**
     * Restituisce il risultato interrogando relazione sdi, gli si passa l'ID della coda per ricavarne l'ID di testa (ossia il padre).
     *
     * @param tailId variabile di coda su quale si effettua la ricerca presso la relazione sdi
     * @return iterator
     */
    private Iterator<?> fetchHeadIdByRelationTailId(String tailId) {
        String querySource = "SELECT [head_chronicle_id] FROM [acq_relation] WHERE [tail_chronicle_id] = " + tailId;
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(querySource);
        return new SearchScope(instance.getObjectStore()).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }

    /**
     * Restituisce il system_id in base alla variabile bo_bu_chronid_ref
     *
     * @param boBuChronicleIdRef variabile che contiene in sé GUID su quale effettuare una query.
     * @return iterator
     */
    private Iterator<?> fetchSystemIdByBOBUChronicleIdRef(String boBuChronicleIdRef) {
        String querySource = "SELECT [system_id] FROM [acq_anagrafica_bu] WHERE [id] = " + boBuChronicleIdRef;
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(querySource);
        return new SearchScope(instance.getObjectStore()).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }

    /**
     * Restituisce security_proxy ricercato.
     *
     * @param securityProxies   variabile della security_proxy da ricercare
     * @param objectStoreSource contiene i dati dell'object store
     * @return iterator
     */
    private static Iterator<?> fetchSecurityProxies(String securityProxies, ObjectStore objectStoreSource) {
        String querySource = "SELECT * FROM [acq_security_proxy] WHERE [codice] = " + securityProxies;
        SearchSQL searchSQL = new SearchSQL();
        searchSQL.setQueryString(querySource);
        return new SearchScope(objectStoreSource).fetchRows(searchSQL, null, null, Boolean.TRUE).iterator();
    }

    /**
     * Restituisce la lista delle classi documentali, folder o custom object passandogli per query o senza tramite docClass.
     *
     * @param docClass          una variabile configurabile su config.json, DEVE contenere almeno un dato.
     *                          I dati validi sono: Document, CustomObject o Folder.
     *                          Attualmente è gestito Document.
     * @param query             una variabile della query indicata nel config.json, può essere anche vuota.
     * @param objectStoreSource contiene i dati dell'object store
     * @return iterator
     */
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
