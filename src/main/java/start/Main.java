package start;

import exceptions.ConnectionErrorException;
import exceptions.NodeIdAlreadyExistsException;
import exceptions.UnexpectedBehaviourException;
import node.Collector;
import node.Controller;
import node.Node;
import node.NodeInterface;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.concurrent.Executors;

import static java.lang.System.err;
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

        int localPort;
        String controllerIP;
        int controllerPort;
        String joinIP;
        int joinPort;
        int dimFingerTable;
        int type;

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

        Option debugOpt = new Option("deb", "debug", false, "Debug option");
        debugOpt.setRequired(false);
        options.addOption(debugOpt);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            out.println(e.getMessage());
            formatter.printHelp("utility-name", options);

            System.exit(1);
        }

        localPort = Integer.parseInt(cmd.getOptionValue("port"));
        type = Integer.parseInt(cmd.getOptionValue("type"));

        Node node;

        switch (type) {
            case 0:
                Collector coll = new Collector();

                try (ServerSocket listener = new ServerSocket(localPort)) {
                    out.println("The Controller server is running on Port " + localPort + " ...");
                    while (true) {
                        Executors.newCachedThreadPool().submit(new Controller(listener.accept(), coll));
                    }
                } catch (IOException e){
                    throw new UnexpectedBehaviourException();
                }
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
                    err.println("Dim Finger Table can not be negative");
                    return;
                }
                out.println("-----------------------------");
                out.println("Node Create : Local Port " + localPort + " - Dim " + dimFingerTable + " - ControllerIP " +controllerIP
                        + " - ControllerPort " + controllerPort);
                out.println("-----------------------------");
                node.create(dimFingerTable);

                if (cmd.hasOption("debug"))
                    debugInterface(node);

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
            default:
                throw new UnexpectedBehaviourException();
        }
    }

    private static void debugInterface(Node node){
        Scanner in = new Scanner(System.in);
        boolean exit = false;
        while (!exit){
            //out.println("Select 'lookup', 'addKey', 'p' or 'exit'" );
            String choice = in.nextLine().toLowerCase();
            switch (choice) {
                case "p":
                    out.println(node);
                    out.println(node.getSocketManager());
                    break;
                case "exit":
                    exit = true;
                    break;
                default:
                    out.println("Command not valid or empty. Insert new command");
            }
        }
    }
}
