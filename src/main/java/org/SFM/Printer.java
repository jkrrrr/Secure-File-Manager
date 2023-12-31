package org.SFM;

import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Printer {
    private static Printer instance = null;
    private final Logger logger_Printer;


    /**
     * Responsible for console printing and file management
     */
    private Printer(){
        this.logger_Printer = LoggerFactory.getLogger(Printer.class);
        logger_Printer.info("Printer logger instantiated");
    }

    /**
     * Singleton checking
     */
    public static Printer getInstance(){
        if (instance == null)
            instance = new Printer();
        return instance;
    }

    /**
     * Prints all files and directories in the directory, given as an array of objects
     * @param dirContent Content of directory
     */
    public ArrayList<String> getDirContent(FileObject[] dirContent) throws FileSystemException {
        ArrayList<String> toReturn = new ArrayList<>();
        this.logger_Printer.info("Printing dir content");
        for (FileObject child : dirContent){
            this.logger_Printer.debug("   Printing " + child.getName().getBaseName());
            if (child.getType() == FileType.FOLDER)
                toReturn.add("□ " + child.getName().getBaseName());
            if (child.getType() == FileType.FILE)
                toReturn.add("  " + child.getName().getBaseName());
        }
        return toReturn;
    }

}
