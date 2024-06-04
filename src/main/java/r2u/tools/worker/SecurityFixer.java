package r2u.tools.worker;

import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.property.Properties;
import com.filenet.api.query.RepositoryRow;
import com.filenet.api.util.UserContext;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;
import r2u.tools.config.Configurator;
import r2u.tools.constants.Constants;
import r2u.tools.entities.Attachments;
import r2u.tools.entities.Contracts;
import r2u.tools.entities.Documents;
import r2u.tools.utils.DataFetcher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
        long startTime, endTime;
        String[] doc = instance.getDocumentClass().split(",");
        logger.info("Starting security fix...");
        startTime = System.currentTimeMillis();
        Document fetchedDocument = null;
        for (String docClass : doc) {
            switch (docClass) {
                case "CustomObject":
                case "Folder": {
                    logger.warn(docClass + " - atm isn't implemented.");
                }
                break;
                case "Document": {
                    //Flag di controllo impostabile su json. Se e` false, allora vedo se c'e' la query.
                    //Se e` true allora subentra la lista delle classi documentali impostati a docClassList
                    if (!instance.isMassive()) {
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
                                    } catch (Exception exception) {
                                        logger.error("SOMETHING WENT WRONG... ", exception);
                                        try {
                                            PrintWriter unManagedErrorsWriter = new PrintWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName() + "_caught_errors.txt", true));
                                            exception.printStackTrace(unManagedErrorsWriter);
                                            unManagedErrorsWriter.close();
                                        } catch (IOException e) {
                                            logger.error("UNABLE CREATE & WRITE TO FILE: " + Objects.requireNonNull(fetchedDocument).getClassName() + "_caught_errors.txt");
                                        }
                                    }
                                }
                            } else {
                                logger.error("UNABLE TO FETCH DATA FROM : " + fetchedDocument + " fetchDataByQuery() RETURNED NULL! Aborting...");
                                if (UserContext.get() != null) {
                                    UserContext.get().popSubject();
                                }
                                endTime = System.currentTimeMillis();
                                logger.info("Security fixer terminated within: " + DurationFormatUtils.formatDuration(endTime - startTime, Constants.dateTimeFormat, true));
                                System.exit(-1);
                            }
                        } else {
                            logger.error("SPECIFY QUERY...");
                            endTime = System.currentTimeMillis();
                            logger.info("Security fixer terminated within: " + DurationFormatUtils.formatDuration(endTime - startTime, Constants.dateTimeFormat, true));
                            System.exit(-1);
                            if (UserContext.get() != null) {
                                UserContext.get().popSubject();
                            }
                            System.exit(-1);
                        }
                    } else {
                        for (String document : instance.getDocumentClassList()) {
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
                                            logger.error("NO " + fetchedDocument.getClassName() + " MAPPED IN config.json AT objectClasses -> docClassList");
                                        }
                                    } catch (Exception exception) {
                                        logger.error("SOMETHING WENT WRONG... ", exception);
                                        try {
                                            BufferedWriter readOnlySecurityProxyWriter = new BufferedWriter(new FileWriter(Objects.requireNonNull(fetchedDocument).getClassName()
                                                    + "_caught_errors.txt", true));
                                            readOnlySecurityProxyWriter.write("AN ERROR IS OCCURED WITH DOCUMENT_CLASS: " + fetchedDocument.getClassName() + " ID " + fetchedDocument.getProperties().getIdValue("ID").toString()
                                                    + "\nERROR IS: " + exception.getLocalizedMessage() + "\nCAUSE IS: " + exception.getCause() + "\nSTACK TRACE: " + Arrays.toString(exception.getStackTrace()) + "\n");
                                            readOnlySecurityProxyWriter.close();
                                        } catch (IOException e) {
                                            logger.error("UNABLE CREATE & WRITE TO FILE: " + Objects.requireNonNull(fetchedDocument).getClassName() + "_caught_errors.txt");
                                        }
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
        if (UserContext.get() != null) {
            UserContext.get().popSubject();
        }
    }

    /**
     * Metodo preProcessore atto a determinare come lavorare quando gli arriva un certo tipo di documento
     *
     * @param fetchedDocument documento attuale su cui bisogna lavorare
     */
    private void preProcessDocuments(Document fetchedDocument) {
        switch (fetchedDocument.getClassName()) {
            //Gestione delle classi documentali che non siano gli allegati, per dire Allegato Pon (acq_all_pon)
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
            default: {
                logger.info("Working with docClass " + fetchedDocument.getClassName());
                Documents.processDefaultDocumentClasses(fetchedDocument);
            }
            break;
        }
    }
}