package r2u.tools.worker;

import com.filenet.api.collection.AccessPermissionList;
import com.filenet.api.collection.PageIterator;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.CustomObject;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.property.Properties;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.security.AccessPermission;
import com.filenet.api.util.UserContext;
import org.apache.log4j.Logger;
import r2u.tools.config.Configurator;
import r2u.tools.entities.Attachments;
import r2u.tools.entities.Contracts;
import r2u.tools.entities.Documents;
import r2u.tools.utils.DataFetcher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public class SecurityFixer {
    /**
     * Questi sono i field globali per permettere di accedervi ai loro valori fuori dei metodi ma all'interno della classe stessa.
     */
    private final static Logger logger = Logger.getLogger(SecurityFixer.class.getName());
    private final Configurator instance = Configurator.getInstance();

    /**
     * Metodo che fa il lavoretto nel ricavare le classi documentali e se sono lavorabili su json, flag è true.
     * Chiamare altri metodi di smistamento per netco o servco.
     */
    public void startSecurityFix() {
        String[] doc = instance.getDocumentClass().split(",");
        for (String docClass : doc) {
            switch (docClass) {
                case "CustomObject":
                case "Folder": {
                    logger.warn(docClass + " - atm isn't implemented.");
                }
                break;
                case "Document": {
                    //Blocco di codice atto a svolgere la statistica dei documenti elaborati
                    //Se e` a true - allora si fa soltanto la count. Diversamente si procede con resto del lavoro
                    if (instance.isCount()) {
                        Properties properties = null;
                        for (String document : instance.getDocumentClassList()) {
                            switch (document) {
                                case "acq_all_contratto":
                                case "acq_all_doc_contratto":
                                case "acq_contratto":
                                case "acq_pos_contratto": {
                                    if (instance.isPagedIterator()) {
                                        //region counters
                                        int noBuSecurityCounter = 0, noBuNetcoSecurityCounter = 0,
                                                noBuServcoSecurityCounter = 0, nullSecurityCounter = 0;
                                        //endregion

                                        //region NoBu
                                        Iterator<?> noBU_Security = DataFetcher.getSecurityProxy(document + "_NO_BU_security", instance.getObjectStore());
                                        if (noBU_Security != null && noBU_Security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) noBU_Security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        PageIterator noBuSecurity = DataFetcher.getDocumentClassBySecurityProxyIdPaged(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (noBuSecurity.nextPage()) {
                                            noBuSecurityCounter += noBuSecurity.getElementCount();
                                        }
                                        logger.info(document.toUpperCase() + "_NO_BU_SECURITY: " + noBuSecurityCounter);
                                        //endregion

                                        //region NoBu_Netco
                                        Iterator<?> noBuNetco_Security = DataFetcher.getSecurityProxy(document + "_NO_BU_netco_security", instance.getObjectStore());
                                        if (noBuNetco_Security != null && noBuNetco_Security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) noBuNetco_Security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        PageIterator noBuNetco = DataFetcher.getDocumentClassBySecurityProxyIdPaged(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (noBuNetco.nextPage()) {
                                            noBuNetcoSecurityCounter += noBuNetco.getElementCount();
                                        }
                                        logger.info(document.toUpperCase() + "_NO_BU_NETCO_SECURITY: " + noBuNetcoSecurityCounter);
                                        //endregion

                                        //region NoBu_Servco
                                        Iterator<?> noBuServco_Security = DataFetcher.getSecurityProxy(document + "_NO_BU_servco_security", instance.getObjectStore());
                                        if (noBuServco_Security != null && noBuServco_Security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) noBuServco_Security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        PageIterator noBuServco = DataFetcher.getDocumentClassBySecurityProxyIdPaged(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (noBuServco.nextPage()) {
                                            noBuServcoSecurityCounter += noBuServco.getElementCount();
                                        }
                                        logger.info(document.toUpperCase() + "_NO_BU_SERVCO_SECURITY: " + noBuServcoSecurityCounter);
                                        //endregion

                                        //region nullSecurity
                                        Iterator<?> nullSecurity = DataFetcher.fetchDocumentWithNullSecurityProxy(document, instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (nullSecurity != null && nullSecurity.hasNext()) {
                                            nullSecurity.next();
                                            nullSecurityCounter++;
                                        }
                                        logger.info(document.toUpperCase() + " NULL SECURITY: " + nullSecurityCounter);
                                        //endregion
                                    } else {
                                        //region counters
                                        int noBuSecurityCounter = 0, noBuNetcoSecurityCounter = 0,
                                                noBuServcoSecurityCounter = 0, nullSecurityCounter = 0;
                                        //endregion

                                        //region NoBu
                                        Iterator<?> noBU_Security = DataFetcher.getSecurityProxy(document + "_NO_BU_security", instance.getObjectStore());
                                        if (noBU_Security != null && noBU_Security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) noBU_Security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        Iterator<?> noBuSecurity = DataFetcher.getDocumentClassBySecurityProxyId(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (noBuSecurity != null && noBuSecurity.hasNext()) {
                                            noBuSecurity.next();
                                            noBuSecurityCounter++;
                                        }
                                        logger.info(document.toUpperCase() + "_NO_BU_SECURITY: " + noBuSecurityCounter);
                                        //endregion

                                        //region NoBu_Netco
                                        Iterator<?> noBuNetco_Security = DataFetcher.getSecurityProxy(document + "_NO_BU_netco_security", instance.getObjectStore());
                                        if (noBuNetco_Security != null && noBuNetco_Security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) noBuNetco_Security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        Iterator<?> noBuNetco = DataFetcher.getDocumentClassBySecurityProxyId(document, Objects.requireNonNull(properties).getIdValue("Id").toString(), instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (noBuNetco != null && noBuNetco.hasNext()) {
                                            noBuNetco.next();
                                            noBuNetcoSecurityCounter++;
                                        }
                                        logger.info(document.toUpperCase() + "_NO_BU_NETCO_SECURITY: " + noBuNetcoSecurityCounter);
                                        //endregion

                                        //region NoBu_Servco
                                        Iterator<?> noBuServco_Security = DataFetcher.getSecurityProxy(document + "_NO_BU_servco_security", instance.getObjectStore());
                                        if (noBuServco_Security != null && noBuServco_Security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) noBuServco_Security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        Iterator<?> noBuServco = DataFetcher.getDocumentClassBySecurityProxyId(document, Objects.requireNonNull(properties).getIdValue("Id").toString(), instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (noBuServco != null && noBuServco.hasNext()) {
                                            noBuServco.next();
                                            noBuServcoSecurityCounter++;
                                        }
                                        logger.info(document.toUpperCase() + "_NO_BU_SERVCO_SECURITY: " + noBuServcoSecurityCounter);
                                        //endregion

                                        //region nullSecurity
                                        Iterator<?> nullSecurity = DataFetcher.fetchDocumentWithNullSecurityProxy(document, instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (nullSecurity != null && nullSecurity.hasNext()) {
                                            nullSecurity.next();
                                            nullSecurityCounter++;
                                        }
                                        logger.info(document.toUpperCase() + " NULL SECURITY: " + nullSecurityCounter);
                                        //endregion
                                    }
                                }
                                break;
                                default: {
                                    if (instance.isPagedIterator()) {
                                        //region counters
                                        int servcoSecurityCounter = 0, netcoSecurityCounter = 0,
                                                defaultSecurityCounter = 0, nullSecurityCounter = 0;
                                        //endregion

                                        //region Servco_Security
                                        Iterator<?> servco_security = DataFetcher.getSecurityProxy(document + "_servco_security", instance.getObjectStore());
                                        if (servco_security != null && servco_security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) servco_security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        PageIterator servcoSecurity = DataFetcher.getDocumentClassBySecurityProxyIdPaged(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());

                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (servcoSecurity.nextPage()) {
                                            servcoSecurityCounter += servcoSecurity.getElementCount();
                                        }
                                        logger.info(document.toUpperCase() + "_SERVCO_SECURITY: " + servcoSecurityCounter);
                                        //endregion

                                        //region Netco_Security
                                        Iterator<?> netco_security = DataFetcher.getSecurityProxy(document + "_netco_security", instance.getObjectStore());
                                        if (netco_security != null && netco_security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) netco_security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        PageIterator netcoSecurity = DataFetcher.getDocumentClassBySecurityProxyIdPaged(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (netcoSecurity.nextPage()) {
                                            netcoSecurityCounter += netcoSecurity.getElementCount();
                                        }
                                        logger.info(document.toUpperCase() + "_NETCO_SECURITY: " + netcoSecurityCounter);
                                        //endregion

                                        //region Oda_Security
                                        Iterator<?> oda_security = DataFetcher.getSecurityProxy(document + "_security", instance.getObjectStore());
                                        if (oda_security != null && oda_security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) oda_security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        PageIterator defaultSecurity = DataFetcher.getDocumentClassBySecurityProxyIdPaged(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (defaultSecurity.nextPage()) {
                                            defaultSecurityCounter += defaultSecurity.getElementCount();
                                        }
                                        logger.info(document.toUpperCase() + "_SECURITY: " + defaultSecurityCounter);
                                        //endregion

                                        //region nullSecurity
                                        Iterator<?> nullSecurity = DataFetcher.fetchDocumentWithNullSecurityProxy(document, instance.getObjectStore());
                                        while (nullSecurity != null && nullSecurity.hasNext()) {
                                            nullSecurity.next();
                                            nullSecurityCounter++;
                                        }
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        logger.info(document.toUpperCase() + " NULL SECURITY: " + nullSecurityCounter);
                                        //endregion
                                    } else {
                                        //region counters
                                        int servcoSecurityCounter = 0, netcoSecurityCounter = 0,
                                                defaultSecurityCounter = 0, nullSecurityCounter = 0;
                                        //endregion

                                        //region Servco_Security
                                        Iterator<?> servco_security = DataFetcher.getSecurityProxy(document + "_servco_security", instance.getObjectStore());
                                        if (servco_security != null && servco_security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) servco_security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        Iterator<?> servcoSecurity = DataFetcher.getDocumentClassBySecurityProxyId(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());

                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (servcoSecurity != null && servcoSecurity.hasNext()) {
                                            servcoSecurity.next();
                                            servcoSecurityCounter++;
                                        }
                                        logger.info(document.toUpperCase() + "_SERVCO_SECURITY: " + servcoSecurityCounter);
                                        //endregion

                                        //region Netco_Security
                                        Iterator<?> netco_security = DataFetcher.getSecurityProxy(document + "_netco_security", instance.getObjectStore());
                                        if (netco_security != null && netco_security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) netco_security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        Iterator<?> netcoSecurity = DataFetcher.getDocumentClassBySecurityProxyId(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (netcoSecurity != null && netcoSecurity.hasNext()) {
                                            netcoSecurity.next();
                                            netcoSecurityCounter++;
                                        }
                                        logger.info(document.toUpperCase() + "_NETCO_SECURITY: " + netcoSecurityCounter);
                                        //endregion

                                        //region Oda_Security
                                        Iterator<?> oda_security = DataFetcher.getSecurityProxy(document + "_security", instance.getObjectStore());
                                        if (oda_security != null && oda_security.hasNext()) {
                                            RepositoryRow repositoryRow = (RepositoryRow) oda_security.next();
                                            properties = repositoryRow.getProperties();
                                        }
                                        Iterator<?> defaultSecurity = DataFetcher.getDocumentClassBySecurityProxyId(document,
                                                Objects.requireNonNull(properties).getIdValue("Id").toString(),
                                                instance.getObjectStore());
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        while (defaultSecurity != null && defaultSecurity.hasNext()) {
                                            defaultSecurity.next();
                                            defaultSecurityCounter++;
                                        }
                                        logger.info(document.toUpperCase() + "_SECURITY: " + defaultSecurityCounter);
                                        //endregion

                                        //region nullSecurity
                                        Iterator<?> nullSecurity = DataFetcher.fetchDocumentWithNullSecurityProxy(document, instance.getObjectStore());
                                        while (nullSecurity != null && nullSecurity.hasNext()) {
                                            nullSecurity.next();
                                            nullSecurityCounter++;
                                        }
                                        logger.info("Query/queries executed, calculating the results... grab some tea, this may take a while");
                                        logger.info(document.toUpperCase() + " NULL SECURITY: " + nullSecurityCounter);
                                        //endregion
                                    }
                                }
                                break;
                            }
                        }
                        return;
                    } else {
                        if (!instance.getQuery().isEmpty()) {
                            Document fetchedDocument = null;
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
                                    } catch (Exception exception) {
                                        logger.error("SOMETHING WENT WRONG... ", exception);
                                        try {
                                            BufferedWriter readOnlySecurityProxyWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName()
                                                    + "_caught_errors.txt", true));
                                            readOnlySecurityProxyWriter.write("AN ERROR IS OCCURED WITH DOCUMENT_CLASS: " + fetchedDocument.getClassName() + " ID " + fetchedDocument.getProperties().getIdValue("ID").toString()
                                                    + "\nERROR IS: " + exception +
                                                    "\nMESSAGE IS: " + exception.getMessage() +
                                                    "\nCAUSE IS: " + exception.getCause() +
                                                    "\nSTACK TRACE: " + Arrays.toString(exception.getStackTrace()) + "\n");
                                            readOnlySecurityProxyWriter.close();
                                        } catch (IOException e) {
                                            logger.error("UNABLE CREATE & WRITE TO FILE: " + Objects.requireNonNull(fetchedDocument).getClassName() + "_caught_errors.txt");
                                        }
                                    }
                                }
                            } else {
                                logger.error("UNABLE TO FETCH DATA FROM " + instance.getQuery() + " DataFetcher.fetchDataByQuery() RETURNED NULL! Aborting...");
                                if (UserContext.get() != null) {
                                    UserContext.get().popSubject();
                                }
                                return;
                            }
                            return;
                        }
                        //TODO da testare la rimozione dei vecchi permessi sugli security_proxy
                        if (instance.isRemoveOldPermissions()) {
                            Iterator<?> securityProxies = DataFetcher.getSecurityProxies(instance.getObjectStore());
                            while (securityProxies != null && securityProxies.hasNext()) {
                                RepositoryRow repositoryRow = (RepositoryRow) securityProxies.next();
                                Properties properties = repositoryRow.getProperties();
                                CustomObject securityProxy = Factory.CustomObject.fetchInstance(instance.getObjectStore(), properties.getIdValue("ID"), null);
                                AccessPermissionList permissions = securityProxy.get_Permissions();
                                for (Object perm : permissions) {
                                    AccessPermission accessPermission = (AccessPermission) perm;
                                    //Scorro la lista degli gruppi da rimuovere e se ne trovo la corrispondenza - la rimuovo
                                    for (String groupToRemove : instance.getLdapGroupToRemove()) {
                                        if (accessPermission.get_GranteeName().equals(groupToRemove)) {
                                            permissions.remove(groupToRemove);
                                        }
                                    }
                                }
                                securityProxy.save(RefreshMode.REFRESH);
                            }
                            return;
                        }
                        //Flag di controllo impostabile su json.Se e`false, allora vedo se c 'e' la query.
                        //Se e`true allora subentra la lista delle classi documentali impostati a docClassList
                        if (instance.isMassive()) {
                            Document fetchedDocument = null;
                            for (String document : instance.getDocumentClassList()) {
                                Iterator<?> iterator = DataFetcher.fetchRowsByClass(document, instance.getObjectStore());
                                if (iterator != null) {
                                    while (iterator.hasNext()) {
                                        try {
                                            RepositoryRow repositoryRow = (RepositoryRow) iterator.next();
                                            Properties properties = repositoryRow.getProperties();
                                            fetchedDocument = Factory.Document.fetchInstance(instance.getObjectStore(), properties.getIdValue("ID"), null);
                                            //Controllo se la classe documentale e` mappata
                                            if (instance.getDocumentMap().containsKey(fetchedDocument.getClassName())) {
                                                //Controllo se bisogna lavorare sulla classe documentale quindi flag = true
                                                if (instance.getDocumentMap().get(fetchedDocument.getClassName())) {
                                                    preProcessDocuments(fetchedDocument);
                                                }
                                            } else {
                                                logger.error("NO " + fetchedDocument.getClassName() + " MAPPED IN config.json AT objectClasses -> docClassList");
                                            }
                                        } catch (Exception exception) {
                                            logger.error("SOMETHING WENT WRONG... ", exception);
                                            try {
                                                BufferedWriter readOnlySecurityProxyWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName()
                                                        + "_caught_errors.txt", true));
                                                readOnlySecurityProxyWriter.write("AN ERROR IS OCCURED WITH DOCUMENT_CLASS: " + fetchedDocument.getClassName() + " ID " + fetchedDocument.getProperties().getIdValue("ID").toString()
                                                        + "\nERROR IS: " + exception +
                                                        "\nMESSAGE IS: " + exception.getMessage() +
                                                        "\nCAUSE IS: " + exception.getCause() +
                                                        "\nSTACK TRACE: " + Arrays.toString(exception.getStackTrace()) + "\n");
                                                readOnlySecurityProxyWriter.close();
                                            } catch (IOException e) {
                                                logger.error("UNABLE CREATE & WRITE TO FILE: " + Objects.requireNonNull(fetchedDocument).getClassName() + "_caught_errors.txt");
                                            }
                                        }
                                    }
                                } else {
                                    logger.error("UNABLE TO FETCH DATA FROM : " + document + " DataFetcher.fetchRowsByClass() RETURNED NULL! Aborting...");
                                    if (UserContext.get() != null) {
                                        UserContext.get().popSubject();
                                    }
                                    return;
                                }
                            }
                            return;
                        }
                        //Flag di controllo, se bisogna lavorare i documenti con vecchio security_proxy
                        //E` a false, quindi procedura standard
                        if (instance.getReWorkOldIds()) {
                            //Processo di automazione x elaborare le classi documentale i quali non sono stati processati
                            logger.info("ReWork of unchanged documents is started!");
                            for (String document : instance.getDocumentClassList()) {
                                //Ottengo security proxy vecchio per dire: acq_oda_security ed il suo ID
                                Iterator<?> oldSecurityProxyIterator = DataFetcher.fetchDocumentWithUnchangedSecurityProxies(document, instance.getObjectStore());
                                if (oldSecurityProxyIterator != null) {
                                    while (oldSecurityProxyIterator.hasNext()) {
                                        RepositoryRow repositoryRow = (RepositoryRow) oldSecurityProxyIterator.next();
                                        Properties properties = repositoryRow.getProperties();
                                        logger.info("Found document: " + document + " with old security_proxy " + properties.getStringValue("codice"));
                                        CustomObject securityProxy = Factory.CustomObject.fetchInstance(instance.getObjectStore(), properties.getIdValue("ID").toString(), null);
                                        //Ricerco la classe documentale di interesse con l'id del security_proxy per dire acq_oda_security
                                        Iterator<?> documentWithOldSecurityProxyIterator = DataFetcher.getDocumentClassBySecurityProxyId(document, securityProxy.getProperties().getIdValue("ID").toString(), instance.getObjectStore());
                                        if (documentWithOldSecurityProxyIterator != null) {
                                            while (documentWithOldSecurityProxyIterator.hasNext()) {
                                                RepositoryRow reWorkFetchRow = (RepositoryRow) documentWithOldSecurityProxyIterator.next();
                                                Properties documentProperties = reWorkFetchRow.getProperties();
                                                Document currentDocument = Factory.Document.fetchInstance(instance.getObjectStore(), documentProperties.getIdValue("ID"), null);
                                                logger.info("Sending to rework it."); //E quindi qualora ci fossero li mando x elaborazione ex-novo.
                                                try {
                                                    preProcessDocuments(currentDocument);
                                                } catch (Exception exception) {
                                                    logger.error("SOMETHING WENT WRONG... ", exception);
                                                    try {
                                                        BufferedWriter readOnlySecurityProxyWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(currentDocument).getClassName()
                                                                + "_caught_errors.txt", true));
                                                        readOnlySecurityProxyWriter.write("AN ERROR IS OCCURED WITH DOCUMENT_CLASS: " + currentDocument.getClassName() + " ID " + Objects.requireNonNull(currentDocument).getProperties().getIdValue("ID").toString()
                                                                + "\nERROR IS: " + exception +
                                                                "\nMESSAGE IS: " + exception.getMessage() +
                                                                "\nCAUSE IS: " + exception.getCause() +
                                                                "\nSTACK TRACE: " + Arrays.toString(exception.getStackTrace()) + "\n");
                                                        readOnlySecurityProxyWriter.close();
                                                    } catch (IOException ignored) {
                                                        logger.error("UNABLE CREATE & WRITE TO FILE: " + Objects.requireNonNull(currentDocument).getClassName() + "_caught_errors.txt");
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            return;
                        }
                        //Flag di controllo in cui bisognerà poi lavorare i documenti con security_proxy vuota
                        if (instance.isReWorkNulls()) {
                            logger.info("ReWork of documents with security_proxy = null is started!");
                            for (String document : instance.getDocumentClassList()) {
                                Iterator<?> nullSecurityProxyIterator = DataFetcher.fetchDocumentWithNullSecurityProxy(document, instance.getObjectStore());
                                if (nullSecurityProxyIterator != null) {
                                    while (nullSecurityProxyIterator.hasNext()) {
                                        RepositoryRow repositoryRow = (RepositoryRow) nullSecurityProxyIterator.next();
                                        Properties properties = repositoryRow.getProperties();
                                        logger.info("Found document: " + document + " with null security_proxy ");
                                        Document currentDocument = Factory.Document.fetchInstance(instance.getObjectStore(), properties.getIdValue("ID"), null);
                                        logger.info("Sending to rework it."); //E quindi qualora ci fossero li mando x elaborazione ex-novo.
                                        try {
                                            preProcessDocuments(currentDocument);
                                        } catch (Exception exception) {
                                            logger.error("SOMETHING WENT WRONG... ", exception);
                                            try {
                                                BufferedWriter readOnlySecurityProxyWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(currentDocument).getClassName()
                                                        + "_caught_errors.txt", true));
                                                readOnlySecurityProxyWriter.write("AN ERROR IS OCCURED WITH DOCUMENT_CLASS: " + currentDocument.getClassName() + " ID " + Objects.requireNonNull(currentDocument).getProperties().getIdValue("ID").toString()
                                                        + "\nERROR IS: " + exception +
                                                        "\nMESSAGE IS: " + exception.getMessage() +
                                                        "\nCAUSE IS: " + exception.getCause() +
                                                        "\nSTACK TRACE: " + Arrays.toString(exception.getStackTrace()) + "\n");
                                                readOnlySecurityProxyWriter.close();
                                            } catch (IOException ignored) {
                                                logger.error("UNABLE CREATE & WRITE TO FILE: " + Objects.requireNonNull(currentDocument).getClassName() + "_caught_errors.txt");
                                            }
                                        }
                                    }
                                }
                            }
                            return;
                        }
                    }
                }
                break;
            }
        }
        if (UserContext.get() != null) {
            UserContext.get().popSubject();
        }
    }

    /**
     * Metodo preProcessore atto a determinare come lavorare quando gli arriva un certo tipo di documento
     * Qualora il documento è in stato "FREEZE" (Bloccato) allora non si potrà fare nulla.
     *
     * @param fetchedDocument documento attuale su cui bisogna lavorare
     */
    private void preProcessDocuments(Document fetchedDocument) {
        if (fetchedDocument.get_IsFrozenVersion()) {
            logger.warn("FROZEN VERSION IS DETECTED. IT'S NOT POSSIBILE TO MODIFY CUSTOM PROPERTIES.\n" +
                    "DOC CLASS IS: " + fetchedDocument.getClassName() + "/" + fetchedDocument.getProperties().getIdValue("ID"));
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(fetchedDocument.getClassName() + "_frozen_version.txt", true));
                writer.write("FROZEN VERSION IS DETECTED. IT'S NOT POSSIBILE TO MODIFY CUSTOM PROPERTIES.\n" +
                        "DOC CLASS IS: " + fetchedDocument.getClassName() + "/" + fetchedDocument.getProperties().getIdValue("ID") + "\n");
                writer.close();
            } catch (IOException e) {
                logger.error("UNABLE TO WRITE TO FILE: " + fetchedDocument.getClassName() + "_frozen_version.txt", e);
            }
            return;

        }
        switch (fetchedDocument.getClassName()) {
            //Gestione dei contratti
            case "acq_all_contratto":
            case "acq_all_doc_contratto":
            case "acq_contratto":
            case "acq_pos_contratto": {
                logger.info("Working with docClass " + fetchedDocument.getClassName());
                Contracts.processContractDocumentClasses(fetchedDocument);
            }
            break;
            //Gestione degli allegati
            case "acq_all_pon":
            case "acq_all_oda":
            case "acq_all_rda":
            case "acq_all_rdo": {
                logger.info("Working with docClass " + fetchedDocument.getClassName());
                Attachments.processAttachments(fetchedDocument);
            }
            break;
            //Gestione delle classi documentali che non siano gli allegati, per dire Allegato Pon (acq_all_pon)
            default: {
                logger.info("Working with docClass " + fetchedDocument.getClassName());
                Documents.processDefaultDocumentClasses(fetchedDocument);
            }
            break;
        }
    }
}