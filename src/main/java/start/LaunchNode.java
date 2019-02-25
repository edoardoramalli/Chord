package start;

import node.Node;

import java.util.Scanner;

import static java.lang.System.out;

public class LaunchNode {
    private static final String CREATE_COMMAND = "create";
    private static final String JOIN_COMMAND = "join";
    private static Scanner in = new Scanner(System.in);
    private static Node node;

    public static void main(String[] args){
        out.println("Insert ip address");
        node = new Node(in.nextLine().toLowerCase());
        Boolean read = false;
        while (!read){
            out.println("Select create or join");
            String choice = in.nextLine().toLowerCase();
            switch (choice){
                case CREATE_COMMAND:
                    node.create();
                    read = true;
                    break;
                case JOIN_COMMAND:
                    node.join(new Node("aa")); //Qui va modificato
                    read = true;
                    break;
                default:
                    out.println("Command not valid or empty. Insert new command");
            }
        }
    }
}
