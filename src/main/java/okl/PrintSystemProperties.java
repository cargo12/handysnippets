package okl;

import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.Set;


public class PrintSystemProperties {

    private static int col1Width = 30;
    private static int col2Width = 45;

    public static void main (String[] args) throws Exception {

        if (args.length > 0){
            if (args[0].startsWith("-h")){
                System.err.println("allowed argument: number of characters for value column (default 45)");
                System.exit(0);
            }
            try {
                col2Width = Integer.parseInt(args[0]);
            } catch (NumberFormatException nfe){
                System.err.println("Argument " + args[0] + " :: " + nfe);
                System.exit(1);
            }
        }

        Properties props = System.getProperties();
        Set<String> sortedProps = new TreeSet<String>();
        sortedProps.addAll(props.stringPropertyNames());

        for (String p : sortedProps){
            String val = props.getProperty(p);
            //System.out.println("###val###"+val);
            String valFormatted = format(val);

            if (p.equals("line.separator")){
                int c = (int)val.charAt(0);
                valFormatted = String.format("(hex)%x (int)%d%n", c, c);
            }
            System.out.printf("%-"+col1Width+"s :: %s", p,
                valFormatted);
        }

        System.out.printf(". . . . . . . . . . . . . . . . . . . . . . . . . .%n");
        TimeZone tz = TimeZone.getDefault();
        System.out.printf("%-"+col1Width+"s :: %s", "default timezone ID", format(tz.getID()));
        System.out.printf("%-"+col1Width+"s :: %s", "default timezone display name", format(tz.getDisplayName()));
        long now = System.currentTimeMillis();
        System.out.printf("%-"+col1Width+"s :: %b%n", "default timezone inDaylight ", tz.inDaylightTime(new Date(now)));
        System.out.printf("%-"+col1Width+"s :: %d%n", "default timezone offset ", tz.getOffset(now));
        System.out.printf("%-"+col1Width+"s :: %d%n", "now ", now);

    }

    private static String format(String val){
            StringBuilder valFormatted = new StringBuilder();
            boolean firstLine = true;
            while (val.length() > col2Width){
                if (firstLine) {
                    valFormatted.append(String.format("%s%n", val.substring(0,
                        col2Width)));
                    firstLine = false;
                } else {
                    valFormatted.append(String.format("%"+col1Width+
                        "s    %s%n", "", val.substring(0, col2Width)));
                }
                val = val.substring(col2Width);
            }
            if (firstLine){
                valFormatted.append(String.format("%s%n", val));
            } else {
                valFormatted.append(String.format("%"+col1Width+
                    "s    %s%n", "", val));
            }
            return valFormatted.toString();
    }
}
