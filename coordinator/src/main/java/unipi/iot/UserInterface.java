package unipi.iot;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.*;

import unipi.iot.sensors.TempManager;
import unipi.iot.sensors.ConsPowerManager;

public class UserInterface {
    private static final int nSubzones = 1;

    static Coordinator coordinator;
    static TempManager tManager;
    static ConsPowerManager cpManager;

    private static void printCommands() {
        System.out.println(
            "help <command> --> Shows the usage of a specified command. If empty shows all the commands.\n\n" +
            "get_temp [subzone_list] --> Shows the temperature of all the listed subzones. Every subzone should be separated by a comma. `-a` or an empty set shows all subzones.\n\n" +
            "set_temp [lower_bound] [upper_bound] [subzone_list] --> Changes the bounds in which the temperature will stay. Default: [18.00°C] [26.00°C]\n\n" +
            "get_pow [subzone_list] --> Shows the power consumed by all the listed subzones. Every subzone should be separated by a comma. `-a` or an empty set shows all subzones.\n\n" +
            "set_pow_bounds [lower_bound] [upper_bound] [subzone_list] --> Changes the bounds of `Underuse` and `Overload`. Default: [40kW] [100kW]\n\n" +
            "overview --> Shows a list that displays the general informations of the datacenter.\n\n" +
            "quit --> Exits from the application.\n"
            );
    }

    private static void printHelp(String cmd) {
        switch (cmd) {
            case "help": {
                System.out.println("help <command> --> Shows the usage of a specified command. If empty shows all the commands.\n" +
                                    "Example: help get_temp"
                );
                break;
            }
            case "get_temp": {
                System.out.println("get_temp [z=subzone_list] --> Shows the temperature of all the listed subzones. Every subzone should be separated by a comma. `-a` or an empty set shows all subzones.\n" +
                                    "Example 1: get_temp 1,3,45,2\n" +
                                    "Example 2: get_temp -a"
                );
                break;
            }
            case "set_temp": {
                System.out.println("set_temp [lower_bound] [upper_bound] [z=subzone_list] --> Changes the bounds in which the temperature will stay. Default: [18.00°C] [26.00°C]\n" +
                                    "Example 1: set_temp 20.00 28.00 2,3,4\n" +
                                    "Example 2: set_temp 20.00 28.00 -a"
                );
                break;
            }
            case "get_pow": {
                System.out.println("get_pow [subzone_list] --> Shows the power consumed by all the listed subzones. Every subzone should be separated by a comma. `-a` or an empty set shows all subzones.\n" +
                                    "Example 1: get_pow 1,3,45,2\n" +
                                    "Example 2: get_pow -a"
                );
                break;
            }
            case "set_pow_bounds": {
                System.out.println("set_pow_bounds [lower_bound] [upper_bound] [subzone_list] --> Changes the bounds of `Underuse` and `Overload`. Default: [40kW] [100kW]\n" +
                                    "Example 1: set_pow_bounds 50 90 1,6,45\n" +
                                    "Example 2: set_pow_bounds 50 90 -a"
                );
                break;
            }
            case "overview": {
                System.out.println("overview --> Shows a list that displays the general informations of the datacenter.\n" +
                                    "Example: overview"
                );
                break;
            }
            case "quit": {
                System.out.println("quit --> Exits from the application.\n" +
                                    "Example: quit"
                );
                break;
            }
            case "": {
                printCommands();
                break;
            }
            default: {
                System.out.println("Command not recognised!");
                break;
            }
        }
    }

    private static void parseInput(String cmd) {
        String parts[] = cmd.split(" ");

        switch (parts[0]) {
            case "help": {
                if(parts.length == 1)
                    printHelp("");
                else
                    printHelp(parts[1]);
                break;
            }
            case "get_temp": {
                if(parts.length == 1 || parts[1].equals("-a")){ // If there's no argument or the argument is "-a" print all
                    // PRINT ALL SUBZONE TEMP
                    tManager.get(null);
                    break;
                }
                String subzones[] = parts[1].split(",");
                int i_subzones[] = new int[subzones.length]; 

                try {
                    for (int i = 0; i < subzones.length; i++) {
                        i_subzones[i] = Integer.parseInt(subzones[i]);
                        if(i_subzones[i] > nSubzones){
                            System.out.println("Some subzones are nonexistent! Max number: " + nSubzones);
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Incorrect subzone format!");
                    break;
                }
                // PRINT TEMP OF SUBZONES FROM THE LIST [i_subzones]
                tManager.get(i_subzones);

                break;
            }
            case "set_temp": {
                if(parts.length != 4) {
                    System.out.println("Incorrect format! Try again.");
                    break;
                }

                try {
                    int lowTemp = Integer.parseInt(parts[1].replace(".", "").replace(",", ""));
                    int upTemp = Integer.parseInt(parts[2].replace(".", "").replace(",", ""));
                
                    // If the down temperature is higher than the up temperature i swap them
                    if(lowTemp > upTemp) {
                        int c = lowTemp;
                        lowTemp = upTemp;
                        upTemp = c;
                    }

                    if(parts[3].equals("-a")){

                        //CHANGE ALL SUBZONE TEMP
                        tManager.set(null, lowTemp, upTemp);
                        break;
                    }

                    String subzones[] = parts[3].split(",");
                    int i_subzones[] = new int[subzones.length]; 
                
                    for (int i = 0; i < subzones.length; i++) {
                        i_subzones[i] = Integer.parseInt(subzones[i]);
                        if(i_subzones[i] > nSubzones){
                            System.out.println("Some subzones are nonexistent! Max number: " + nSubzones);
                            return;
                        }
                    }

                    // CHANGE TEMP OF SUBZONES FROM THE LIST [i_subzones]
                    tManager.set(i_subzones, lowTemp, upTemp);
                    
                    break;

                } catch (NumberFormatException e) {
                    System.out.println("Incorrect format! Try again.");
                    break;
                }
            }
            case "get_pow": {
                if(parts.length == 1 || parts[1].equals("-a")){ // If there's no argument or the argument is "-a" print all
                    // PRINT ALL SUBZONE CONSUMED POWER
                    cpManager.get(null);
                    break;
                }
                String subzones[] = parts[1].split(",");
                int i_subzones[] = new int[subzones.length]; 

                try {
                    for (int i = 0; i < subzones.length; i++) {
                        i_subzones[i] = Integer.parseInt(subzones[i]);
                        if(i_subzones[i] > nSubzones){
                            System.out.println("Some subzones are nonexistent! Max number: " + nSubzones);
                            return;
                        }
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Incorrect subzone format!");
                    break;
                }
                // PRINT CONSUMED POWER OF SUBZONE FROM THE LIST [i_subzones]
                cpManager.get(i_subzones);
                break;
            }
            case "set_pow_bounds": {
                if(parts.length != 4) {
                    System.out.println("Incorrect format! Try again.");
                    break;
                }

                try {
                    int lowPow = Integer.parseInt(parts[1]) * 1000;
                    int upPow = Integer.parseInt(parts[2]) * 1000;
                
                    // If the down power is higher than the up power i swap them
                    if(lowPow > upPow) {
                        int c = lowPow;
                        lowPow = upPow;
                        upPow = c;
                    }

                    if(parts[3].equals("-a")){
                        //CHANGE ALL SUBZONE POWER BOUNDS
                        cpManager.set(null, lowPow, upPow);
                        break;
                    }

                    String subzones[] = parts[3].split(",");
                    int i_subzones[] = new int[subzones.length]; 
                
                    for (int i = 0; i < subzones.length; i++) {
                        i_subzones[i] = Integer.parseInt(subzones[i]);
                        if(i_subzones[i] > nSubzones){
                            System.out.println("Some subzones are nonexistent! Max number: " + nSubzones);
                            return;
                        }
                    }

                    // CHANGE TEMP OF SUBZONES FROM THE LIST [i_subzones]
                    cpManager.set(i_subzones, lowPow, upPow);
                    
                    break;
                } catch (NumberFormatException e) {
                    System.out.println("Incorrect format! Try again.");
                    break;
                }

            }
            case "overview": {
                
                // PRINT A TABLE CONTAINING THE OVERVIEW OF THE DATACENTER
                coordinator.overview();
                break;
            }
            case "quit": {
                System.out.println("Exiting the program...");
                System.exit(0);
            }
            case "": {
                break;
            }
            default: {
                System.out.println("Command not recognised!");
                break;
            }
        }
    }

    public static void main( String[] args ) {

        // I deactivate the Californium logging
        Logger californiumLogger = Logger.getLogger("org.eclipse.californium");
        californiumLogger.setLevel(Level.OFF);

        coordinator = new Coordinator();
        coordinator.start();

        tManager = (TempManager) coordinator.getSensorManager("temperature");
        cpManager = (ConsPowerManager) coordinator.getSensorManager("consumed_power");

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("**********************************************SMART DATACENTER**********************************************\n");
        printCommands();
        while(true) {
            System.out.print("-> ");
            try {
                parseInput(bufferedReader.readLine());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
