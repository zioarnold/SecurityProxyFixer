package r2u.tools.entities;

import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.query.RepositoryRow;
import org.apache.log4j.Logger;
import r2u.tools.config.Configurator;
import r2u.tools.utils.Assigner;
import r2u.tools.utils.Checker;
import r2u.tools.utils.Converters;
import r2u.tools.utils.DataFetcher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class Attachments {
    private static final Configurator instance = Configurator.getInstance();
    private final static Logger logger = Logger.getLogger(Attachments.class.getName());

    /**
     * Metodo atto a: avendo allegato si ricava il suo documento padre.
     * Dal documento padre si estraggono le bo_bu_chronid_ref o bu_all_chronid_ref
     *
     * @param fetchedDocument una allegato da processare
     * {@link #saveSecurityProxyToChildDocument(Document, Document) saveSecurityProxyToChildDocument}
     */
    public static void processAttachments(Document fetchedDocument) {
        try {
            logger.info("Found document: " + fetchedDocument.getProperties().getIdValue("ID") + " of class: " + fetchedDocument.getClassName());
            //Qui recupero l'ID del documento padre dato che, siamo in gestione degli allegati.
            //Quindi recupero l'ID del padre per poi recuperare il suo security proxy da impostare all'allegato
            //Restituisce una string dell'ID dell documento, se è vuota, allora non si fa nulla,
            //Se non è vuota - allora si salva il documento con security proxy del padre
            Iterator<?> relationIterator = DataFetcher.fetchHeadIdByRelationTailId(fetchedDocument.getProperties().getIdValue("ID").toString(), instance.getObjectStore());
            String headByRelationByTailId = "";
            if (relationIterator != null && relationIterator.hasNext()) {
                RepositoryRow repositoryRow = (RepositoryRow) relationIterator.next();
                headByRelationByTailId = String.valueOf(repositoryRow.getProperties().getIdValue("head_chronicle_id"));
            }
            //BUGFIX: Refactoring dell'allegato. Devo valutare quale security metter in base alla bu del padre
            //Trovando il padre, lo recupero per recuperare le bo_bu_chronid_ref o bu_all_chronid_ref
            if (!headByRelationByTailId.isEmpty()) {
                Document parentDocumentId = Factory.Document.fetchInstance(instance.getObjectStore(), headByRelationByTailId, null);
                saveSecurityProxyToChildDocument(parentDocumentId, fetchedDocument);
            } else {
                logger.warn("There's no parent found for : " + fetchedDocument.getClassName() + " document id: " + fetchedDocument.getProperties().getIdValue("ID"));
                BufferedWriter documentWithoutParent = new BufferedWriter(new FileWriter(fetchedDocument.getClassName() + "_no_parent.txt", true));
                documentWithoutParent.write(fetchedDocument.getClassName() + ";" + fetchedDocument.getProperties().getIdValue("ID") + "\n");
                documentWithoutParent.close();
            }
        } catch (IOException e) {
            logger.error("UNABLE TO WRITE TO FILE: " + fetchedDocument.getClassName() + "_no_parent.txt", e);
        }
    }

    /**
     * Non fa altro che, salvare il security_proxy del padre al suo figlio.
     *
     * @param parentDocumentId documento padre dal quale ottengo informazioni
     * @param childDocument    documento figlio al quale poi assegnerò security_proxy in base ai dati del padre
     */
    private static void saveSecurityProxyToChildDocument(Document parentDocumentId,
                                                         Document childDocument) {
        logger.info("Working with parentDocument/id: " + parentDocumentId.getClassName() + "/" + parentDocumentId.getProperties().getIdValue("ID")
                + " of childDocument/id: " + childDocument.getClassName() + "/" + childDocument.getProperties().getIdValue("ID"));
        int boBuChronidRef = 0;
        ArrayList<Integer> buAllChronidRef = new ArrayList<>();
        //Recupero il system_id del documento in anagrafica
        //Nonostante che sia stringa, a db risulta numeric.
        if (parentDocumentId.getProperties().isPropertyPresent("bo_bu_chronid_ref") &&
                parentDocumentId.getProperties().getStringValue("bo_bu_chronid_ref") != null) {
            Iterator<?> boBuChronidRefIterator = DataFetcher.fetchSystemIdByBOBUChronicleIdRef(parentDocumentId.getProperties().getStringValue("bo_bu_chronid_ref"),
                    instance.getObjectStore());
            if (boBuChronidRefIterator != null && boBuChronidRefIterator.hasNext()) {
                RepositoryRow repositoryRow = (RepositoryRow) boBuChronidRefIterator.next();
                boBuChronidRef = repositoryRow.getProperties().getInteger32Value("system_id");
            }
            logger.info("Parent document system_id={" + boBuChronidRef + "}");
        } else {
            logger.error("PARENT DOCUMENT system_id={" + boBuChronidRef + "} LOOKING FOR bu_all_chronid_ref");
        }
        //Se bo_bu_chronid_ref è presente quindi maggior di zero.
        if (boBuChronidRef > 0) {
            // Verifico se nella mapping e` presente system_id trovato
            Checker.checkBoBuAssignNetCoServCo(childDocument, boBuChronidRef);
        }
        if (boBuChronidRef == 0) {
            //Controllo se campo e` presente.
            if (parentDocumentId.getProperties().isPropertyPresent("bu_all_chronid_ref")
                    && parentDocumentId.getProperties().getStringListValue("bu_all_chronid_ref") != null) {
                //Casistica in qui, system_id = 0 sul campo bo_bu_chronid_ref, si cerca di recuperare bu_all_chronid_ref
                Iterator<?> iterator = DataFetcher.fetchSystemIdByBUALLChronicleIdRef(parentDocumentId.getProperties().getStringListValue("bu_all_chronid_ref"),
                        instance.getObjectStore());
                while (iterator != null && iterator.hasNext()) {
                    RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                    buAllChronidRef.add(repositoryRow.getProperties().getInteger32Value("system_id"));
                }
                ArrayList<Integer> systemIds = Converters.getIntegerFromHashMap(instance.getNetCo());
                // Se ho soltanto uno trovato - allora procedimento simile a quanto sopra
                if (buAllChronidRef.size() == 1) {
                    //Tanto prendo il bu_all_chronid_ref di elemento trovato
                    Checker.checkBoBuAssignNetCoServCo(childDocument, buAllChronidRef.get(0));
                    //Se ho più di un risultato, allora
                }
                if (buAllChronidRef.size() > 1) {
                    String netco_servco;
                    //Verifico se le SYSTEM_ID (bu_all_chronid_ref) recuperate sono IDENTICI a SYSTEM_ID mappate in config.json sotto NetCoServCo,
                    //Allora gli assegno NETCO_SECURITY. Esempio ho un documento con due soltanto system_id e tutte e due sono NETCO.
                    if (systemIds.equals(buAllChronidRef)) {
                        netco_servco = instance.getNetCo().get(String.valueOf(buAllChronidRef.get(0)));
                        Assigner.assignNetCoSecurityProxy(netco_servco, childDocument);
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
                            Assigner.assignServCoSecurityProxy(netco_servco, childDocument);
                        }
                        //Altrimenti, se si trova ad esempio uno di NETCO e gli altri SERVCO e/o non mappati - non si fa nulla.
                        else {
                            logger.error("POSSIBLE MAPPING OF SERVCO & NETCO DETECTED. THERE'S NOTHING TO DO!");
                        }
                    }
                }
            } else {
                logger.error("PROPERTY bu_all_chronid_ref IS ABSENT ON PARENT DOCUMENT: " +
                        parentDocumentId.getClassName() + "/" + parentDocumentId.getProperties().getIdValue("ID") + " NOTHING TO DO! " +
                        childDocument.getClassName() + "/" + childDocument.getProperties().getIdValue("ID") + " UNCHANGED");
            }
        }

        //Caso in cui non trovo bo_bu_chronid_ref = 0 e bu_all_chronid_ref = 0
        //Per ora stampo msg di errore.
        if (buAllChronidRef.isEmpty() && boBuChronidRef == 0) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(parentDocumentId.getClassName() + "_generic.txt", true));
                if (parentDocumentId.getProperties().isPropertyPresent("bo_bu_chronid_ref")) {
                    writer.write("UNABLE TO PROCESS: " + parentDocumentId.getClassName()
                            + " DUE TO [bo_bu_chronid_ref]: " + parentDocumentId.getProperties().getStringValue("bo_bu_chronid_ref")
                            + " AND [bu_all_chronid_ref]: " + buAllChronidRef + " ARE NULL or EMPTY! PARENT ID DOC: " + parentDocumentId.getProperties().getIdValue("ID") + "\n");
                    logger.error("UNABLE TO PROCESS: " + parentDocumentId.getClassName()
                            + " DUE TO [bo_bu_chronid_ref]: " + parentDocumentId.getProperties().getStringValue("bo_bu_chronid_ref")
                            + " AND [bu_all_chronid_ref]: " + buAllChronidRef + " ARE NULL or EMPTY! PARENT ID DOC: " + parentDocumentId.getProperties().getIdValue("ID"));
                } else {
                    writer.write("UNABLE TO PROCESS: " + parentDocumentId.getClassName()
                            + " DUE TO [bo_bu_chronid_ref]: " + boBuChronidRef
                            + " AND [bu_all_chronid_ref]: " + buAllChronidRef + " ARE NULL or EMPTY! PARENT ID DOC: " + parentDocumentId.getProperties().getIdValue("ID") + "\n");
                    logger.error("UNABLE TO PROCESS: " + parentDocumentId.getClassName()
                            + " DUE TO [bo_bu_chronid_ref]: " + boBuChronidRef
                            + " AND [bu_all_chronid_ref]: " + buAllChronidRef + " ARE NULL or EMPTY! PARENT ID DOC: " + parentDocumentId.getProperties().getIdValue("ID"));
                }
                writer.close();
            } catch (IOException e) {
                logger.error("UNABLE TO WRITE TO FILE: " + childDocument.getClassName() + "_generic.txt", e);
            }
        }
    }
}
