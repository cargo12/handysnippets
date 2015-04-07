package okl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.Deflater;


/**
 * De- and encode unencrypted TomTom .ov2 POI files.
 * The format is described in the file ttnavsdk3_manual.pdf
 * Character encoding MUST be ISO-8859-1/Latin-1, so this is enforced when
 * reading and writing files. Redirecting output to stdout is not recommended
 * because an encoding conversion might happen.
 * http://www.tomtom.com/lib/doc/ttnavsdk3_manual.pdf
 */
public class Ov2{

    private static final String VERSION = "9.9.2011 09:19";
    private static Level logLevel = Level.WARNING;
    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.S z");
    private static boolean printOsm = false;
    private static boolean printCsv = true;
    private static char FS = ',';
    private static char COMMENT = ';';
    private static int DELETED = 0;
    private static int SKIPPER = 1;
    private static int SIMPLE_POI = 2;
    private static int EXTENDED_POI = 3;

    private final static void println(BufferedWriter bw,  String s) throws IOException {
        if (bw != null) {
            bw.append(s);
            bw.newLine();
        } else {
            System.out.println(s);
        }
    }

    public static final List<Record> decode (File f, File outfileDecode) {
        BufferedInputStream is = null;
        List<Record> ov2list = new ArrayList<Record>();

        BufferedWriter os = null;
        
        try {
            is = new BufferedInputStream(new FileInputStream(f)); 
            if (outfileDecode != null){
                os = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(outfileDecode), "ISO-8859-1"));
            }
            if (printCsv) { 
                println(os, COMMENT+" Readable locations in " + f.getName());
                println(os, String.valueOf(COMMENT));
                if (printCsv){
                    println(os, COMMENT+" Longitude,    Latitude, \"Name\"");
                    println(os, COMMENT+" ========== ============ ==================================================");
                } else {
                    println(os, COMMENT+" Type,   Len,   Longitude,    Latitude, \"Name\"");
                    println(os, COMMENT+" ===== ====== ============ ============ ==================================================");
                }
                println(os, "");
            }
            
            boolean ende=false;
            int type = -1;
            int l = 0;

            if ((type = is.read()) == -1) {ende=true;}
            if (type == 100){
                log(Level.FINE, "skipping 21 bytes");
                for (int i = 0; i < 21; i++) {
                    if ((type = is.read()) == -1) {ende=true;}
                }
            }
            log(Level.FINER, "Type now="+type);
            /*
            do {
                if ((type = is.read()) == -1) {ende=true;}
                log(Level.FINEST, "skipping "+type);
            } while (!ende && ((type < 1)  || (type > 3)));
            */
            while (!ende){
                if (type == -1){
                    ende = true;
                    break;
                }
                int len = is.read() + (is.read()<<8) + (is.read()<<16) + (is.read()<<24);
                log(Level.FINER, "len="+len);
    
                if (type == SIMPLE_POI || type == DELETED){ // simple POI
                    if (type == DELETED) log(Level.FINER, "deleted record");
                    if (type == SIMPLE_POI) log(Level.FINER, "simple POI record");
                    int x = (is.read() + (is.read()<<8) + (is.read()<<16) + (is.read()<<24));
                    int y = (is.read() + (is.read()<<8) + (is.read()<<16) + (is.read()<<24));
                    int c = 0;
                    StringBuilder name = new StringBuilder();
                    while ((c=is.read()) != 0){
                        if (c==-1) {
                            ende = true;
                            break;
                        }
                        name.append((char)c);
                    }
                    String s = String.format("x=[%d] y=[%d] name=[%s]", x, y, name.toString());
                    log(Level.FINER, s);
                    if (!ende){
                        Record r = new Record(type, len, x, y, name.toString());
                        ov2list.add(r);
                        if (printCsv) { 
                            println(os, r.toCSV());
                        } else if (printOsm) {
                            println(os, r.toOpenStreetMapPermaLink());
                        } else {
                            println(os, r.toString());
                        }
                    }
                } else if (type == SKIPPER) { // skipper record
                    int x1 = (is.read() + (is.read()<<8) + (is.read()<<16) + (is.read()<<24));
                    int y1 = (is.read() + (is.read()<<8) + (is.read()<<16) + (is.read()<<24));
                    int x2 = (is.read() + (is.read()<<8) + (is.read()<<16) + (is.read()<<24));
                    int y2 = (is.read() + (is.read()<<8) + (is.read()<<16) + (is.read()<<24));
                    log(Level.FINER, String.format("skipper record: %4d x1=%-3.5f y1=%-3.5f x2=%-3.5f y2=%-3.5f", len, x1 / 100000.0f, y1 / 100000.0f, x2 / 100000.0f, y2 / 100000.0f));
                } else if (type == EXTENDED_POI) { // extended poi
                    int x = (is.read() + (is.read()<<8) + (is.read()<<16) + (is.read()<<24));
                    int y = (is.read() + (is.read()<<8) + (is.read()<<16) + (is.read()<<24));
                    int c = 0;
                    StringBuilder name = new StringBuilder();
                    while ((c=is.read()) != 0){
                        if (c==-1) {
                            ende = true;
                            break;
                        }
                        name.append((char)c);
                    }
                    StringBuilder uniqueId = new StringBuilder();
                    while ((c=is.read()) != 0){
                        if (c==-1) {
                            ende = true;
                            break;
                        }
                        uniqueId.append((char)c);
                    }
                    StringBuilder extra = new StringBuilder();
                    while ((c=is.read()) != 0){
                        if (c==-1) {
                            ende = true;
                            break;
                        }
                        extra.append((char)c);
                    }
                    String s = String.format("x=[%d] y=[%d] name=[%s] unique ID=[%s] extra=[%s]", x, y, name.toString(), uniqueId.toString(), extra.toString());
                    log(Level.FINER, s);
                    if (!ende){
                        Record r = new Record(type, len, x, y, name.toString(), uniqueId.toString(), extra.toString());
                        ov2list.add(r);
                        if (printCsv) { 
                            println(os, r.toCSV());
                        } else if (printOsm) {
                            println(os, r.toOpenStreetMapPermaLink());
                        } else {
                            println(os, r.toString());
                        }
                    }

                } else {
                    log(Level.WARNING, "Unknown type " + type);
                }
                    
                    
                type = is.read();
                log(Level.FINER, "type="+type);
            }
        } catch (FileNotFoundException fne){
            log(Level.SEVERE, "File not found: " + f);
        } catch (IOException ioe){
          log(Level.SEVERE, "IOException: "+ioe.getMessage(), ioe);  
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe){
                log(Level.SEVERE, "IOException: "+ioe.getMessage(), ioe);  
            }
            try {
                if (os != null) os.close();
            } catch (IOException ioe){
                log(Level.SEVERE, "IOException: "+ioe.getMessage(), ioe);  
            }
        }

        return ov2list;
    }
    
    final public static void encode (File txtfile, File ov2file) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(txtfile), "ISO-8859-1")); //!!!
        //BufferedOutputStream os = null;
        BufferedWriter os = null;
        try {
            //os = new BufferedOutputStream(new FileOutputStream(ov2file));
            os = new BufferedWriter(new OutputStreamWriter( new FileOutputStream(ov2file), "ISO-8859-1"));
            
            String s = null;
            while ((s =br.readLine()) != null) {
                if (s.startsWith(";")) continue;
                if (s.length() == 0) continue;

                String[] sa = s.split(FS+"", 3);
                //int type = Integer.parseInt(sa[0].trim());
                //int len = Integer.parseInt(sa[1].trim());
                float x = Float.parseFloat(sa[0].trim());
                float y = Float.parseFloat(sa[1].trim());
                String name = sa[2].trim();
                if (name.startsWith("\"")) name = name.substring(1);
                if (name.endsWith("\"")) name = name.substring(0, name.lastIndexOf("\""));
                // len comprises header length + name length + 0 terminator byte
                int len = 14 + name.length();
                Record r = new Record(SIMPLE_POI, len, x, y, name);
                log(Level.FINER, r.toString());
    
                os.write(r.type);
                os.flush();
                
                int i = (int)(r.len);
                os.write(i & 0xff);
                os.write((i & 0xff00)>>8);
                os.write((i & 0xff0000)>>8);
                os.write((i & 0xff000000)>>8);
                os.flush();
                
                i = (int)(r.xi);
                os.write(i & 0xff);
                os.write((i & 0xff00)>>>8);
                os.write((i & 0xff0000)>>>16);
                os.write((i & 0xff000000)>>>24);
                os.flush();
                
                i = (int)(r.yi);
                os.write(i & 0xff);
                os.write((i & 0xff00)>>>8);
                os.write((i & 0xff0000)>>>16);
                os.write((i & 0xff000000)>>>24);
                
                // TODO: not unicode aware
                os.write(r.name);
                os.write(0);
                os.flush();
   
            }
                
        } finally {
            if (os != null) os.close();
            if (br != null) br.close();
        }
    }
    

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
            if (level.intValue() >= Level.FINEST.intValue()){
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
        log (Level.SEVERE, "Version: "+VERSION);
        log (Level.SEVERE, "Usage: java -cp . Ov2");
        log (Level.SEVERE, "  -h                     --> this help");
        log (Level.SEVERE, "  -osm                   --> print OpenStreetMap permalink");
        log (Level.SEVERE, "  -csv                   --> print comma separated records (default)");
        log (Level.SEVERE, "  -nocsv                 --> don't print comma separated records");
        log (Level.SEVERE, "  -0                     --> print ASCII zero instead of \""+FS+ "\" as field separator in CSV");
        log (Level.SEVERE, "  -o <output file for decode>");
        log (Level.SEVERE, "  -d <file_to_decode>    --> default mode");
        log (Level.SEVERE, "  -e <file_to_encode> <output_file>");
        log (Level.SEVERE, "NB: don't redirect decode output to terminal, might mess up encoding");
    }

    public static void main(String[] args) throws Exception {
        
        if (args.length == 0){
            usage();
            System.exit(0);
        }

        File infile = null;
        File outfile = null;
        File outfileDecode = null;
        boolean decodeMode = true;
        
        int a = 0;
        while(a < args.length){
            String opt = args[a];
            if (opt.startsWith("-")){
                // first options without argument
                if (opt.startsWith("-osm")){ 
                    printOsm = true;
                    printCsv = false;
                    log(Level.CONFIG, "Setting OpenStreetMap link");
                    a++;
                } else if (opt.startsWith("-csv")){ 
                    printCsv = true;
                    log(Level.CONFIG, "Setting csv");
                    a++;
                } else if (opt.startsWith("-nocsv")){ 
                    printCsv = false;
                    log(Level.CONFIG, "Setting no csv");
                    a++;
                } else if (opt.startsWith("-h")){ 
                    usage();
                    System.exit(0);
                } else if (opt.startsWith("-0")){ 
                    log(Level.CONFIG, "Setting zero as FS");
                    FS = '\0';
                    a++;
                } else {
                    // now options with 1 argument 
                    a++;
                    if (a >= args.length){
                        usage();
                        continue;
                    }
                    String parm = args[a];
                    if (opt.startsWith("-d")){ 
                           infile = new File(parm); 
                           a++;
                        log (Level.CONFIG, "Setting input ov2 file to " + infile);
                    } else if (opt.startsWith("-e")){ 
                        decodeMode = false;
                        infile = new File(parm); 
                        a++;
                        if (a >= args.length || args[a].startsWith("-")){
                            log(Level.SEVERE, "-e expects two parameters");
                            usage();
                            System.exit(1);
                        }
                        parm = args[a];
                        outfile = new File(parm);
                        a++;
                        log (Level.CONFIG, "Setting input text file to " + infile);
                        log (Level.CONFIG, "Setting input ov2 file to " + outfile);
                    } else if (opt.startsWith("-o")){ 
                        parm = args[a];
                        outfileDecode = new File(parm);
                        a++;
                        log (Level.CONFIG, "Setting output file for decode to " + outfileDecode);
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
                        log (Level.CONFIG, "Setting log level to " + logLevel.getName());
                        a++;
                    } else if (opt.startsWith("-")){
                        log (Level.WARNING, "Unknown option: " + opt);
                        usage();
                        System.exit(1);
                    }
                }
            }  else { // must be an input file, decode
                infile = new File(opt); 
                a++;
                log (Level.CONFIG, "Setting input ov2 file to " + infile);
            }
        }
        
        if (decodeMode){
            List<Record> records = decode (infile, outfileDecode);
            log(Level.INFO, records.size()+" records decoded.");
            if (printCsv) { 
                System.out.println();
                System.out.println(COMMENT+" "+ records.size()+" records decoded.");
            }
        } else {
            //log(Level.WARNING, "encoding not yet supported");
            encode(infile, outfile);
            log(Level.INFO, "Finished encoding.");
        }
    }
    
    static class Record {
        int type = -1;
        int len = 0;
        int xi = 0; // microdegrees
        int yi = 0; // microdegrees
        float x = 0.0f; // wgs84
        float y = 0.0f; // wgs84
        String name = "";
        String uniqueId = "";
        String extra = "";
        
        public Record (int type, int len, int xi, int yi, String name){
            this.type = type;
            this.len = len;
            this.xi = xi;
            this.yi = yi;
            this.x = xi / 100000.0f;
            this.y = yi / 100000.0f;
            this.name = name;
            if (name.contains(";")){
                log(Level.WARNING, "Name contains semicolon, will cause problems with CSV format! "+name);
            }
        }

        public Record (int type, int len, int xi, int yi, String name, String uniqueId, String extra){
            this.type = type;
            this.len = len;
            this.xi = xi;
            this.yi = yi;
            this.x = xi / 100000.0f;
            this.y = yi / 100000.0f;
            this.name = name;
            if (name.contains(";")){
                log(Level.WARNING, "Name contains semicolon, will cause problems with CSV format! "+name);
            }
            this.uniqueId = uniqueId;
            this.extra = extra;
        }

        public Record (int type, int len, float x, float y, String name){
            this.type = type;
            this.len = len;
            this.x = x * 100000;
            this.y = y * 100000;
            this.xi = (int)this.x;
            this.yi = (int)this.y;
            this.name = name;
        }
        
        
        public String toString(){
            if ("".equals(uniqueId)){
                return String.format("%d %-4d %3.5f %2.5f  %s", type, len, x, y, name);
            } else {
                return String.format("%d %-4d %3.5f %2.5f  \"%s\" \"%s\" \"%s\" ", type, len, x, y, name, uniqueId, extra);
            }
        }

        public String toCSV(){
            //return String.format("%5d %s %4d %s %10.5f %s %10.5f %s \"%s\"", type, FS, len, FS, x, FS, y, FS, name);
            if ("".equals(uniqueId)){
                return String.format("%10.5f %s %10.5f %s \"%s\"", x, FS, y, FS, name);
            } else {
                return String.format("%10.5f %s %10.5f %s \"%s\" %s \"%s\"", x, FS, y, FS, name, FS, uniqueId);
            }
        }

        public String toOpenStreetMapPermaLink(){
            return name + "  " + "http://www.openstreetmap.org/?mlat="+y+"&mlon="+x+"&zoom=14&layers=M";
        }
    }
    
    

}
