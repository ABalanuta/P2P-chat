package project.planetlab;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import project.ConfChatAPI;


public class StartPlanetLabTest {

    static Logger logger = Logger.getLogger(StartPlanetLabTest.class);
    private static long finalTimer;
    


    public static void main(String[] args) {
        ConfChatAPI confchat = null;
        finalTimer = System.currentTimeMillis() + 120 * 60 * 1000; // 2h of Runtime until
                                                                 	// shutdown
        boolean bootServer = false;

        String servername;
        try {
            servername = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.debug("Could not get HostName");
            return;
        }

        // Set up a simple configuration that logs on the console.
        PropertyConfigurator.configure("log4j.properties");
        logger.info("Welcome to ConfChat server: " + servername);

        ArrayList<String> bootstrapServers = new ArrayList<String>();
        ArrayList<String> bootstrapPorts = new ArrayList<String>();

        bootstrapServers.add("193.136.166.54");
        bootstrapServers.add("193.136.166.56");
        bootstrapPorts.add("8080");
        bootstrapPorts.add("8081");
        
        String serverListFileName = "nodes.txt";

        /**
         * O servidor planetlab-1.tagus.ist.utl.pt é o master da rede, o outro servidor
         * liga-se como cliente a ele e os restantes ligam-se a um deles
         */
        try {
            //bootstrap server1 
        	if (servername.equals("planetlab-1.tagus.ist.utl.pt")) {
                confchat = new ConfChatAPI("8080", bootstrapServers.get(0), "8080");
                bootServer = true;
                finalTimer += 30*60*1000; //The Boot Server Stays online 30 more minutes to complete conversations
            } 
        	
        	//bootstrap server2 
            else if (servername.equals("planetlab-2.tagus.ist.utl.pt")) {
               // logger.info("Waiting 120 seconst for MasterNode to load");
               // Thread.sleep(120000);
                //Thread.sleep(20000);
                confchat = new ConfChatAPI("8081", bootstrapServers.get(0), "8080");
                bootServer = true;
                finalTimer += 20*60*1000; //The Boot Server Stays online 20 more minutes to complete conversations
            } 
            
            else {
                Random dice = new Random();
                long time = 10000 + (dice.nextInt(10000));
                //long time = 30000 + (dice.nextInt(15000));
                logger.info("Waiting " + time / 1000 + " seconst for BootNode to load");
                
                // Avoid overload of the BootStrap Nodes
                Thread.sleep(time);

                // launch our node booting fom one of two servers
                //int index = dice.nextInt(2);
                int index = 0;
                String ip = bootstrapServers.get(index);
                String port = bootstrapPorts.get(index);
                 confchat = new ConfChatAPI("8082", ip,port);
            }

            // wait 1 seconds
            Thread.sleep(1000);

        } catch (Exception e) {
            logger.info(e.toString());
        }

        // Start interface connected with the App API
        PlanetLabTest test = new PlanetLabTest(confchat, servername, serverListFileName, finalTimer, bootServer);

        test.run();
        confchat.exit();
        logger.info("DONE");
    }
}
