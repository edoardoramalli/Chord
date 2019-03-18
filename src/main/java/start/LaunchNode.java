package start;

import exceptions.ConnectionErrorException;
import exceptions.UnexpectedBehaviourException;
import node.Node;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import static java.lang.System.out;

public class LaunchNode {
    private static final String CREATE_COMMAND = "create";
    private static final String JOIN_COMMAND = "join";
    private static final String LOOKUP_COMMAND = "lookup";
    private static final String ADDKEY_COMMAND = "addkey";
    private static final String PRINT_SUCCESSOR_PREDECESSOR_COMMAND = "printps";
    private static final String PRINT_FINGERTABLE_COMMAND = "printft";
    private static final String EXIT_COMMAND = "exit";
    private static Scanner in = new Scanner(System.in);

    public static void main(String[] args){
        Node node = null;
        //TODO andrebbero fatti dei controlli sugli inserimenti

        boolean exit = false;
        while (!exit){
            out.println("Select create or join");
            String choice = in.nextLine().toLowerCase();
            out.println("Select port"); //poi aggiungo controllo
            int mySocketPort =  Integer.parseInt(in.nextLine());
            try {
                node = new Node(InetAddress.getLocalHost().getHostAddress(), mySocketPort);
            } catch (UnknownHostException e) {
                throw new UnexpectedBehaviourException();
            }
            switch (choice){
                case CREATE_COMMAND:
                    out.println("Insert value of m:");
                    node.create(Integer.parseInt(in.nextLine()));
                    exit = true;
                    break;
                case JOIN_COMMAND:
                    out.println("Insert ip address of node");
                    String ipAddress = in.nextLine().toLowerCase();
                    out.println("Insert socket port of node");
                    int socketPort = Integer.parseInt(in.nextLine().toLowerCase());
                    try {
                        node.join(ipAddress, socketPort);
                        exit = true;
                    } catch (ConnectionErrorException e) {
                        out.println("Wrong ip address or port");
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
            out.println("Select 'lookup', 'addKey', 'printps', 'printft' or 'exit'" );
            String choice = in.nextLine().toLowerCase();
            switch (choice) {
                case LOOKUP_COMMAND:
                    //TODO da fare
                    break;
                case ADDKEY_COMMAND:
                    //TODO da fare
                    break;
                case PRINT_SUCCESSOR_PREDECESSOR_COMMAND:
                    try {
                        node.printPredecessorAndSuccessor();
                    } catch (IOException e) {
                        throw new UnexpectedBehaviourException();
                    }
                    break;
                case PRINT_FINGERTABLE_COMMAND:
                    try {
                        node.printFingerTable();
                    } catch (IOException e) {
                        throw new UnexpectedBehaviourException();
                    }
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
