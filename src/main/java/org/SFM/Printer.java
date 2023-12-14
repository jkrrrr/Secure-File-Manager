package org.SFM;

import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;

public class Printer {
    private static Printer instance = null;
    private final Logger logger_Printer;

    private final FileSystemManager fsManager = VFS.getManager();

    /**
     * Responsible for console printing and file management
     */
    private Printer() throws Exception {
        this.logger_Printer = LoggerFactory.getLogger(Printer.class);
        logger_Printer.info("Printer logger instantiated");
    }

    /**
     * Singleton checking
     * @return
     * @throws Exception
     */
    public static Printer getInstance() throws Exception {
        if (instance == null)
            instance = new Printer();
        return instance;
    }

    /**
     * Prints all files and directories in the directory, given as an array of objects
     * @param dirContent Content of directory
     */
    public void printDirContent(FileObject[] dirContent) throws FileSystemException {
        this.logger_Printer.info("Printing dir content");
        for (FileObject child : dirContent){
            this.logger_Printer.debug("   Printing " + child.getName().getBaseName());
            if (child.getType() == FileType.FOLDER)
                System.out.println("\u25A1 " + child.getName().getBaseName());
            if (child.getType() == FileType.FILE)
                System.out.println("  " + child.getName().getBaseName());
        }
    }

    /**
     * Prints a string of text in a colour
     * @param ln String to print
     * @param colour Colour, using Colour enum
     */
    public void println(String ln, Colour colour){
        this.logger_Printer.info("Printing \"%s\" in colour %s".formatted(ln, colour));
        System.out.println(colour + ln);
    }

}
