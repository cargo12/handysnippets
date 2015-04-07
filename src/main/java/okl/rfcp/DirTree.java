package okl.rfcp;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
* Recursive file listing under a specified directory.
*  
* @author javapractices.com
* @author Alex Wong
* @author modified by okl
*/
public final class DirTree {

  /**
  * Demonstrate use.
  * 
  * @param args - <tt>args[0]</tt> is the full name of an existing 
  * directory that can be read.
  */
  public static void main(String... args) throws FileNotFoundException {
    File startingDirectory= new File(args[0]);
    List<File> files = DirTree.getFileListing(startingDirectory);

    //print out all file names, in the the order of File.compareTo()
    for(File file : files ){
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(file.lastModified());
        Date d = cal.getTime();
                System.out.printf("%10d %tH:%tM:%tS %Td.%Tm.%TY %s\n",
                        file.length(), 
                        d,d,d,d,d,d, 
                        file.toString());
    }
  }
  
  /**
  * Recursively walk a directory tree and return a List of all
  * Files found; the List is sorted using File.compareTo().
  *
  * @param startingDir is a valid directory, which can be read.
  */
    public static List<File> getFileListing(File startingDir) {
        List<File> result =new ArrayList<File>();
        try {
            result = getFileListingNoSort(startingDir);
        } catch (FileNotFoundException fnfe) {
            System.err.println("Starting directory could not be found: " + startingDir);
        }
        Collections.sort(result);
        return result;
    }

  // PRIVATE //
    private static List<File> getFileListingNoSort(File startingDir)
            throws FileNotFoundException {
        //System.out.println("===" + startingDir);
        List<File> result = new ArrayList<File>();
        validateDirectory(startingDir);
        File[] filesAndDirs = startingDir.listFiles();
        if (filesAndDirs == null) {
            System.err.println(" !!!! Could not read: " + startingDir);
            return result;
        }
        List<File> filesDirs = Arrays.asList(filesAndDirs);
        for (File file : filesDirs) {
            if (file.isFile())
                result.add(file); // only add leaf files
//            if (!file.isFile()) {
              if (file.isDirectory()) {
                // must be a directory
                // recursive call!
                List<File> deeperList = getFileListingNoSort(file);
                result.addAll(deeperList);
            }
        }
        return result;
    }

    /**
     * Directory is valid if it exists, does not represent a file, and can be
     * read.
     */
    private static boolean validateDirectory(File directory)
            throws FileNotFoundException {
        boolean ok = false;
        if (directory == null) {
            //throw new IllegalArgumentException("Directory should not be null.");
            System.err.println("Directory should not be null: " + directory);
            ok = false;
        }
        if (!directory.exists()) {
            // throw new FileNotFoundException("Directory does not exist: " +
            // aDirectory);
            System.err.println("Directory does not exist: " + directory);
            ok = false;
        }
        if (!directory.isDirectory()) {
            /*
            throw new IllegalArgumentException("Is not a directory: "
                    + aDirectory);*/
            System.err.println("Is not a directory: " + directory);
            ok = false;
        }
        if (!directory.canRead()) {
            /*
            throw new IllegalArgumentException("Directory cannot be read: "
                    + aDirectory);*/
            System.err.println("Directory cannot be read: " + directory);
            ok = false;
        }
        
        return ok;
    }
} 
