package start;

import exceptions.ConnectionErrorException;
import exceptions.NodeIdAlreadyExistsException;
import exceptions.UnexpectedBehaviourException;
import node.Collector;
import node.Controller;
import node.Node;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;


import static java.lang.System.out;

public class Main {
    public static void main(String[] args) {

        // Local Port
        // Remote IP
        // Remote Port
        // Create/Join/Controller
        // Controller IP
        // Controller Port
        // Dim Finger Table

        int localPort = -1;
        String controllerIP = "";
        int controllerPort = -1;
        String joinIP = "";
        int joinPort = -1;
        int dimFingerTable = -1;
        int type = -1;

        Options options = new Options();

        Option localPortOpt = new Option("p", "port", true, "Local Port");
        localPortOpt.setRequired(true);
        options.addOption(localPortOpt);

        Option controllerIPOpt = new Option("cip", "contrip", true, "Controller IP");
        controllerIPOpt.setRequired(false);
        options.addOption(controllerIPOpt);

        Option controllerPortOpt = new Option("cp", "contrport", true, "Controller Port");
        controllerPortOpt.setRequired(false);
        options.addOption(controllerPortOpt);

        Option typeOpt = new Option("t", "type", true, "Type Node");
        typeOpt.setRequired(true);
        options.addOption(typeOpt);

        Option joinIpOpt = new Option("jip", "joinip", true, "Join IP");
        joinIpOpt.setRequired(false);
        options.addOption(joinIpOpt);

        Option joinPortOpt = new Option("jp", "joinport", true, "Join Port");
        joinPortOpt.setRequired(false);
        options.addOption(joinPortOpt);

        Option dimFingerTableOpt = new Option("d", "dim", true, "Dimension Finger Table");
        dimFingerTableOpt.setRequired(false);
        options.addOption(dimFingerTableOpt);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }


        localPort = Integer.parseInt(cmd.getOptionValue("port"));
        type = Integer.parseInt(cmd.getOptionValue("type"));

        Node node = null;

        switch (type) {
            case 0:
                Collector coll = new Collector();

                try (ServerSocket listener = new ServerSocket(localPort)) {
                    System.out.println("The Controller server is running on Port " + localPort + " ...");
                    while (true) {
                        Executors.newCachedThreadPool().submit(new Controller(listener.accept(), coll));
                    }
                } catch (IOException e) {

                }
                break;
            case 1:
                // Create
                controllerIP = cmd.getOptionValue("contrip");
                controllerPort = Integer.parseInt(cmd.getOptionValue("contrport"));
                try {
                    node = new Node(InetAddress.getLocalHost().getHostAddress(), localPort,
                            controllerIP, controllerPort);
                } catch (UnknownHostException e) {
                    throw new UnexpectedBehaviourException();
                }
                dimFingerTable = Integer.parseInt(cmd.getOptionValue("dim"));
                if (dimFingerTable <= 0) {
                    System.err.println("Dim Finger Table can not be negative");
                    return;
                }
                out.println("-----------------------------");
                out.println("Node Create : Local Port " + localPort + " - Dim " + dimFingerTable + " - ControllerIP " +controllerIP
                        + " - ControllerPort " + controllerPort);
                out.println("-----------------------------");
                node.create(dimFingerTable);
                break;
            case 2:
                //join
                //TODO Magari aggiungere controlli

                controllerIP = cmd.getOptionValue("contrip");
                controllerPort = Integer.parseInt(cmd.getOptionValue("contrport"));
                joinIP = cmd.getOptionValue("joinip");
                joinPort = Integer.parseInt(cmd.getOptionValue("joinport"));

                try {
                    node = new Node(InetAddress.getLocalHost().getHostAddress(), localPort,
                            controllerIP, controllerPort);
                } catch (UnknownHostException e) {
                    throw new UnexpectedBehaviourException();
                }

                out.println("-----------------------------");
                out.println("Node Join : Local Port " + localPort + " - ControllerIP " +controllerIP
                        + " - ControllerPort " + controllerPort + " - JoinIP " +joinIP + " - JoinPort " + joinPort);
                out.println("-----------------------------");

                try {
                    node.join(joinIP, joinPort);
                } catch (ConnectionErrorException e) {
                    out.println("Wrong ip address or port");
                } catch (NodeIdAlreadyExistsException e) {
                    out.println("Node Id already existent");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
