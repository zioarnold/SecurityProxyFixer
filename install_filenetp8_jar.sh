mvn install:install-file -Dfile=libs/Jace.jar -DgroupId=com.ibm.filenet.cpe -DartifactId=Jace -Dversion=5.2.1.5 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=libs/navigatorAPI.jar -DgroupId=com.ibm.filenet.cpe -DartifactId=navigatorAPI -Dversion=icn203.601.003 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=libs/ojdbc7-12.1.0.2.jar -DgroupId=com.oracle -DartifactId=ojdbc7 -Dversion=12.1.0.2 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=libs/javaapi.jar -DgroupId=com.ibm.filenet.cpe -DartifactId=javaapi -Dversion=5.2.1.5 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=cdapi/stax-api.jar -DgroupId=com.ibm.filenet.cpe -DartifactId=stax-api -Dversion=5.2.1.5 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=cdapi/xlxpScanner.jar -DgroupId=com.ibm.filenet.cpe -DartifactId=xlxpScanner -Dversion=5.2.1.5 -Dpackaging=jar -DgeneratePom=true
mvn install:install-file -Dfile=cdapi/xlxpScannerUtils.jar -DgroupId=com.ibm.filenet.cpe -DartifactId=xlxpScannerUtils -Dversion=5.2.1.5 -Dpackaging=jar -DgeneratePom=true
