# SdiPurchasingSecurityFixer ver1.2.1 by MrArni_ZIO - Released
### Description
Tool dedicato a SdiAcquisti, sviluppato per fixare le security proxy e le security di classi documentali in base ai criteri.
Nel suo funzionamento il Tool non fa altro che:<br>
1 - Estrarre ciò che gli si passa in query, o nel caso estrae tutte le classi documentali;<br>
2 - Per ogni classe documentale, recupera il suo system_id per poi fare delle considerazioni;<br>
3 - Tool si rivolge al config json nella sezione: `objectClasses -> Document` vede se la classe documentale intanto è presente ed è lavorabile (=true/false).
`Esempio: example=true` oppure `example=false`<br>
4 - Se è `false`, allora non si fa niente, passa al punto 9;<br>
5 - Se è `true`, allora tira fuori il suo `security_proxy` ID per eventuale restore;<br>
6 - Vede se il `system_id` è maggiore di zero e quindi presente nel documento lavorabile, allora 
 si ridirige verso `config.json` nella sezione `objectClasses -> NetCoServCo`, per vedere se system_id è presente;
`Esempio: 7272192=_netco_security`<br>
7 - Se è presente, allora prende il contenuto dopo '='`(_netco_security)` per poi recuperarlo e inserirlo nel documento.
`(In sostanza diventa, esempio: object_class_example_netco_security).`
Ma prima di questo, verifica se nel documento non è presente security_proxy per poi inserirlo, 
oppure se non è stato aggiornato. Se sono uguali non fa niente;<br>
8 - Aggiorna la classe documentale e salva le modifiche con nuovo security_proxy e passa al successivo per lavorarlo;<br>
9 - Fine.<br>
`P.S. Attenzione, prima di lanciare il tool, bisogna creare delle security_proxy con della convention impostata.`<br>
`symbolic_name(classe documentale)_netco_security oppure symbolic_name_servco_security`!
</br>
Intanto qui la spiegazione delle chiavi su config.json
### sourceCPE
Indicare lo WSI del FN di partenza, esempio `http://0.0.0.0:0/wsi/FNCEWS40MTOM/`
### sourceCPEObjectStore
Indicare il nome dell'object store
### sourceCPEUsername
Indicare lo username che abilitato ad accedere ad ACCE.
### sourceCPEPassword 
Indicare relativo password
### jaasStanzaName
Deve essere FileNetP8WSI.
### documentClass
E' al momento gestito `Document`, ma può ricevere in ingresso `Document,CustomObject,Folder`
### query
Può essere anche vuota. Se è vuota fa una query sulla documentClass. `Attenzione` però, se scrivete una query vostra a piacere, 
siate coscienti di modificare in `objectClasses->Document` mettendo a true la classe documentale.</br>
Esempio della query: `SELECT TOP 3* FROM [object_class] ORDER BY DateCreated DESC` quindi impostare `object_class=true`.<br>
Altrimenti non si fa nulla. 
### _usage_
`java -jar path\filename.jar path\config.json`
#### Nota
Quando svilupperete ereditando il codice, eseguite prima di tutto il file `install_filenetp8_jar.sh`.
