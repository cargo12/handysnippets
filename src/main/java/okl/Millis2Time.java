package okl;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

public class Millis2Time{
    static DateFormat inFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.GERMAN);
    static DateFormat outFormat = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
    static SimpleDateFormat miniFormat1 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
    static SimpleDateFormat miniFormat2 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    static SimpleDateFormat miniFormat3 = new SimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss'Z'Z");
    static SimpleDateFormat miniFormat4 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");
    static SimpleDateFormat miniFormat5 = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss Z");
    //static SimpleDateFormat miniFormat6 = new SimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss");
    static SimpleDateFormat miniFormat6 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // ISO 8601

    static List<DateFormat> dateFormatList = new ArrayList<DateFormat>();
    static {
        dateFormatList.add(miniFormat4);
        dateFormatList.add(miniFormat5);
        dateFormatList.add(miniFormat1);
        dateFormatList.add(miniFormat2);
        dateFormatList.add(miniFormat3);
        dateFormatList.add(inFormat);
        dateFormatList.add(miniFormat6);
    }

    static boolean mValueIsSeconds = false;

    private static Date tryDateFormat (DateFormat df, String dateStr){
        System.out.printf(" trying  %-30s :: ",((SimpleDateFormat)df).toPattern());
        Date date = null;
        try {
            date = df.parse(dateStr);
            //System.out.println(date.getTime());
        } catch (ParseException e) {
            System.out.println("Not in a recognized format: " + dateStr);
        }

        return date;
    }

    public static void main (String [] args) {
        outFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        if (args.length == 0) {
            final SimpleDateFormat myFormat = new SimpleDateFormat("EEEE dd. MMMM yyyy HH:mm:ss z");
            myFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            JFrame f = new JFrame();
            f.setTitle("Milliseconds to readable");
            f.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent we) {
                    System.exit(0);
                }
            });
            f.getContentPane().setLayout(new BorderLayout(5,5));
            final JTextField inputField = new JTextField();
            final JLabel dateLabel = new JLabel();
            final JCheckBox secondsCb = new JCheckBox();
            secondsCb.setToolTipText("Interpret number as seconds, not milliseconds.");
            f.add(inputField, BorderLayout.NORTH);
            f.add(secondsCb, BorderLayout.EAST);
            f.add(dateLabel, BorderLayout.CENTER);
            f.setSize(400, 80);
            f.setVisible(true);
            
            final Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard(); 
            
            
            inputField.addActionListener(new ActionListener() {
                
                public void actionPerformed(ActionEvent e) {
                    String text = inputField.getText();
                    long millis = 0L;
                    try {
                        millis = Long.parseLong(text);
                        if (mValueIsSeconds){
                            millis *= 1000;
                        }
                        dateLabel.setText(myFormat.format(millis));
                    } catch (NumberFormatException nfe) {
                        System.err.println("Not a long number: " + millis);
                        //nfe.printStackTrace();
                        try {
                            Date date = null;
                            try {
                                date = miniFormat1.parse(text);
                            } catch (ParseException pe1) {
                                date = miniFormat2.parse(text);
                            }
                            dateLabel.setText(String.valueOf(date.getTime()));
                            System.out.println(date.getTime());
                        } catch (Exception ex) {
                            System.err.println("action: " + ex);
                        }
                    } 

                }
            });
            
            inputField.addFocusListener(new FocusListener() {
                
                public void focusGained(FocusEvent e) {
                    Transferable transfer = systemClipboard.getContents( null );
                    try {
                        String data = (String) transfer.getTransferData( DataFlavor.stringFlavor );
                        inputField.setText((String)data);
                        long millis = 0L;
                        try {
                            millis = Long.parseLong(data);
                            if (mValueIsSeconds){
                                millis *= 1000;
                            }
                            String readable = myFormat.format(millis);
                            dateLabel.setText(readable);
                            System.out.println(data + " = " + readable);
                        } catch (NumberFormatException nfe) {
                            Date date = null;
                            try {
                                date = miniFormat1.parse(data);
                            } catch (ParseException pe1) {
                                date = miniFormat2.parse(data);
                            }
                            dateLabel.setText(String.valueOf(date.getTime()));
                            System.out.printf("%-23s = %s\n", data, date.getTime());

                            //System.err.println("Not a long number: " + millis);
                            //nfe.printStackTrace();
                        }
                    } catch (Exception ioe) {
                        System.err.println("focus: " + ioe);
                    }
                    /*
                    for(DataFlavor dataFlavor : transferData.getTransferDataFlavors()){ 
                      Object content = null;
                      try { 
                          content = transferData.getTransferData        ( dataFlavor ); 
                      } catch (Exception ioe) {
                          //
                      }
                      if (content instanceof String ){ 
                        inputField.setText((String)content);
                      }
                    }*/
                }

                public void focusLost(FocusEvent e) {
                }
            });

            secondsCb.addItemListener(new ItemListener(){

                public void itemStateChanged(ItemEvent e){
                    int stateChanged = e.getStateChange();
                    switch (stateChanged){
                        case ItemEvent.SELECTED:
                            mValueIsSeconds = true;
                            break;
                        case ItemEvent.DESELECTED:
                            mValueIsSeconds = false;
                            break;
                    }
                    inputField.requestFocus();
                }

            });

        } else {
            if (args[0].equals("-h")){
                System.out.println("[-h] help");
                System.out.println("[-r <date>] converts plain text date to millis, e.g. 2012-12-20T13:40:01");
                System.out.println("[-p] prints current time");
            } else if (args[0].equals("-p")){
                System.out.println(System.currentTimeMillis());
            } else if (args[0].equals("-r")){
                Date date = null;
                boolean ok = false;
                for (DateFormat df : dateFormatList){
                    date = tryDateFormat (df, args[1]);
                    if (date != null){
                        System.out.println(date.getTime());
                        ok = true;
                        //break;
                    }
                }
                if (!ok){
                    System.out.println("Not a date: " + args[1]);
                }
            } else {
                long millis = 0L;
                try {
                    millis = Long.parseLong(args[0]);
                } catch (NumberFormatException e) {
                    System.err.println("Not a long number: " + args[0]);
                }
                System.out.println(outFormat.format(millis));
            }
        }

    }
}
