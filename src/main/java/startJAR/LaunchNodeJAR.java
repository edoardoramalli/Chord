package startJAR;

import exceptions.ConnectionErrorException;
import exceptions.NodeIdAlreadyExistsException;
import exceptions.UnexpectedBehaviourException;
import node.Node;
import node.NodeInterface;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Scanner;

import static java.lang.System.out;

public class LaunchNodeJAR {
    private static final String CREATE_COMMAND = "create";
    private static final String JOIN_COMMAND = "join";
    private static final String LOOKUP_COMMAND = "lookup";
    private static final String ADDKEY_COMMAND = "addkey";
    private static final String PRINT_COMMAND = "p";
    private static final String EXIT_COMMAND = "exit";
    private static final String FIND_COMMAND = "find";
    private static Scanner in = new Scanner(System.in);

    public static void main(String[] args){
        Node node = null;
        //TODO andrebbero fatti dei controlli sugli inserimenti
        boolean exit = false;
        while (!exit){
            out.println("Select create or join");
            String choice = args[0];
            out.println("Select port"); //poi aggiungo controllo
            int mySocketPort =  Integer.parseInt(args[1]);
            try {
                node = new Node(InetAddress.getLocalHost().getHostAddress(), mySocketPort);
            } catch (UnknownHostException e) {
                throw new UnexpectedBehaviourException();
            }
            switch (choice){
                case CREATE_COMMAND:
                    out.println("Insert value of m:");
                    node.create(Integer.parseInt(args[2]));
                    exit = true;
                    break;
                case JOIN_COMMAND:
                    out.println("Insert ip address of node");
                    String ipAddress = args[2];
                    out.println("Insert socket port of node");
                    int socketPort = Integer.parseInt(args[3]);
                    try {
                        node.join(ipAddress, socketPort);
                        exit = true;
                    } catch (ConnectionErrorException e) {
                        out.println("Wrong ip address or port");
                    } catch (NodeIdAlreadyExistsException e) {
                        out.println("Node Id already existent");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    out.println("Command not valid or empty. Insert new command");
            }
        }
        exit = false;
        while (!exit){
            out.println("Select 'lookup', 'addKey', 'ps', 'ft' or 'exit'" );
            String choice = in.nextLine().toLowerCase();
            switch (choice) {
                case LOOKUP_COMMAND:
                    out.println("Insert ID of node to find" );
                    Long id = Long.parseLong(in.nextLine().toLowerCase());
                    try {
                        NodeInterface searchedNode = node.lookup(id);
                        out.println("Searched Node: " + searchedNode.getNodeId()+ " " + searchedNode.getIpAddress());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //TODO da fare
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
                    }
                    System.out.println("KEY SAVED: " + result);
                    break;
                case FIND_COMMAND:
                    System.out.println("Insert key to find");
                    Long keyToFind = Long.parseLong(in.nextLine().toLowerCase());
                    Object value = null;
                    try {
                        value = node.findKey(keyToFind);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (value==null)
                        System.out.println("KEY NOT FOUND");
                    else
                        System.out.println("VALUE: " + value);

                    break;
                case PRINT_COMMAND:
                    out.println(node);
                    break;
                case EXIT_COMMAND:
                    exit = true;
                    break;
                default:
                    out.println("Command not valid or empty. Insert new command");
            }
        }
    }
}