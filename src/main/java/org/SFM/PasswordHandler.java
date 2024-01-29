package org.SFM;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class PasswordHandler {
    private static PasswordHandler instance = null;
    private String path;
    private HashMap<String, String> recordMap;
    private final CryptoHandler cryptoHandler;
    private final Logger logger_PasswordHandler;


    /**
     * Password management
     */
    private PasswordHandler() throws Exception {
        this.logger_PasswordHandler = LoggerFactory.getLogger(PasswordHandler.class);
        this.logger_PasswordHandler.info("PasswordHandler logger instantiated");

        try{
            this.recordMap = new HashMap<>();
            this.cryptoHandler = CryptoHandler.getInstance();
        } catch (Exception e){
            this.logger_PasswordHandler.error(e.getMessage());
            throw new Exception();
        }
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
        this.logger_PasswordHandler.info("Set path to " + path);
        updateHashes();
    }

    /**
     * Checks to see if the given password is contained within the password file
     * @param password password to check
     * @return true if in password file, otherwise false
     */
    public boolean checkLogin(String username, String password){
        this.updateHashes();
        for (String key : this.recordMap.keySet()){
            if (this.cryptoHandler.verifyPassword(username+password, key))
                return true;
        }
        return false;
    }

    /**
     * Appends a password to the file, hashed
     * @param password password to hash and store
     */
    public void insertLogin(String username, String password, String publicKey, String privateKey) throws IOException {
        try{
            String toHash = username + "/,/" + password;
            String hash = this.cryptoHandler.hashString(toHash);

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
        try (Reader reader = new FileReader(this.path)){
            this.logger_PasswordHandler.debug("Retrieving authentication.json");
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            for (JsonElement element : jsonArray){
                JsonObject jsonObject = element.getAsJsonObject();

                String id = jsonObject.get("authenticationString").getAsString();
                String privateKey = jsonObject.get("privateKey").getAsString();

                recordMap.put(id, privateKey);
            }
            this.logger_PasswordHandler.debug("Updated hashes");
        } catch (IOException e){
            this.logger_PasswordHandler.error(e.getMessage());
        }
    }

}
