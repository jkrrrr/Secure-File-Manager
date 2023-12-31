package org.SFM;

import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class DirectoryHandler {
    private String currentDirPath;
    private FileObject[] currentDir;
    private int numOfFiles;
    private int numOfDirs;
    private final FileSystemManager fsManager = VFS.getManager();
    private final Printer printer;
    private final Logger logger_DirectoryHandler;

    /**
     * Handles directory navigation
     * @param fileString
     * @throws Exception
     */
    public DirectoryHandler(String fileString) throws Exception {
        this.logger_DirectoryHandler = LoggerFactory.getLogger(DirectoryHandler.class);
        logger_DirectoryHandler.info("DirectoryHandler logger instantiated");

        this.printer = Printer.getInstance();

        this.numOfFiles = 0;
        this.numOfDirs = 0;
        this.currentDirPath = fileString;
        this.currentDir = this.fsManager.resolveFile(this.currentDirPath).getChildren();

        updateTree();
    }

    /**
     * Updates the handler to the selected directory
     * @param index
     * @throws Exception
     * @return true when successfully indexed, false otherwise
     */
    public boolean enterDir(int index) throws Exception {
        this.logger_DirectoryHandler.debug("Entering dir index " + index);
        // The content in a directory is stored in an array. The directory that the user enters is identified by its index in this array.

        try{
            if (index == -1) {
                this.logger_DirectoryHandler.debug("   Current path " + this.currentDirPath);
                StringBuilder sb = new StringBuilder(this.currentDirPath);
                int lastIndex = sb.lastIndexOf("/");
                this.logger_DirectoryHandler.debug("   Last / index: " + lastIndex);
                this.currentDirPath = sb.delete(lastIndex, sb.length()).toString();
                this.logger_DirectoryHandler.debug("   Changed to path" + sb);
                updateTree();
                return true;
            }

            if (!this.currentDir[index].isFolder())
                return false;

            this.currentDirPath = String.valueOf(this.currentDir[index].getName());

            updateTree();

            return true;
        } catch (Exception e){
            this.logger_DirectoryHandler.error(e.getMessage());
            this.logger_DirectoryHandler.error(Arrays.toString(e.getStackTrace()));

            return false;
        }
    }

    /**
     * Prints the content of the current directory
     * @throws FileSystemException
     */
    public ArrayList<String> getDirContent() throws FileSystemException {
        return this.printer.getDirContent(this.currentDir);
    }

    /**
     * Sorts the current directory content array so that directories appear first, then files
     */
    private void sortContentArr() throws FileSystemException {
        this.logger_DirectoryHandler.info("Sorting content object in " + this.currentDirPath);
        ArrayList<FileObject> dirs = new ArrayList<>();
        ArrayList<FileObject> files = new ArrayList<>();
        FileObject[] toReplace = new FileObject[this.currentDir.length];

        // Separate content into directories and files
        for (FileObject child : this.currentDir){
            this.logger_DirectoryHandler.debug("   Sorting object " + child.getName().getBaseName());
            if (child.getType() == FileType.FOLDER){
                this.numOfDirs++;
                dirs.add(child);
            } else {
                this.numOfFiles++;
                files.add(child);
            }
        }

        this.logger_DirectoryHandler.debug("Creating directory and file arrays");
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
        this.logger_DirectoryHandler.info("Updating content in " + this.currentDirPath);

        FileObject currentDir = this.fsManager.resolveFile(this.currentDirPath);
        this.currentDir = currentDir.getChildren();

        this.numOfFiles = 0;
        this.numOfDirs = 0;

        for (FileObject child : this.currentDir){
            this.logger_DirectoryHandler.debug("   Current object: " + child.getName().getBaseName());
            if (child.getType() == FileType.FILE){
                this.numOfFiles++;
            } else if (child.getType() == FileType.FOLDER){
                this.numOfDirs++;
            } else {
                this.logger_DirectoryHandler.error("Unable to resolve file of type " + child.getName().getBaseName());
                throw new Exception("Unable to resolve file type of file " + child.getName().getBaseName());
            }
        }

        sortContentArr();
    }

}
