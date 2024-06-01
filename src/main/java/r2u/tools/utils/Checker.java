package r2u.tools.utils;

import com.filenet.api.core.Document;
import r2u.tools.config.Configurator;

import static r2u.tools.constants.Constants.NO_BU;

public class Checker {
    private static final Configurator instance = Configurator.getInstance();

    /**
     * Metodo atto a ricercare le security_proxy per i contratti
     *
     * @param fetchedDocument documento attuale su cui si lavora
     * @param boBuChronidRef  in sostanza è un system_id, ed è importante per capire a quale security_proxy associare @fetchedDocument
     */
    public static void checkNoBuAssignNetCoServCo(Document fetchedDocument, int boBuChronidRef) {
        String netco_servco;
        if (instance.getNetCo().containsKey(String.valueOf(boBuChronidRef))) {
            //Se si trova, allora mi faccio dare il affisso ossia il contenuto dopo =
            netco_servco = instance.getNetCo().get(String.valueOf(boBuChronidRef));
            Assigner.assignNetCoSecurityProxy(NO_BU + netco_servco, fetchedDocument);
        } else {
            //Se non si trova bo_bu_chronid_ref mappato, allora gli si assegna servco_security a prescindere
            netco_servco = instance.getServCo().get(0);
            Assigner.assignServCoSecurityProxy(NO_BU + netco_servco, fetchedDocument);
        }
    }

    /**
     * Metodo che verifica la presenza su config.json del @param bo_bu_chronid_ref.
     * Se è presente allora al documento gli si assegna netco, diversamente servco.
     *
     * @param fetchedDocument   documento attuale su cui si lavora
     * @param bo_bu_chronid_ref in sostanza è un system_id, ed è importante per capire a quale security_proxy associare @fetchedDocument
     */
    public static void checkBoBuAssignNetCoServCo(Document fetchedDocument, int bo_bu_chronid_ref) {
        String netco_servco;
        if (instance.getNetCo().containsKey(String.valueOf(bo_bu_chronid_ref))) {
            //Se si trova, allora mi faccio dare il affisso ossia il contenuto dopo =
            netco_servco = instance.getNetCo().get(String.valueOf(bo_bu_chronid_ref));
            Assigner.assignNetCoSecurityProxy(netco_servco, fetchedDocument);
        } else {
            //Se non si trova bo_bu_chronid_ref mappato, allora gli si assegna servco_security a prescindere
            netco_servco = instance.getServCo().get(0);
            Assigner.assignServCoSecurityProxy(netco_servco, fetchedDocument);
        }
    }
}
