package org.SFM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class PasswordHandler {
    private static PasswordHandler instance = null;
    private final String path;
    private ArrayList<String> hashes;
    private final CryptoHandler cryptoHandler;
    private final Logger logger_PasswordHandler;


    /**
     * Password management
     * @param path path to file containing hashed passwords
     */
    private PasswordHandler(String path) throws Exception {
        this.logger_PasswordHandler = LoggerFactory.getLogger(PasswordHandler.class);
        this.logger_PasswordHandler.info("PasswordHandler logger instantiated (" + path + ")");

        try{
            this.path = path;
            this.hashes = new ArrayList<>();
            this.cryptoHandler = new CryptoHandler();
        } catch (Exception e){
            this.logger_PasswordHandler.error(e.getMessage());
            throw new Exception();
        }

        updateHashes();
    }

    /**
     * Singleton checking
     * @param path path to file contained hashed passwords
     * @return instance of PasswordHandler
     */
    public static PasswordHandler getInstance(String path) throws Exception{
        if (instance == null)
            instance = new PasswordHandler(path);
        return instance;
    }

    /**
     * Checks to see if the given password is contained within the password file
     * @param password password to check
     * @return true if in password file, otherwise false
     */
    public boolean checkPassword(String password){
        String hash = this.cryptoHandler.processPassword(password);

        this.logger_PasswordHandler.info("Searching for " + hash);

        for (String s : this.hashes){
            if (this.cryptoHandler.verifyPassword(password, s))
                return true;
        }

        return false;
    }

    /**
     * Appends a password to the file, hashed
     * @param password password to hash and store
     */
    public void insertPassword(String password) throws IOException {
        try{
            String hash = this.cryptoHandler.processPassword(password);

            this.logger_PasswordHandler.info("Inserting " + hash + " to passwords");

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
                String line = reader.nextLine();
                this.hashes.add(line);
            }
            reader.close();
        } catch (Exception e){
            this.logger_PasswordHandler.error(e.getMessage());
        }

    }

}
