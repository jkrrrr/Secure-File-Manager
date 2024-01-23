package org.SFM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class PasswordHandler {
    private static PasswordHandler instance = null;
    private String path;
    private ArrayList<String> hashes;
    private final CryptoHandler cryptoHandler;
    private final Logger logger_PasswordHandler;


    /**
     * Password management
     */
    private PasswordHandler() throws Exception {
        this.logger_PasswordHandler = LoggerFactory.getLogger(PasswordHandler.class);
        this.logger_PasswordHandler.info("PasswordHandler logger instantiated (" + path + ")");

        try{
            this.hashes = new ArrayList<>();
            this.cryptoHandler = CryptoHandler.getInstance();
        } catch (Exception e){
            this.logger_PasswordHandler.error(e.getMessage());
            throw new Exception();
        }

        updateHashes();
    }

    /**
     * Singleton checking
     * @return instance of PasswordHandler
     */
    public static PasswordHandler getInstance() throws Exception{
        if (instance == null)
            instance = new PasswordHandler();
        return instance;
    }

    /**
     * Sets the path of the password file
     * @param path path of the password file
     */
    public void setPath(String path){
        this.path = path;
    }

    /**
     * Checks to see if the given password is contained within the password file
     * @param password password to check
     * @return true if in password file, otherwise false
     */
    public boolean checkLogin(String username, String password){
        this.updateHashes();
        return this.hashes.parallelStream()
                .anyMatch(s -> this.cryptoHandler.verifyPassword((username + "/,/" + password), s.strip()));
    }

    /**
     * Appends a password to the file, hashed
     * @param password password to hash and store
     */
    public void insertLogin(String username, String password) throws IOException {
        try{
            String toHash = username + "/,/" + password;
            String hash = this.cryptoHandler.processPassword(toHash);

            FileWriter writer = new FileWriter(path, true);

            writer.write(hash + "\n");
            writer.close();
        } catch (Exception e){
            this.logger_PasswordHandler.error(e.getMessage());
        }

        updateHashes();
    }

    /**
     * Refreshes the class' list of passwords based on the ones in the file
     */
    private void updateHashes() {
        this.logger_PasswordHandler.info("Updating hashes");

        try{
            this.hashes = new ArrayList<>();

            // Get hashes from file
            File file = new File(path);
            Scanner reader = new Scanner(file);

            while(reader.hasNextLine()){
                String line = reader.nextLine().strip();
                this.hashes.add(line);
            }
            reader.close();
        } catch (Exception e){
            this.logger_PasswordHandler.error(e.getMessage());
        }

    }

}
