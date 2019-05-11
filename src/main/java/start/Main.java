package start;

import controller.Collector;
import controller.SocketController;
import exceptions.ConnectionErrorException;
import exceptions.NodeIdAlreadyExistsException;
import exceptions.TimerExpiredException;
import exceptions.UnexpectedBehaviourException;
import controller.OldCollector;
import node.Node;
import node.NodeInterface;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;

import static java.lang.System.err;
import static java.lang.System.out;

public class Main {
    private static final String LOOKUP_COMMAND = "lookup";
    private static final String ADDKEY_COMMAND = "addkey";
    private static final String PRINT_COMMAND = "p";
    private static final String EXIT_COMMAND = "exit";
    private static final String FIND_COMMAND = "find";

    public static void main(String[] args) {

        // Local Port
        // Remote IP
        // Remote Port
        // Create/Join/OldController
        // OldController IP
        // OldController Port
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

        Option controllerIPOpt = new Option("cip", "controllerIp", true, "OldController IP");
        controllerIPOpt.setRequired(false);
        options.addOption(controllerIPOpt);

        Option controllerPortOpt = new Option("cp", "controllerPort", true, "OldController Port");
        controllerPortOpt.setRequired(false);
        options.addOption(controllerPortOpt);

        Option typeOpt = new Option("t", "type", true, "Type Node");
        typeOpt.setRequired(true);
        options.addOption(typeOpt);

        Option joinIpOpt = new Option("jip", "joinIp", true, "Join IP");
        joinIpOpt.setRequired(false);
        options.addOption(joinIpOpt);

        Option joinPortOpt = new Option("jp", "joinPort", true, "Join Port");
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
                OldCollector coll = new OldCollector();

                Collector collector = Collector.getController();
                try (ServerSocket listener = new ServerSocket(localPort)) {
                    out.println("The OldController server is running on Port " + localPort + " ...");
                    while (true) {
                        Socket nodeSocket = listener.accept();
                        Executors.newCachedThreadPool().submit(new SocketController(collector, nodeSocket));
                    }
                } catch (IOException e){
                    throw new UnexpectedBehaviourException();
                }
            case 1:
                // Create
                controllerIP = cmd.getOptionValue("controllerIp");
                controllerPort = Integer.parseInt(cmd.getOptionValue("controllerPort"));
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
                controllerIP = cmd.getOptionValue("controllerIp");
                controllerPort = Integer.parseInt(cmd.getOptionValue("controllerPort"));
                joinIP = cmd.getOptionValue("joinIp");
                joinPort = Integer.parseInt(cmd.getOptionValue("joinPort"));

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

                if (cmd.hasOption("debug"))
                    debugInterface(node);

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
                case PRINT_COMMAND:
                    out.println(node);
                    out.println(node.getSocketManager());
                    break;
                case LOOKUP_COMMAND:
                    out.println("Insert ID of node to find" );
                    Long id = Long.parseLong(in.nextLine().toLowerCase());
                    try {
                        NodeInterface prova = node.lookup(id);
                        out.println("Nodo cercato: " + prova.getNodeId());
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (TimerExpiredException e) {
                        out.println("Node not found");
                    }
                    break;
                case ADDKEY_COMMAND:
                    out.println("Insert ID of node to find" );
                    Long key = Long.parseLong(in.nextLine().toLowerCase());
                    Map.Entry<Long, Object> keyValue = new AbstractMap.SimpleEntry<>(key,2);
                    NodeInterface result=null;
                    try {
                        result = node.addKey(keyValue);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (TimerExpiredException e) {
                        out.println("impossible to add key");
                    }
                    out.println("KEY SAVED: " + result);
                    break;
                case FIND_COMMAND:
                    out.println("Insert key to find");
                    Long keyToFind = Long.parseLong(in.nextLine().toLowerCase());
                    Object value = null;
                    try {
                        value = node.findKey(keyToFind);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (TimerExpiredException e) {
                        out.println("Impossible to Find the Key");
                    }
                    if (value == null)
                        out.println("KEY NOT FOUND");
                    else
                        out.println("VALUE: " + value);
                    break;
                case EXIT_COMMAND:
                    try {
                        node.leave();
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (ConnectionErrorException e) {
                        e.printStackTrace();
                    }
                    exit = true;
                    break;
                default:
                    out.println("Command not valid or empty. Insert new command");
            }
        }
    }
}