package r2u.tools;

import r2u.tools.utils.JSONParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.logging.Logger;

public class Main {
    static Logger logger = Logger.getLogger("SdiPurchasingSecurityFixer");

    public static void main(String[] args) throws IOException, URISyntaxException, SQLException {
        if (args.length == 0) {
            System.out.println("No config.json passed to the argument...");
            System.exit(-1);
        }
        if (args.length > 1) {
            System.out.println("You have to pass only path to config.json...");
            System.exit(-1);
        }
        if (!Files.exists(Paths.get(args[0]))) {
            System.out.println("No config.json has been found");
            System.exit(-1);
        }
        JSONParser jsonParser = new JSONParser();
        jsonParser.parseJson(args[0], logger);
    }
}