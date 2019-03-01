package start;

import node.Node;

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
        node = new Node(in.nextLine().toLowerCase());
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
                    node.join(new Node("aa")); //Qui va modificato
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
