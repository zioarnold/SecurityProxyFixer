# SdiPurchasingSecurityFixer ver1.0 by MrArni_ZIO - WIP
### Description
Tool dedicato a SdiAcquisti, sviluppato per fixare le security proxy e le security di classi documentali in base ai criteri.
Attualmente gli si passa un json fatto cosi: 
`{
"sourceCPE": "http://xxx:000/wsi/FNCEWS40MTOM/",
"sourceCPEObjectStore": "XXX",
"sourceCPEUsername": "XXX",
"sourceCPEPassword": "@ssw0rd",
"jaasStanzaName": "FileNetP8WSI",
"documentClass": "Document",
"query": "",
"phase": "3",
"objectClasses": [
{
"Document": [
],
"SecurityProxies": [
]
}
]
}
`
### documentClass
indicare per ora 'Document'
### query
Puo` essere anche vuota. Se e' vuota fa una query sulla documentClass.
### phase
Attualmente e' gestita la fase 3. Cioe' security proxy fix.
### usage
`java -jar SdiPurchasingSecurityFixer-1.0.jar ./config.json`
