package okl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

public class RegexTestHarness {

    public static void main(String[] args){

        while (true) {

            String inputValue = JOptionPane.showInputDialog("Enter your regex: ");
            Pattern pattern = 
            Pattern.compile(inputValue);
            System.out.println("regex: "+inputValue);

            inputValue = JOptionPane.showInputDialog("Enter input string to search: ");
            Matcher matcher = 
            pattern.matcher(inputValue);
            System.out.println("string: "+inputValue);

            boolean found = false;
            while (matcher.find()) {
                System.out.println("I found the text "+ matcher.group()+" starting at " +
                   "index "+matcher.start()+" and ending at index "+matcher.end());
                found = true;
            }
            if(!found){
                System.out.println("No match found.");
            }
        }
    }
}
