package okl;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;

public class FontTest extends JFrame {

    /**
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new FontTest();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontFamilyNames = ge.getAvailableFontFamilyNames();
        for(String s : fontFamilyNames){
            if (s.contains("Verdana"))
            System.out.println(s);
        }
        Font[] allFonts = ge.getAllFonts();
        for (Font f : allFonts){
            System.out.printf("%20s %20s\n", f.getFamily(), f.getName());
        }

    }

}
