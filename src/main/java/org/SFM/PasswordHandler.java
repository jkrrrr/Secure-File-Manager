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


    private PasswordHandler(String path) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException {
        this.path = path;
        this.hashes = new ArrayList<>();
        this.cryptoHandler = new CryptoHandler();

        updateHashes();
    }


    public static PasswordHandler getInstance(String path) throws Exception{
        if (instance == null)
            instance = new PasswordHandler(path);
        return instance;
    }

    public boolean checkPassword(String password) throws NoSuchAlgorithmException {
        // Hash password, and check if it is in the list
        String hash = this.cryptoHandler.processPassword(password);
        return this.hashes.contains(hash);
    }

    public void insertPassword(String password) throws NoSuchAlgorithmException, IOException {
        System.out.println("Adding " + password);
        String hash = this.cryptoHandler.processPassword(password);

        FileWriter writer = new FileWriter(path, true);

        System.out.println("Writing " + hash);

        // Currently not writing properly
        writer.write(hash + "\n");
        writer.close();

        updateHashes();
    }

    public void print(){
        System.out.println(this.hashes.size());
        System.out.println(this.hashes.toString());
    }

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
