package start;

import exceptions.ConnectionErrorException;
import exceptions.UnexpectedBehaviourException;
import node.Node;

import java.io.IOException;
import java.util.Scanner;

import static java.lang.System.out;

public class LaunchNode {
    private static final String CREATE_COMMAND = "create";
    private static final String JOIN_COMMAND = "join";
    private static final String LOOKUP_COMMAND = "lookup";
    private static final String ADDKEY_COMMAND = "addkey";
    private static final String EXIT_COMMAND = "exit";
    private static Scanner in = new Scanner(System.in);
    private static Node node;

    public static void main(String[] args){
        out.println("Insert ip address");
        node = new Node(in.nextLine().toLowerCase()); //TODO andrebbero fatti dei controlli sugli inserimenti
        boolean exit = false;
        while (!exit){
            out.println("Select create or join");
            String choice = in.nextLine().toLowerCase();
            switch (choice){
                case CREATE_COMMAND:
                    node.create();
                    exit = true;
                    break;
                case JOIN_COMMAND:
                    out.println("Insert ip address of node");
                    String ipAddress = in.nextLine().toLowerCase();
                    out.println("Insert socket port of node");
                    int socketPort = Integer.parseInt(in.nextLine().toLowerCase());
                    try {
                        node.join(ipAddress, socketPort); //Qui va modificato
                    } catch (ConnectionErrorException e) {
                        out.println("Wrong ip address or port");
                    } catch (IOException e) {
                        throw new UnexpectedBehaviourException();
                    }
                    exit = true;
                    break;
                default:
                    out.println("Command not valid or empty. Insert new command");
            }
        }
        exit = false;
        while (!exit){
            out.println("Select lookup or addKey");
            String choice = in.nextLine().toLowerCase();
            switch (choice) {
                case LOOKUP_COMMAND:
                    //TODO da fare
                    break;
                case ADDKEY_COMMAND:
                    //TODO da fare
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
