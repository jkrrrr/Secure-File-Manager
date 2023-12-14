package org.SFM;

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


    /**
     * Password management
     * @param path path to file containing hashed passwords
     * @throws IOException
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     */
    private PasswordHandler(String path) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
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
        // Hash password, and check if it is in the list
        String hash = this.cryptoHandler.processPassword(password);
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

        FileWriter writer = new FileWriter(path, true);

        System.out.println("Writing " + hash);

        // Currently not writing properly
        writer.write(hash + "\n");
        writer.close();

        updateHashes();
    }

    /**
     * Refreshes the class' list of passwords based on the ones in the file
     * @throws IOException
     */
    private void updateHashes() throws IOException {
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
