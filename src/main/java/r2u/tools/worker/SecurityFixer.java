package r2u.tools.worker;

import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.*;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.property.Properties;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.util.Id;
import com.filenet.api.util.UserContext;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import r2u.tools.config.Configurator;
import r2u.tools.constants.Constants;
import r2u.tools.utils.Converters;
import r2u.tools.utils.DataFetcher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;

import static r2u.tools.constants.Constants.SECURITY_PROXY;

public class SecurityFixer {
    /**
     * Questi sono i field globali per permettere di accedervi ai loro valori fuori dei metodi ma all'interno della classe stessa.
     */
    private final static Logger logger = Logger.getLogger(SecurityFixer.class.getName());
    private final Configurator instance = Configurator.getInstance();
    private final HashMap<String, String> readOnlyDocuments = new HashMap<>();

    /**
     * Metodo che fa il lavoretto nel ricavare le classi documentali e se sono lavorabili su json, flag è true.
     * Chiamare altri metodi di smistamento per netco o servco.
     */
    public void startSecurityFix() {
        long startTime, endTime;
        String[] doc = instance.getDocumentClass().split(",");
        logger.info("Starting security fix...");
        startTime = System.currentTimeMillis();
        Document fetchedDocument = null;
        try {
            for (String docClass : doc) {
                switch (docClass) {
                    case "CustomObject":
                    case "Folder": {
                        logger.warn(docClass + " - atm isn't implemented.");
                    }
                    break;
                    case "Document": {
                        if (!instance.getQuery().isEmpty()) {
                            //Ottengo le informazioni dalla query impostata su config.json
                            //Se ho dei risultati allora li processo uno x uno
                            Iterator<?> iterator = DataFetcher.fetchDataByQuery(instance.getQuery(), instance.getObjectStore());
                            if (iterator != null) {
                                while (iterator.hasNext()) {
                                    try {
                                        RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                                        Properties properties = repositoryRow.getProperties();
                                        fetchedDocument = Factory.Document.fetchInstance(instance.getObjectStore(), properties.getIdValue("ID").toString(), null);
                                        if (instance.getDocumentMap().containsKey(fetchedDocument.getClassName())) {
                                            //Controllo se bisogna lavorare sulla classe documentale quindi flag = true
                                            if (instance.getDocumentMap().get(fetchedDocument.getClassName())) {
                                                preProcessDocuments(fetchedDocument);
                                            }
                                        } else {
                                            logger.error("NO " + fetchedDocument.getClassName() + " MAPPED IN config.json AT objectClasses -> Document");
                                        }
                                    } catch (EngineRuntimeException e) {
                                        //Produco dei file in cui vengono scritti le informazioni sui documenti che non sono stati processati
                                        //Per motivo di campo read-only quando potrebbe non esserlo
                                        if (e.getExceptionCode().getErrorId().equals("FNRCE0057")) {
                                            readOnlyDocuments.put(Objects.requireNonNull(fetchedDocument).getClassName(), fetchedDocument.getProperties().getIdValue("Id").toString());
                                            logger.error("SOMETHING WENT WRONG... ", e);
                                            BufferedWriter readOnlySecurityProxyWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName()
                                                    + "_read_only_security_proxy.txt", true));
                                            readOnlySecurityProxyWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID").toString() + "\n");
                                            readOnlySecurityProxyWriter.close();
                                            //Per via del dato sporco
                                        } else if (e.getExceptionCode().getErrorId().equals("FNRCA0024")) {
                                            logger.error("SOMETHING WENT WRONG... ", e);
                                            BufferedWriter emptyBuAllChronIdWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName()
                                                    + "_empty_bu_all_chronid_ref.txt", true));
                                            emptyBuAllChronIdWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID").toString() + "\n");
                                            emptyBuAllChronIdWriter.close();
                                            //Per via di errori non gestiti
                                        } else if (e.getExceptionCode().getErrorId().equals("FNRCE0046")) {
                                            logger.error("SOMETHING WENT WRONG... ", e);
                                            BufferedWriter parentSecProxyValueNullWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName() + "_parent_security_proxy_null.txt", true));
                                            parentSecProxyValueNullWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID") + "\n");
                                            parentSecProxyValueNullWriter.close();
                                        } else {
                                            logger.error("SOMETHING WENT WRONG... ", e);
                                            PrintWriter unManagedErrorsWriter = new PrintWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName() + "_unmanaged_errors.txt", true));
                                            e.printStackTrace(unManagedErrorsWriter);
                                            unManagedErrorsWriter.close();
                                        }
                                        //Qualora dovesse capitare errore non gestito
                                    } catch (Exception exception) {
                                        BufferedWriter guidClassListUnmanagedErrorsWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName() + "_unmanaged_errors_guid_list_class.txt", true));
                                        guidClassListUnmanagedErrorsWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID").toString() + "\n");
                                        guidClassListUnmanagedErrorsWriter.close();
                                        PrintWriter unManagedErrorsWriter = new PrintWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName() + "_unmanaged_errors.txt", true));
                                        exception.printStackTrace(unManagedErrorsWriter);
                                        unManagedErrorsWriter.close();
                                    }
                                }
                            } else {
                                logger.error("UNABLE TO FETCH DATA FROM : " + fetchedDocument + " fetchDataByQuery() RETURNED NULL! Aborting...");
                                if (readOnlyDocuments.isEmpty()) {
                                    if (UserContext.get() != null) {
                                        UserContext.get().popSubject();
                                    }
                                }
                                endTime = System.currentTimeMillis();
                                logger.info("Security fixer terminated within: " + DurationFormatUtils.formatDuration(endTime - startTime, Constants.dateTimeFormat, true));
                                System.exit(-1);
                            }
                        } else {
                            for (String document : instance.getDocumentClassList()) {
                                logger.info("Working with docClass " + document);
                                Iterator<?> iterator = DataFetcher.fetchRowsByClass(document, instance.getObjectStore());
                                if (iterator != null) {
                                    while (iterator.hasNext()) {
                                        try {
                                            RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                                            Properties properties = repositoryRow.getProperties();
                                            fetchedDocument = Factory.Document.fetchInstance(instance.getObjectStore(), properties.getIdValue("ID").toString(), null);
                                            //Controllo se la classe documentale e` mappata
                                            if (instance.getDocumentMap().containsKey(fetchedDocument.getClassName())) {
                                                //Controllo se bisogna lavorare sulla classe documentale quindi flag = true
                                                if (instance.getDocumentMap().get(fetchedDocument.getClassName())) {
                                                    preProcessDocuments(fetchedDocument);
                                                }
                                            } else {
                                                logger.error("NO " + fetchedDocument.getClassName() + " MAPPED IN config.json AT objectClasses -> Document");
                                            }
                                        } catch (EngineRuntimeException e) {
                                            //Produco dei file in cui vengono scritti le informazioni sui documenti che non sono stati processati
                                            //Per motivo di campo read-only quando potrebbe non esserlo
                                            if (e.getExceptionCode().getErrorId().equals("FNRCE0057")) {
                                                readOnlyDocuments.put(Objects.requireNonNull(fetchedDocument).getClassName(), fetchedDocument.getProperties().getIdValue("Id").toString());
                                                logger.error("SOMETHING WENT WRONG... ", e);
                                                BufferedWriter readOnlySecurityProxyWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName()
                                                        + "_read_only_security_proxy.txt", true));
                                                readOnlySecurityProxyWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID").toString() + "\n");
                                                readOnlySecurityProxyWriter.close();
                                                //Per via del dato sporco
                                            } else if (e.getExceptionCode().getErrorId().equals("FNRCA0024")) {
                                                logger.error("SOMETHING WENT WRONG... ", e);
                                                BufferedWriter emptyBuAllChronIdWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName()
                                                        + "_empty_bu_all_chronid_ref.txt", true));
                                                emptyBuAllChronIdWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID").toString() + "\n");
                                                emptyBuAllChronIdWriter.close();
                                                //Per via di errori non gestiti
                                            } else if (e.getExceptionCode().getErrorId().equals("FNRCE0046")) {
                                                logger.error("SOMETHING WENT WRONG... ", e);
                                                BufferedWriter parentSecProxyValueNullWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName() + "_parent_security_proxy_null.txt", true));
                                                parentSecProxyValueNullWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID") + "\n");
                                                parentSecProxyValueNullWriter.close();
                                            } else {
                                                logger.error("SOMETHING WENT WRONG... ", e);
                                                PrintWriter unManagedErrorsWriter = new PrintWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName() + "_unmanaged_errors.txt", true));
                                                e.printStackTrace(unManagedErrorsWriter);
                                                unManagedErrorsWriter.close();
                                            }
                                            //Qualora dovesse capitare errore non gestito
                                        } catch (Exception exception) {
                                            BufferedWriter guidClassListUnmanagedErrorsWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName() + "_unmanaged_errors_guid_list_class.txt", true));
                                            guidClassListUnmanagedErrorsWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID").toString() + "\n");
                                            guidClassListUnmanagedErrorsWriter.close();
                                            PrintWriter unManagedErrorsWriter = new PrintWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName() + "_unmanaged_errors.txt", true));
                                            exception.printStackTrace(unManagedErrorsWriter);
                                            unManagedErrorsWriter.close();
                                        }
                                    }
                                } else {
                                    logger.error("UNABLE TO FETCH DATA FROM : " + document + " fetchRowsByClass() RETURNED NULL! Aborting...");
                                    if (UserContext.get() != null) {
                                        UserContext.get().popSubject();
                                    }
                                    endTime = System.currentTimeMillis();
                                    logger.info("Security fixer terminated within: " + DurationFormatUtils.formatDuration(endTime - startTime, Constants.dateTimeFormat, true));
                                    System.exit(-1);
                                }
                            }
                        }
                    }
                    break;
                }
            }
            endTime = System.currentTimeMillis();
            logger.info("Security fixer terminated within: " + DurationFormatUtils.formatDuration(endTime - startTime, Constants.dateTimeFormat, true));
            if (readOnlyDocuments.isEmpty()) {
                if (UserContext.get() != null) {
                    UserContext.get().popSubject();
                }
            } else {
                reWorkSecurityFix();
            }
        } catch (IOException e) {
            if (UserContext.get() != null) {
                UserContext.get().popSubject();
            }
            logger.error("UNABLE TO WRITE TO FILE: " + Objects.requireNonNull(fetchedDocument).getClassName() + "_generic.txt", e);
        }
    }

    /**
     * Metodo preProcessore atto a determinare come lavorare quando gli arriva un certo tipo di documento
     *
     * @param fetchedDocument documento attuale su cui bisogna lavorare
     */
    private void preProcessDocuments(Document fetchedDocument) {
        try {
            RepositoryRow repositoryRow;
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
                    Iterator<?> relationIterator = DataFetcher.fetchHeadIdByRelationTailId(fetchedDocument.getProperties().getIdValue("ID").toString(), instance.getObjectStore());
                    String headByRelationByTailId = "";
                    if (relationIterator != null && relationIterator.hasNext()) {
                        repositoryRow = (RepositoryRow) relationIterator.next();
                        headByRelationByTailId = String.valueOf(repositoryRow.getProperties().getIdValue("head_chronicle_id"));
                    }
                    if (!headByRelationByTailId.isEmpty()) {
                        saveSecurityProxyToChildDocument(instance.getObjectStore(), fetchedDocument, headByRelationByTailId);
                    } else {
                        logger.warn("There's no parent found for : " + fetchedDocument.getClassName() + " document id: " + fetchedDocument.getProperties().getIdValue("ID"));
                        BufferedWriter documentWithoutParent = new BufferedWriter(new FileWriter(fetchedDocument.getClassName() + "_no_parent.txt", true));
                        documentWithoutParent.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID") + "\n");
                        documentWithoutParent.close();
                    }
                }
                break;
            }
        } catch (IOException e) {
            logger.error("UNABLE TO WRITE TO FILE: " + fetchedDocument.getClassName() + "_no_parent.txt", e);
        }
    }

    /**
     * Metodo atto a rifare il giro quando capita l'errore READ_ONLY
     */
    public void reWorkSecurityFix() {
        ArrayList<String> list = Converters.getStringsFromHashMap(readOnlyDocuments);
        Document document = null;
        try {
            if (!list.isEmpty()) {
                long startTime, endTime;
                logger.info("Starting reWorkSecurityFix...");
                startTime = System.currentTimeMillis();
                for (String docClassId : list) {
                    try {
                        Iterator<?> iterator = DataFetcher.reWorkFetchRows(docClassId.split(";")[0], docClassId.split(";")[1], instance.getObjectStore());
                        if (iterator != null && iterator.hasNext()) {
                            RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                            Properties properties = repositoryRow.getProperties();
                            document = Factory.Document.fetchInstance(instance.getObjectStore(), properties.getIdValue("ID").toString(), null);
                            preProcessDocuments(document);
                        }
                    } catch (EngineRuntimeException e) {
                        //Produco dei file in cui vengono scritti le informazioni sui documenti che non sono stati processati
                        //Per motivo di campo read-only quando potrebbe non esserlo
                        if (e.getExceptionCode().getErrorId().equals("FNRCE0057")) {
                            logger.error("SOMETHING WENT WRONG... ", e);
                            BufferedWriter readOnlySecurityProxyWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(document).getClassName()
                                    + "_read_only_security_proxy_retried.txt", true));
                            readOnlySecurityProxyWriter.write(document.getClassName() + ";" + document.getProperties().getIdValue("ID").toString() + "\n");
                            readOnlySecurityProxyWriter.close();
                            //Per via del dato sporco
                        } else if (e.getExceptionCode().getErrorId().equals("FNRCA0024")) {
                            logger.error("SOMETHING WENT WRONG... ", e);
                            BufferedWriter emptyBuAllChronIdWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(document).getClassName()
                                    + "_empty_bu_all_chronid_ref_retried.txt", true));
                            emptyBuAllChronIdWriter.write(document.getClassName() + ";" + document.getProperties().getIdValue("ID").toString() + "\n");
                            emptyBuAllChronIdWriter.close();
                            //Per via di errori non gestiti
                        } else if (e.getExceptionCode().getErrorId().equals("FNRCE0046")) {
                            logger.error("SOMETHING WENT WRONG... ", e);
                            BufferedWriter parentSecProxyValueNullWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(document).getClassName() + "_parent_security_proxy_null_retried.txt", true));
                            parentSecProxyValueNullWriter.write(document.getClassName() + ";" + document.getProperties().getIdValue("ID") + "\n");
                            parentSecProxyValueNullWriter.close();
                        } else {
                            logger.error("SOMETHING WENT WRONG... ", e);
                            BufferedWriter guidClassListUnmanagedErrorsWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(document).getClassName() + "_unmanaged_errors_guid_list_class.txt", true));
                            guidClassListUnmanagedErrorsWriter.write(document.getClassName() + ";" + document.getProperties().getIdValue("ID").toString() + "\n");
                            guidClassListUnmanagedErrorsWriter.close();
                            PrintWriter unManagedErrorsWriter = new PrintWriter(new FileWriter(Objects.requireNonNull(document).getClassName() + "_unmanaged_errors_retried.txt", true));
                            e.printStackTrace(unManagedErrorsWriter);
                            unManagedErrorsWriter.close();
                        }
                    }
                }
                if (UserContext.get() != null) {
                    UserContext.get().popSubject();
                }
                endTime = System.currentTimeMillis();
                logger.info("reWorkSecurityFix terminated within: " + DurationFormatUtils.formatDuration(endTime - startTime, Constants.dateTimeFormat, true));
            }
        } catch (IOException e) {
            if (UserContext.get() != null) {
                UserContext.get().popSubject();
            }
            logger.error("UNABLE TO WRITE TO FILE:", e);
        }
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
        if (fetchedDocument.getProperties().isPropertyPresent("bo_bu_chronid_ref") &&
                fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref") != null) {
            Iterator<?> boBuChronidRefIterator = DataFetcher.fetchSystemIdByBOBUChronicleIdRef(fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref"), instance.getObjectStore());
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
            Iterator<?> iterator = DataFetcher.fetchSystemIdByBUALLChronicleIdRef(fetchedDocument.getProperties().getStringListValue("bu_all_chronid_ref"), instance.getObjectStore());
            while (iterator != null && iterator.hasNext()) {
                RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                buAllChronidRef.add(repositoryRow.getProperties().getInteger32Value("system_id"));
            }
            logger.info("Working document: " + fetchedDocument.getProperties().getIdValue("ID") + " of class: " + fetchedDocument.getClassName()
                    + " with system_id={" + buAllChronidRef + "}");
            ArrayList<Integer> systemIds = Converters.getIntegerFromHashMap(instance.getNetCo());
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
     * N.B. Attualmente non si fa nulla, perciò si stampa msg a video e scrittura su file log
     *
     * @param fetchedDocument   documento su quale si sta attualmente lavorando
     * @param buAllChronidRef   una lista di system_id ossia bu_all_chronid_ref
     * @param bo_bu_chronid_ref variabile che contiene system_id
     */
    private void buALLandBoBu_empty(Document fetchedDocument, ArrayList<Integer> buAllChronidRef,
                                    int bo_bu_chronid_ref) {
        try {
            if (buAllChronidRef.isEmpty() && bo_bu_chronid_ref == 0) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fetchedDocument.getClassName() + "_generic.txt", true));
                writer.write("UNABLE TO PROCESS: " + fetchedDocument.getClassName()
                        + " DUE TO [bo_bu_chronid_ref]: " + fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref")
                        + " AND [bu_all_chronid_ref]: " + buAllChronidRef + " ARE NULL or EMPTY! ID DOC: " + fetchedDocument.getProperties().getIdValue("ID") + "\n");
                logger.error("UNABLE TO PROCESS: " + fetchedDocument.getClassName()
                        + " DUE TO [bo_bu_chronid_ref]: " + fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref")
                        + " AND [bu_all_chronid_ref]: " + buAllChronidRef + " ARE NULL or EMPTY ID DOC: " + fetchedDocument.getProperties().getIdValue("ID"));
                writer.close();
            }
        } catch (IOException e) {
            logger.error("UNABLE TO WRITE TO FILE: " + fetchedDocument.getClassName() + "_generic.txt", e);
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
     * Metodo che imposta security_proxy in base alla variabile passatogli netco_servco
     *
     * @param netco_servco    variabile contenente {@code "_netco_security"} oppure {@code "_servco_security"}
     * @param fetchedDocument documento attuale su cui si lavora
     * @param objectStore     variabile che contiene i dati sull'object store.
     */
    private static void securityProxySetUp(String netco_servco, Document fetchedDocument, ObjectStore
            objectStore) {
        //Vecchio ID del security proxy
        Id oldSecurityProxyId = fetchedDocument.getProperties().getIdValue(SECURITY_PROXY);
        Iterator<?> fetchedSecurityProxies = DataFetcher.fetchSecurityProxies(fetchedDocument.getClassName() + netco_servco,
                objectStore);
        //Se c'è, allora sostituisco il valore del campo security_proxy della classe documentale con il riferimento del nuovo security_proxy.
        if (fetchedSecurityProxies != null && fetchedSecurityProxies.hasNext()) {
            RepositoryRow securityProxyRepository = (RepositoryRow) fetchedSecurityProxies.next();
            Properties securityProxyProperties = securityProxyRepository.getProperties();
            //Nuovo ID del security proxy, sia netco o servco
            Id securityProxyIdValue = securityProxyProperties.getIdValue("ID");
            CustomObject securityProxyCustomObject = Factory.CustomObject.fetchInstance(objectStore, securityProxyIdValue, null);
            ObjectReference securityProxy = securityProxyCustomObject.getObjectReference();
            //Gestione del caso in cui, nel documento il campo security_proxy e` vuoto e quindi NullPointer.
            //Quindi lo si imposta.
            if (oldSecurityProxyId == null) {
                logger.warn("There's no security_proxy found on document id: " + fetchedDocument.getProperties().getIdValue("ID")
                        + " security_proxy is null, trying insert a new security_proxy id: " + securityProxyIdValue);
                fetchedDocument.getProperties().putObjectValue(SECURITY_PROXY, securityProxy);
                fetchedDocument.save(RefreshMode.REFRESH);
            }
            //Verifico se nella classe documentale e` gia` presente la security_proxy nuova
            //Per capirci: se nella classe documentale c'e` gia` la security proxy nuova
            //Allora non si fa nulla.
            //Per capirci bis: se nella classe documentale c'e` gia` la security proxy
            //Ma che son diversi tipo: acq_pon_security != acq_pon_netco_security
            //Allora gli impasto quella nuova.
            if (oldSecurityProxyId != null && !oldSecurityProxyId.equals(securityProxyIdValue)) {
                logger.info("Replacing old security_proxy Id:  " + oldSecurityProxyId +
                        " with new security_proxy Id: " + securityProxyIdValue);
                fetchedDocument.getProperties().putObjectValue(SECURITY_PROXY, securityProxy);
                fetchedDocument.save(RefreshMode.REFRESH);
                logger.info("saved!");
            }
        } else {
            logger.error("THERE'S NO SECURITY_PROXY: " + fetchedDocument.getClassName() + netco_servco + " CREATED");
        }
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
        try {
            ObjectReference netCoServCoSecurityProxy = null;
            PrintWriter unManagedError = new PrintWriter(new FileWriter(fetchedDocument.getClassName() + "_unmanaged_errors.txt", true));
            try {
                Document document = Factory.Document.fetchInstance(objectStoreSource, headByRelationByTailId, null);
                logger.info("Looking for parent document based on current document Id:" +
                        fetchedDocument.getProperties().getIdValue("ID") + " found parent document id: " +
                        document.getProperties().getIdValue("ID"));
                Id securityProxy = document.getProperties().getIdValue(SECURITY_PROXY);
                CustomObject servCoNetCoCustomObject = Factory.CustomObject.fetchInstance(objectStoreSource, securityProxy, null);
                logger.info("Looking for security proxy of parent document id: " + document.getProperties().getIdValue("ID") +
                        " security proxy id found: " + securityProxy);
                netCoServCoSecurityProxy = servCoNetCoCustomObject.getObjectReference();
                if (netCoServCoSecurityProxy != null || !servCoNetCoCustomObject.toString().equals("null")) {
                    fetchedDocument.getProperties().putObjectValue(SECURITY_PROXY, netCoServCoSecurityProxy);
                    logger.info("Saving current document id: " + fetchedDocument.getProperties().getIdValue("ID") + " with a new security proxy value of parent document: " + securityProxy);
                    fetchedDocument.save(RefreshMode.REFRESH);
                    logger.info("saved!");
                } else {
                    BufferedWriter nullParentSecurityProxyWriter = new BufferedWriter(new FileWriter(fetchedDocument.getClassName() + "_parent_security_proxy_null.txt", true));
                    nullParentSecurityProxyWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID").toString() + "\n");
                    nullParentSecurityProxyWriter.close();
                }
            } catch (Exception exception) {
                logger.error("SOMETHING WENT WRONG ON SAVING security_proxy: {" + netCoServCoSecurityProxy + "} " +
                        "TO CHILD DOCUMENT : {" + fetchedDocument.getClassName() + "} id: " + fetchedDocument.getProperties().getIdValue("ID"), exception);
                BufferedWriter guidClassListUnmanagedErrorsWriter = new BufferedWriter(new FileWriter(fetchedDocument.getClassName() + "_unmanaged_errors_guid_list_class.txt", true));
                guidClassListUnmanagedErrorsWriter.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID").toString() + "\n");
                guidClassListUnmanagedErrorsWriter.close();
                exception.printStackTrace(unManagedError);
            }
            unManagedError.close();
        } catch (IOException e) {
            logger.error("UNABLE TO WRITE TO FILE: " + fetchedDocument.getClassName() + "_unmanaged_errors.txt", e);
        }
    }
}