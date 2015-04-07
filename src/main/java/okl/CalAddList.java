package okl;

import java.util.Locale;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class CalAddList {
    static SimpleDateFormat miniFormat = new SimpleDateFormat("dd.MM.yyyy");
    public static void main(String[] args){
        DateFormat dateFormat = new SimpleDateFormat("d.MM.yyyy");
        long addDays = 80;
        long days = 10;
        long from = System.currentTimeMillis();
        Date fromDate = null;
        try{
            if ("-h".equals(args[0])){
                System.err.println("Aufruf: java CalAddList [days to add] [num days to print] [dd.mm.yyyy]");
                System.exit(0);
            }
            addDays = Long.parseLong(args[0]);
            if (args.length == 2){
                days = Long.parseLong(args[1]);
            }
            if (args.length == 3){
                fromDate = miniFormat.parse(args[2]);
                from = fromDate.getTime();
                System.out.println(from);
            }
        } catch (ArrayIndexOutOfBoundsException e){
            System.err.println("Aufruf: java CalAddList [days to add] [num days to print] [dd.mm.yyyy]");
        } catch (ParseException pe){
            System.err.println(pe);
        }
        System.out.println(String.format("Datum       + %d Tage", addDays));
        for (int i = 0; i < days; i++){
            long then = from + addDays * 3600L*24L*1000L;
            System.out.println(String.format("%s %s", dateFormat.format(from), dateFormat.format(then)));
            from += 3600*24*1000;
        }
    }
}

