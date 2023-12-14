package org.SFM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.security.NoSuchAlgorithmException;
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
     * @throws IOException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    private PasswordHandler(String path) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
        this.logger_PasswordHandler = LoggerFactory.getLogger(Main.class);
        this.logger_PasswordHandler.info("PasswordHandler logger instantiated (" + path + ")");

        this.path = path;
        this.hashes = new ArrayList<>();
        this.cryptoHandler = new CryptoHandler();

        updateHashes();
    }

    /**
     * Singleton checking
     * @param path path to file contained hashed passwords
     * @return instance of PasswordHandler
     * @throws Exception
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
     * @throws NoSuchAlgorithmException
     */
    public boolean checkPassword(String password) throws NoSuchAlgorithmException {
        String hash = this.cryptoHandler.processPassword(password);

        this.logger_PasswordHandler.info("Searching for " + hash);

        return this.hashes.contains(hash);
    }

    /**
     * Appends a password to the file, hashed
     * @param password password to hash and store
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public void insertPassword(String password) throws NoSuchAlgorithmException, IOException {
        String hash = this.cryptoHandler.processPassword(password);

        this.logger_PasswordHandler.info("Inserting " + hash + " to passwords");

        FileWriter writer = new FileWriter(path, true);

        writer.write(hash + "\n");
        writer.close();

        updateHashes();
    }

    /**
     * Refreshes the class' list of passwords based on the ones in the file
     * @throws IOException
     */
    private void updateHashes() throws IOException {
        this.logger_PasswordHandler.info("Updating hashes");

        this.hashes = new ArrayList<>();

        // Get hashes from file
        File file = new File(path);
        Scanner reader = new Scanner(file);

        while(reader.hasNextLine()){
            String line = reader.nextLine();
            this.hashes.add(line);
        }
        reader.close();
    }

}
