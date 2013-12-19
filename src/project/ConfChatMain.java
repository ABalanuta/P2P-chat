package project;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class ConfChatMain {
    static Logger logger = Logger.getLogger(ConfChatMain.class);

    public static void main(String[] args) throws UnknownHostException {
        ConfChatAPI confchat = null;

        PropertyConfigurator.configure("log4j.properties");
        logger.info("Welcome to ConfChat");

        String bootStrapPort = null;
        String bootstrapIP = null;
        String clientPort = null;
        // IP Porto
        switch (args.length) {
        case (2):
            clientPort = args[0];
            bootstrapIP = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Start with ip: " + bootstrapIP);
            bootStrapPort = args[1];
            break;
        case (3):
            clientPort = args[0];
            bootstrapIP = args[1];
            bootStrapPort = args[2];
            break;
        default:
            logger.info("Usage:");
            logger.info("java ConfChatMain localbindport bootIP bootPort    (if(1st node) localbindport=bootPort=8080 else != 8181)");
            logger.info("example (1st node) java ConfChatMain 8080 192.168.2.1 8080");
            logger.info("example (others) java ConfChatMain 8081 192.168.2.1 8080");
            logger.info("You can hide the bootstrap IP if is localhost");
            return;
        }


        // launch our node!
        try {
            confchat = new ConfChatAPI(clientPort, bootstrapIP, bootStrapPort);
        } catch (Exception e) {
            logger.info(e);
            logger.info("Usage:");
            logger.info("java ConfChatMain localbindport bootIP bootPort    (if(1st node) localbindport=bootPort=8080 else != 8181)");
            logger.info("example (1st node) java ConfChatMain 8080 192.168.2.1 8080");
            logger.info("example (others) java ConfChatMain 8081 192.168.2.1 8080");
            return;
        }

        // wait 1 seconds
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
        }

        // Start interface connected with the App API
        SimpleUI ui = new SimpleUI(confchat);

        ui.start();
        confchat.exit();
    }
}
