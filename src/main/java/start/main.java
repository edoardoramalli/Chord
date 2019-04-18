/*package start;

import com.sun.xml.internal.bind.v2.TODO;
import org.apache.commons.cli.*;

public class main {
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
        controllerIPOpt.setRequired(true);
        options.addOption(controllerIPOpt);

        Option controllerPortOpt = new Option("cp", "contrport", true, "Controller Port");
        controllerPortOpt.setRequired(true);
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
        controllerIP = cmd.getOptionValue("contrip");
        controllerPort = Integer.parseInt(cmd.getOptionValue("contrport"));
        type = Integer.parseInt(cmd.getOptionValue("type"));

        switch (type) {
            case 0:
                // Controller
                break;
            case 1:
                // Create
                dimFingerTable = Integer.parseInt(cmd.getOptionValue("dim"));
                if (dimFingerTable <= 0) {
                    System.err.println("Dim Finger Table can not be negative");
                }
                break;
            case 2:
                //join
                //TODO Magari aggiungere controlli
                joinIP = cmd.getOptionValue("joinip");
                joinPort = Integer.parseInt(cmd.getOptionValue("joinport"));
                break;
        }


        System.out.println(localPort);
        System.out.println(controllerIP);
        System.out.println(controllerPort);
        System.out.println(type);

        System.out.println(joinIP);
        System.out.println(joinPort);
        System.out.println(dimFingerTable);


    }
}
*/