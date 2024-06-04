package r2u.tools.entities;

import com.filenet.api.core.Document;
import com.filenet.api.query.RepositoryRow;
import org.apache.log4j.Logger;
import r2u.tools.config.Configurator;
import r2u.tools.utils.Assigner;
import r2u.tools.utils.Checker;
import r2u.tools.utils.Converters;
import r2u.tools.utils.DataFetcher;

import java.util.ArrayList;
import java.util.Iterator;

import static r2u.tools.constants.Constants.NO_BU;

public class Contracts {
    private static final Configurator instance = Configurator.getInstance();
    private final static Logger logger = Logger.getLogger(Contracts.class.getName());

    /**
     * Metodo di gestione delle classi contrattuali, esempio: "acq_contratto" o simili.
     * Nella sua logica, lui determina a quale security_proxy associare che per questo sono state implementati dei metodi appositi.
     * Prima di assegnare la security_proxy, tale metodo verifica la presenza del nome_gruppo.
     * Se nome_gruppo e` presente, allora non si fa nulla, diversamente si procede ad assegnare le security_proxy standardamente.
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
    public static void processContractDocumentClasses(Document fetchedDocument) {
        int boBuChronidRef = 0;
        ArrayList<Integer> buAllChronidRef = new ArrayList<>();
        ArrayList<String> groupNameList = new ArrayList<>();
        //Gestione degli contratti. Per prima cosa si va a vedere il campo nome_gruppo della Bu Societa.
        //Se e` presente, allora non si fa nulla.
        //Se e` assente, allora procedimento standard con la assegnazione di NO_BU_servco/netco_security
        if (fetchedDocument.getProperties().isPropertyPresent("bo_bu_chronid_ref") &&
                fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref") != null) {
            logger.info("bo_bu_chronid_ref is present and is not null on document: " + fetchedDocument.getProperties().getIdValue("ID")
                    + " of class: " + fetchedDocument.getClassName());
            Iterator<?> groupNameIterator = DataFetcher.fetchGroupNameByBOBUChronicleIdRef(fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref"),
                    instance.getObjectStore());
            //Se il campo e` popolato (nome_gruppo) allora nun si fa nulla
            if (groupNameIterator != null && groupNameIterator.hasNext()) {
                RepositoryRow repositoryRow = (RepositoryRow) groupNameIterator.next();
                String groupName = repositoryRow.getProperties().getStringValue("nome_gruppo");
                if (groupName != null && !groupName.isEmpty()) {
                    logger.warn("NOME_GRUPPO field isn`t empty, the field value is: " + groupName + "! There's nothing to do!");
                    return;
                }
            }
        }
        //Gestione dei casi in cui, per acq_all_contratto sono stati trovati dei documenti con security_proxy null
        //Succede per mancanza di campo bo_bu_chronid_ref, quindi e` stato aggiunto il controllo
        //sul campo bu_all_chronid_ref e quindi si controlla uno x uno...se tutti hanno il nome_gruppo popolato - non si fa niente
        if (fetchedDocument.getProperties().isPropertyPresent("bu_all_chronid_ref") &&
                fetchedDocument.getProperties().getStringListValue("bu_all_chronid_ref") != null) {
            logger.info("bu_all_chronid_ref is present and is not null on document: " + fetchedDocument.getProperties().getIdValue("ID")
                    + " of class: " + fetchedDocument.getClassName());
            Iterator<?> iterator = DataFetcher.fetchSystemIdByBUALLChronicleIdRef(fetchedDocument.getProperties().getStringListValue("bu_all_chronid_ref"), instance.getObjectStore());
            while (iterator != null && iterator.hasNext()) {
                RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                groupNameList.add(repositoryRow.getProperties().getStringValue("nome_gruppo"));
            }
            int nullCounter = 0;
            for (String groupName : groupNameList) {
                if (groupName == null || groupName.isEmpty()) {
                    nullCounter++;
                }
            }
            if (nullCounter == groupNameList.size()) {
                logger.warn("NOME_GRUPPO field isn`t empty, the field value are: " + groupNameList + "! There's nothing to do!");
                return;
            }
        }
        //Recupero il system_id del documento in anagrafica
        //Nonostante che sia stringa, a db risulta numeric.
        if (fetchedDocument.getProperties().isPropertyPresent("bo_bu_chronid_ref") &&
                fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref") != null) {
            Iterator<?> boBuChronidRefIterator = DataFetcher.fetchSystemIdByBOBUChronicleIdRef(fetchedDocument.getProperties().getStringValue("bo_bu_chronid_ref"),
                    instance.getObjectStore());
            if (boBuChronidRefIterator != null && boBuChronidRefIterator.hasNext()) {
                RepositoryRow repositoryRow = (RepositoryRow) boBuChronidRefIterator.next();
                boBuChronidRef = repositoryRow.getProperties().getInteger32Value("system_id");
            }
            logger.info("Found document: " + fetchedDocument.getProperties().getIdValue("ID") + " of class: " + fetchedDocument.getClassName()
                    + " with system_id={" + boBuChronidRef + "}");
        } else {
            logger.error("FOUND DOCUMENT: " + fetchedDocument.getProperties().getIdValue("ID") + " OF CLASS: " + fetchedDocument.getClassName()
                    + " WITH system_id = {" + boBuChronidRef + "} LOOKING FOR bu_all_chronid_ref");
        }
        //Se bo_bu_chronid_ref è presente quindi maggior di zero.
        if (boBuChronidRef > 0) {
            logger.info("Working document: " + fetchedDocument.getProperties().getIdValue("ID") + " of class: " + fetchedDocument.getClassName()
                    + " with system_id={" + boBuChronidRef + "}");
            // Verifico se nella mapping e` presente system_id trovato
            Checker.checkNoBuAssignNetCoServCo(fetchedDocument, boBuChronidRef);
        }
        if (boBuChronidRef == 0) {
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
                //Tanto prendo il bu_all_chronid_ref di elemento trovato
                Checker.checkNoBuAssignNetCoServCo(fetchedDocument, buAllChronidRef.get(0));
            }
            //Se ho più di un risultato, allora
            if (buAllChronidRef.size() > 1) {
                String netco_servco;
                //Verifico se le SYSTEM_ID (bu_all_chronid_ref) recuperate sono IDENTICI a SYSTEM_ID mappate in config.json sotto NetCoServCo,
                //Allora gli assegno NETCO_SECURITY. Esempio ho un documento con due soltanto system_id e tutte e due sono NETCO.
                if (systemIds.equals(buAllChronidRef)) {
                    netco_servco = instance.getNetCo().get(String.valueOf(buAllChronidRef.get(0)));
                    Assigner.assignNetCoSecurityProxy(NO_BU + netco_servco, fetchedDocument);
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
                        Assigner.assignServCoSecurityProxy(NO_BU + netco_servco, fetchedDocument);
                    }
                    //Altrimenti, se si trova ad esempio uno di NETCO e gli altri SERVCO e/o non mappati - non si fa nulla.
                    else {
                        logger.error("POSSIBLE MAPPING OF SERVCO & NETCO DETECTED. THERE'S NOTHING TO DO!");
                    }
                }
            }
        }
    }
}
