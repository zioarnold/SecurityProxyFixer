package r2u.tools.entities;

import com.filenet.api.core.Document;
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

public class Documents {
    private static final Configurator instance = Configurator.getInstance();
    private final static Logger logger = Logger.getLogger(Documents.class.getName());

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
    public static void processDefaultDocumentClasses(Document fetchedDocument) {
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
                    + " WITH system_id={" + bo_bu_chronid_ref + "} LOOKING FOR bu_all_chronid_ref");
        }
        //Se bo_bu_chronid_ref è presente quindi maggior di zero.
        if (bo_bu_chronid_ref > 0) {
            logger.info("Working document: " + fetchedDocument.getProperties().getIdValue("ID") + " of class: " + fetchedDocument.getClassName()
                    + " with system_id={" + bo_bu_chronid_ref + "}");
            // Verifico se nella mapping e` presente system_id trovato
            Checker.checkBoBuAssignNetCoServCo(fetchedDocument, bo_bu_chronid_ref);
        }
        ArrayList<Integer> buAllChronidRef = new ArrayList<>();
        if (bo_bu_chronid_ref == 0) {
            //Casistica in qui, system_id = 0 sul campo bo_bu_chronid_ref, si cerca di recuperare bu_all_chronid_ref
            if (fetchedDocument.getProperties().isPropertyPresent("bu_all_chronid_ref")
                    && fetchedDocument.getProperties().getStringValue("bu_all_chronid_ref") != null) {
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
                    //Tanto prendo il bu_all_chronid_ref di elemento trovato
                    Checker.checkBoBuAssignNetCoServCo(fetchedDocument, buAllChronidRef.get(0));
                }
                //Se ho più di un risultato, allora
                if (buAllChronidRef.size() > 1) {
                    String netco_servco;
                    //Verifico se le SYSTEM_ID (bu_all_chronid_ref) recuperate sono IDENTICI a SYSTEM_ID mappate in config.json sotto NetCoServCo,
                    //Allora gli assegno NETCO_SECURITY. Esempio ho un documento con due soltanto system_id e tutte e due sono NETCO.
                    if (systemIds.equals(buAllChronidRef)) {
                        netco_servco = instance.getNetCo().get(String.valueOf(buAllChronidRef.get(0)));
                        Assigner.assignNetCoSecurityProxy(netco_servco, fetchedDocument);
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
                            Assigner.assignServCoSecurityProxy(netco_servco, fetchedDocument);
                        }
                        //Altrimenti, se si trova ad esempio uno di NETCO e gli altri SERVCO e/o non mappati - non si fa nulla.
                        else {
                            logger.error("POSSIBLE MAPPING OF SERVCO & NETCO DETECTED. THERE'S NOTHING TO DO!");
                        }
                    }
                }
            }
        }

        //Caso in cui non trovo bo_bu_chronid_ref = 0 e bu_all_chronid_ref = 0
        //Per ora stampo msg di errore.
        if (buAllChronidRef.isEmpty() && bo_bu_chronid_ref == 0) {
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fetchedDocument.getClassName() + "_generic.txt", true));
                writer.write("UNABLE TO PROCESS: " + fetchedDocument.getClassName()
                        + " DUE TO [bo_bu_chronid_ref]: " + fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref")
                        + " AND [bu_all_chronid_ref]: " + buAllChronidRef + " ARE NULL or EMPTY! ID DOC: " + fetchedDocument.getProperties().getIdValue("ID") + "\n");
                logger.error("UNABLE TO PROCESS: " + fetchedDocument.getClassName()
                        + " DUE TO [bo_bu_chronid_ref]: " + fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref")
                        + " AND [bu_all_chronid_ref]: " + buAllChronidRef + " ARE NULL or EMPTY ID DOC: " + fetchedDocument.getProperties().getIdValue("ID"));
                writer.close();
            } catch (IOException e) {
                logger.error("UNABLE TO WRITE TO FILE: " + fetchedDocument.getClassName() + "_generic.txt", e);
            }
        }
    }
}
