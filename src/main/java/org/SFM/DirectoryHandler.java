package org.SFM;

import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class DirectoryHandler {
    // Path of current directory
    public String currentDirPath;
    // Content of current directory, stored as FileObjects
    private FileObject[] currentDir;
    private int numOfFiles;
    private int numOfDirs;

    private final FileSystemManager fsManager = VFS.getManager();
    private final Logger logger_DirectoryHandler;

    /**
     * Handles directory navigation
     * @param fileString Path of starting directory
     */
    public DirectoryHandler(String fileString) throws Exception {
        this.logger_DirectoryHandler = LoggerFactory.getLogger(DirectoryHandler.class);
        logger_DirectoryHandler.info("DirectoryHandler logger instantiated");

        try {
            this.numOfFiles = 0;
            this.numOfDirs = 0;
            this.currentDirPath = fileString;
            this.currentDir = this.fsManager.resolveFile(this.currentDirPath).getChildren();

            updateTree();
        } catch (Exception e){
            this.logger_DirectoryHandler.error(e.getMessage());
        }
    }

    /**
     * Updates the handler to the selected directory
     * @param index index of directory to enter into
     * @return true when successfully indexed, false otherwise
     */
    public boolean enterDir(int index) {
        this.logger_DirectoryHandler.debug("Entering dir index " + index);
        // The content in a directory is stored in an array. The directory that the user enters is identified by its index in this array.

        try{
            // If index is -1, go upwards in the directory tree
            if (index == -1) {
                this.logger_DirectoryHandler.debug("   Current path " + this.currentDirPath);

                // Remove the last part of the path
                StringBuilder sb = new StringBuilder(this.currentDirPath);
                int lastIndex = sb.lastIndexOf("/");
                this.logger_DirectoryHandler.debug("   Last / index: " + lastIndex);
                String newPath = sb.delete(lastIndex, sb.length()).toString();

                // If the new path is invalid, return false
                if (newPath.equals("-1"))
                    return false;

                this.currentDirPath = newPath;

                this.logger_DirectoryHandler.debug("   Changed to path" + sb);
                updateTree();
                return true;
            }

            // If index is a file, return false
            if (!this.currentDir[index].isFolder())
                return false;

            // Update the path to the new sub-folder
            this.currentDirPath = String.valueOf(this.currentDir[index].getName());

            updateTree();

            return true;
        } catch (Exception e){
            this.logger_DirectoryHandler.error(e.getMessage());
            this.logger_DirectoryHandler.error(Arrays.toString(e.getStackTrace()));

            return false;
        }
    }

    public String getCurrentDirPath(){
        if (this.currentDirPath.contains("file:///"))
            return this.currentDirPath.split("file:///")[1];
        return this.currentDirPath;
    }

    /**
     * Prints the content of the current directory
     */
    public FileObject[] getDirContent(){
        return this.currentDir;
    }

    /**
     * Sorts the current directory content array so that directories appear first, then files
     */
    private void sortContentArr() {
        try{
            this.logger_DirectoryHandler.info("Sorting content object in " + this.currentDirPath);
            ArrayList<FileObject> dirs = new ArrayList<>();
            ArrayList<FileObject> files = new ArrayList<>();
            FileObject[] toReplace = new FileObject[this.currentDir.length];

            // Separate content into directories and files
            for (FileObject child : this.currentDir){
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
        } catch (Exception e){
            this.logger_DirectoryHandler.error(e.getMessage());
        }
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

        // Count number of files and directories
        for (FileObject child : this.currentDir){
            if (child.getType() == FileType.FILE){
                this.numOfFiles++;
            } else if (child.getType() == FileType.FOLDER){
                this.numOfDirs++;
            } else {
                this.logger_DirectoryHandler.error("Unable to resolve file of type " + child.getName().getBaseName());
                throw new Exception("Unable to resolve file type of file " + child.getName().getBaseName());
            }
        }

        this.logger_DirectoryHandler.debug(this.currentDirPath + " has " + this.numOfFiles + " files and " + this.numOfDirs + " dirs.");

        sortContentArr();
    }

    public ArrayList<Path> getDirContent(String path, String type) throws FileSystemException {
        FileObject dir = this.fsManager.resolveFile(path);

        ArrayList<Path> toReturn = new ArrayList<>();

        for (FileObject child : dir){
            if (Objects.equals(type, "dir") && child.getType() == FileType.FOLDER)
                toReturn.add(child.getPath());
            if (Objects.equals(type, "file") && child.getType() == FileType.FILE)
                toReturn.add(child.getPath());
        }

        return toReturn;
    }



}
