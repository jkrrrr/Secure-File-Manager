package org.SFM;

import org.apache.commons.vfs2.*;

import java.util.ArrayList;

enum Colour{
    RESET("\u001B[0m"),
    RED("\u001B[31m"),
    GREEN("\u001B[32m"),
    YELLOW("\u001B[33m"),
    BLUE("\u001B[34m"),
    CYAN("\u001B[36m");

    private final String ANSIcode;
    private Colour(String code){
        this.ANSIcode = code;
    }

    @Override
    public String toString(){
        return ANSIcode;
    }
}


public class Printer {
    private String currentDirPath;
    private FileObject[] currentDir;
    private int numOfFiles;
    private int numOfDirs;

    private final FileSystemManager fsManager = VFS.getManager();

    public Printer(String fileString) throws Exception {
        this.numOfFiles = 0;
        this.numOfDirs = 0;
        this.currentDirPath = fileString;
        this.currentDir = this.fsManager.resolveFile(this.currentDirPath).getChildren();
        updateTree();
    }

    public void printDirContent() throws FileSystemException {
        sortContentArr();
        for (FileObject child : this.currentDir){
            if (child.getType() == FileType.FOLDER)
                println("\u25A1 " + child.getName().getBaseName());
            if (child.getType() == FileType.FILE)
                println("  " + child.getName().getBaseName());
        }
    }

    public void println(String ln){
        System.out.println(ln);
    }

    public void enterDir(int index) throws Exception {
        // The content in a directory is stored in an array. The directory that the user enters is identified by its index in this array.
        this.currentDirPath = this.currentDirPath + this.currentDir[index].getName();
        updateTree();
    }

    /**
     * Sorts the current directory content array so that directories appear first, then files
     * @throws FileSystemException
     */
    private void sortContentArr() throws FileSystemException {
        ArrayList<FileObject> dirs = new ArrayList<>();
        ArrayList<FileObject> files = new ArrayList<>();
        FileObject[] toReplace = new FileObject[this.currentDir.length];

        // Separate content into directories and files
        for (FileObject child : this.currentDir){
            if (child.getType() == FileType.FOLDER){
                dirs.add(child);
            } else {
                files.add(child);
            }
        }

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
     * Gathers the content of the directory
     * @throws FileSystemException
     */
    private void updateTree() throws Exception {
        FileObject currentDir = this.fsManager.resolveFile(this.currentDirPath);
        this.currentDir = currentDir.getChildren();

        this.numOfFiles = 0;
        this.numOfDirs = 0;

        for (FileObject child : this.currentDir){
            if (child.getType() == FileType.FILE){
                this.numOfFiles++;
            } else if (child.getType() == FileType.FOLDER){
                this.numOfDirs++;
            } else {
                throw new Exception("Unable to resolve file type of file " + child.getName().getBaseName());
            }
        }
    }

}
