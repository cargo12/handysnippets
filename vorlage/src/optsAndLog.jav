
    static void usage() {
        log (Level.SEVERE, "Options: ");
        log (Level.SEVERE, "  [-k(ey) <key>]");
        log (Level.SEVERE, "  [-e(nc)]      encode mode (default)");
        log (Level.SEVERE, "  [-d(ec)]      decode mode");
        log (Level.SEVERE, "  [-au(to)]     autokey encryption (append plain text to key)");
        log (Level.SEVERE, "  [-al(phabet)] <alphabet>");
        log (Level.SEVERE, "  [-l(oglevel)  <ERROR|WARNING|INFO|CONFIG|FINE|FINER|FINEST>]");
        log (Level.SEVERE, "  [-nolower]    don't use lower case letters in alphabet");
        log (Level.SEVERE, "  [-noupper]    don't use upper case letters in alphabet");
        log (Level.SEVERE, "  [-nonumerals] don't use numerals in alphabet");
        log (Level.SEVERE, "  [-noblank]    don't use blank character in alphabet");
        log (Level.SEVERE, "  [-nospecial]  don't use special characters :;.,!?@-+=#<>  in alphabet");
        log (Level.SEVERE, "  [<plain- or ciphertext>]");
        log (Level.SEVERE, "  interactive mode if no options are given");
        log (Level.SEVERE, "  default alphabet: " + LOWER+UPPER+NUMERALS+BLANK+SPECIAL);
    }

    static void parseOpt(String [] args){
            
        int a = 0;
        while(a < args.length){
            String opt = args[a];

            if (opt.startsWith("-")){
                // first options without argument
                if (opt.startsWith("-e")){
                    optEncrypt = true;
                    a++;
                    log (Level.CONFIG, "encode");
                } else if (opt.startsWith("-h")){
                    usage();
                    System.exit(0);
                } else if (opt.startsWith("-gui")){
                    optGui = true;
                    log (Level.CONFIG, "GUI version");
                } else {
                    // now options with 1 argument 
                    a++;
                    if (args.length <= a){
                        log (Level.WARNING, "Expecting 1 argument after " + opt);
                        continue;
                    }
                    String parm = args[a];
                    if (opt.startsWith("-al")){
                        optAlphabet = parm;
                        a++;
                        log (Level.CONFIG, "alphabet " + optAlphabet);
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
            } else { 
                String text = args[a++];
                optTexts.add(text);
                log (Level.CONFIG, "text: "+text);
            }
        }

    }

    static String dolog (Level level, String s){
        StringBuilder output = new StringBuilder()
        .append("[")
        .append(level)
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
                    System.err.print(String.format("         %s%n", ste.toString()));
                }
            }
        }
    }


