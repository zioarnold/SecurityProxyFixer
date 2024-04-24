package r2u.tools;

import org.apache.log4j.Logger;
import r2u.tools.utils.JSONParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    private final static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws IOException, URISyntaxException {
        if (args.length == 0) {
            logger.error("No config_coll.json passed to the argument...");
            System.exit(-1);
        }
        if (args.length > 1) {
            logger.error("You have to pass only path to config_coll.json...");
            System.exit(-1);
        }
        if (!Files.exists(Paths.get(args[0]))) {
            logger.error("No config_coll.json has been found");
            System.exit(-1);
        }
        JSONParser jsonParser = new JSONParser();
        jsonParser.parseJson(args[0]);
        System.gc();
    }
}