/**
 * Starts different classes, depending on command line option. Useful for starting different apps from same "java -jar" command.
 * e.g. java -jar VORLAGE.jar -gui
 */
public class Main {
    public static void main(String[] args){
        boolean gui = false;
        for (String s : args){
            if (s.startsWith("-gui")){
                gui = true;
                break;
            }
        }

        if (gui){
            VORLAGEGui.main(args);
        } else {
            VORLAGE.main(args);
        }
    }
}
