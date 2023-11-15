package org.SFM;

import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class Printer {
    private String currentDirPath;
    private FileObject[] currentDir;
    private int numOfFiles;
    private int numOfDirs;
    private final Logger logger_Printer;

    private final FileSystemManager fsManager = VFS.getManager();

    /**
     * Responsible for console printing and file management
     * @param fileString
     */
    public Printer(String fileString) throws Exception {
        this.logger_Printer = LoggerFactory.getLogger(Main.class);
        logger_Printer.info("Printer logger instantiated");
        this.numOfFiles = 0;
        this.numOfDirs = 0;
        this.currentDirPath = fileString;
        this.currentDir = this.fsManager.resolveFile(this.currentDirPath).getChildren();
        updateTree();
    }

    /**
     * Prints all files and directories in the currently selected directory
     */
    public void printDirContent() throws FileSystemException {
        this.logger_Printer.info("Printing content of dir " + this.currentDirPath);
        sortContentArr();
        for (FileObject child : this.currentDir){
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

    public void enterDir(int index) throws Exception {
        // The content in a directory is stored in an array. The directory that the user enters is identified by its index in this array.
        this.currentDirPath = this.currentDirPath + this.currentDir[index].getName();
        updateTree();
    }

    /**
     * Sorts the current directory content array so that directories appear first, then files
     */
    private void sortContentArr() throws FileSystemException {
        this.logger_Printer.info("Sorting content object in " + this.currentDirPath);
        ArrayList<FileObject> dirs = new ArrayList<>();
        ArrayList<FileObject> files = new ArrayList<>();
        FileObject[] toReplace = new FileObject[this.currentDir.length];

        // Separate content into directories and files
        for (FileObject child : this.currentDir){
            this.logger_Printer.debug("   Sorting object " + child.getName().getBaseName());
            if (child.getType() == FileType.FOLDER){
                dirs.add(child);
            } else {
                files.add(child);
            }
        }

        this.logger_Printer.debug("Creating directory and file arrays");
        // Insert directories
        for (int i = 0; i < dirs.size(); i++){
            toReplace[i] = dirs.get(i);
        }

        // Insert files
        for (int i = 0; i < files.size(); i++){
            toReplace[i + dirs.size()] = files.get(i);
        }

        this.currentDir = toReplace;
    }

    /**
     * Gathers directory information
     */
    private void updateTree() throws Exception {
        this.logger_Printer.info("Updating content in " + this.currentDirPath);

        FileObject currentDir = this.fsManager.resolveFile(this.currentDirPath);
        this.currentDir = currentDir.getChildren();

        this.numOfFiles = 0;
        this.numOfDirs = 0;

        for (FileObject child : this.currentDir){
            this.logger_Printer.debug("   Current object: " + child.getName().getBaseName());
            if (child.getType() == FileType.FILE){
                this.numOfFiles++;
            } else if (child.getType() == FileType.FOLDER){
                this.numOfDirs++;
            } else {
                this.logger_Printer.error("Unable to resolve file of type " + child.getName().getBaseName());
                throw new Exception("Unable to resolve file type of file " + child.getName().getBaseName());
            }
        }
    }

}
