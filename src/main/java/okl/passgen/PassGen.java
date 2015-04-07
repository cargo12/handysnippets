package okl.passgen;

import java.util.logging.Level;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.io.IOException;

/**
 * Original: http://www.kessnux.net/anwendungen/passwort-generator.php
 * 0.7.0 2011
 */
public class PassGen {
    private static Level logLevel = Level.INFO;
    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.S z");

    static void alert(String s) {
        System.out.println(s);
    }

    static void generieren(boolean useKlein, boolean useGross, boolean useZahl,
            boolean useSonder, boolean useOhne, int laenge, int anzahl,
            String sozei) {
        String zeichen = "";
        String raus_alt = "";
        if (!useKlein && !useGross && !useZahl && !useSonder) {
            log(Level.SEVERE, "Fehler! Es muss mindestens ein Zeichenbereich ausgewaehlt sein.");
        } else {
            if (laenge < 1 || anzahl < 1) {
                log(Level.SEVERE, "Fehler! Laenge und Anzahl muessen groesser als 0 sein.");
            } else {
                if (useKlein) {
                    if (useOhne) {
                        zeichen = zeichen + "abcdefghijkmnopqrstuvwxyz";
                    } else {
                        zeichen = zeichen + "abcdefghijklmnopqrstuvwxyz";
                    }
                };
                if (useGross) {
                    if (useOhne) {
                        zeichen = zeichen + "ABCDEFGHJKLMNPQRSTUVWXYZ";
                    } else {
                        zeichen = zeichen + "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
                    }
                };
                if (useZahl) {
                    if (useOhne) {
                        zeichen = zeichen + "123456789";
                    } else {
                        zeichen = zeichen + "1234567890";
                    }
                };
                if (useSonder) {
                    zeichen = zeichen + sozei;
                };
                log(Level.FINE, "Zeichen: "+zeichen);
                String arr[] = new String[zeichen.length()];
                String raus = "";
                for (int i = 0; i < zeichen.length(); i++)
                    arr[i] = zeichen.substring(i, i+1);
                for (int j = 0; j < anzahl; j++) {
                    for (int k = 0; k < laenge; k++) {
                        int auswahl;
                        for (auswahl = 100; auswahl >= zeichen.length(); auswahl++) {
                            auswahl = (int) Math.round(Math.random() * 100);
                        }
                        if (!raus_alt.equals(arr[auswahl])) {
                            raus = raus + arr[auswahl];
                            raus_alt = arr[auswahl];
                        } else {
                            k--;
                        }
                    }
                    if (j!=anzahl-1){
                        raus = raus + "\n";
                    }
                }
                alert(raus);
            }
        }
    };

    static String dolog (Level level, String s){
        StringBuilder output = new StringBuilder()
        .append("[")
        .append(level).append('|')
        .append(Thread.currentThread().getName()).append('|')
        .append(dateFormat.format(System.currentTimeMillis()))
        .append("]: ")
        .append(s).append(' ')
        .append(System.getProperty("line.separator"));
        return output.toString();
    }
    
    static String dolog (Level level, String s, Exception e){
        return dolog(level, s) + " " + e.toString();
    }
    
    static void log (Level level, String s){
        if (level.intValue() >= logLevel.intValue()){
            if (level.intValue() >= Level.WARNING.intValue()){
                System.err.print(dolog(level, s));
            } else {
                System.out.print(dolog(level, s));
            }
        }
    }
 
    static void log (Level level, String s, Exception e){
        if (level.intValue() >= logLevel.intValue()){
            System.err.println(dolog(level, s, e));
            if (logLevel.intValue() <= Level.FINE.intValue()){
                for (StackTraceElement ste : e.getStackTrace()){
                    System.err.print(String.format("         %s : %n", ste.toString(), ste.getLineNumber()));
                }
            }
        }
    }

    private static void usage() {
        log (Level.SEVERE, "Optionen: ");
        log (Level.SEVERE, "  [-noklein]");
        log (Level.SEVERE, "  [-nogross]");
        log (Level.SEVERE, "  [-nozahl]");
        log (Level.SEVERE, "  [-nosonder]");
        log (Level.SEVERE, "  [-noohne]  auch 1/l/0/O benutzen");
        log (Level.SEVERE, "  [-laenge]");
        log (Level.SEVERE, "  [-anzahl]");
        log (Level.SEVERE, "  [-sozei <erlaubte sonderzeichen>] ");
        
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        boolean useKlein = true;
        boolean useGross = true;
        boolean useZahl = true;
        boolean useSonder = true;
        boolean useOhne = true;
        int optLaenge = 8;
        int optAnzahl = 10;
        String optSozei = "!$,.:;";

        int a = 0;
        while(a < args.length){
            String opt = args[a];

            //boolean useKlein, boolean useGross, boolean useZahl,
            //boolean useSonder, boolean useOhne, int laenge, int anzahl,
            //String sozei
            if (opt.startsWith("-")){
                // first options without argument
                if (opt.startsWith("-noklein")){
                    useKlein = false;
                    a++;
                    log (Level.CONFIG, "keine Kleinbuchstaben");
                } else if (opt.startsWith("-nogross")){
                    useGross = false;
                    a++;
                    log (Level.CONFIG, "keine Grossbuchstaben");
                } else if (opt.startsWith("-nozahl")){
                    useZahl = false;
                    a++;
                    log (Level.CONFIG, "keine Zahlen");
                } else if (opt.startsWith("-nosonder")){
                    useSonder = false;
                    a++;
                    log (Level.CONFIG, "keine Sonderzeichen");
                } else if (opt.startsWith("-noohne")){
                    useOhne = false;
                    a++;
                    log (Level.CONFIG, "keine  1/l 0/O");
                } else if (opt.startsWith("-h")){
                    usage();
                    System.exit(0);
                } else {
                    // now options with 1 argument 
                    a++;
                    String parm = args[a];
                    if (opt.startsWith("-laenge")){
                        try { 
                            optLaenge = Integer.parseInt(parm);
                            a++;
                        } catch (NumberFormatException nfe){
                            log(Level.SEVERE, "Could not parse laenge "+parm);
                            System.exit(1);
                        }
                        log (Level.CONFIG, "Laenge " + optLaenge);
                    } else if (opt.startsWith("-anzahl")){
                        try { 
                            optAnzahl = Integer.parseInt(parm);
                            a++;
                        } catch (NumberFormatException nfe){
                            log(Level.SEVERE, "Could not parse anzahl "+parm);
                            System.exit(1);
                        }
                        log (Level.CONFIG, "Anzahl " + optAnzahl);
                    } else if (opt.startsWith("-sozei")){
                        optSozei = parm;
                        a++;
                        log (Level.CONFIG, "Sonderzeichen: " + optSozei);
                    } else if (opt.startsWith("-l")){ // log level
                        if (parm.startsWith("FINEST")){
                            logLevel = Level.FINEST;
                        } else if (parm.startsWith("FINER")){
                            logLevel = Level.FINER;
                        } else if (parm.startsWith("FINE")){
                            logLevel = Level.FINE;
                        } else if (parm.startsWith("CONFIG")){
                            logLevel = Level.CONFIG;
                        } else if (parm.startsWith("INFO")){
                            logLevel = Level.INFO;
                        } else if (parm.startsWith("WARNING")){
                            logLevel = Level.WARNING;
                        } else if (parm.startsWith("SEVERE")){
                            logLevel = Level.SEVERE;
                        }
                        log (Level.CONFIG, "logniveau " + logLevel.getName());
                        a++;
                    } else if (opt.startsWith("-")){
                        log (Level.WARNING, "Unknown option: " + opt);
                        usage();
                        System.exit(1);
                    }
                }
            } else { // files to send
            }
        }
        generieren(useKlein, useGross, useZahl, useSonder, useOhne, optLaenge, optAnzahl, optSozei);

    }

}
