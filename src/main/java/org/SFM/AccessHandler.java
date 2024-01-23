package org.SFM;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class AccessHandler {
    private final Logger logger_AccessHandler;
    private PasswordHandler ph;
    private CryptoHandler ch;
    private String privateKey;

    private final String salt = "BTtsI0zG7wFlQdT0";
    private final String jsonPath_publicKey = "publicKeys.json";
    private final String jsonPath_privateKey = "privateKeys.json";

    public AccessHandler() throws Exception {
        this.logger_AccessHandler = LoggerFactory.getLogger(AccessHandler.class);
        this.ph = PasswordHandler.getInstance();
        this.ch = CryptoHandler.getInstance();
    }

    // FUNCTIONS

    /**
     * Authenicates a user into the system, then sets their private key
     * @param user username
     * @param password password
     * @return true if the username and password are correct, otherwise false
     * @throws Exception could not find a private key for the user
     */
    public boolean authenticate(String user, String password) throws Exception {
        this.logger_AccessHandler.info("Authenticating user " + user);
        // Check correct details
        if (!ph.checkLogin(user, password))
            return false;

        // TODO unencrypt privateKeys.json
        // Retrieve private key from JSON
        // Create a HashMap of each identifier and associated private key
        Map<String, String> recordMap = new HashMap<>();
        try (Reader reader = new FileReader(this.jsonPath_privateKey)){
            this.logger_AccessHandler.debug("Retrieving privateKeys.json");
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            for (JsonElement element : jsonArray){
                JsonObject jsonObject = element.getAsJsonObject();

                String id = jsonObject.get("identifier").getAsString();
                String privateKey = jsonObject.get("privateKey").getAsString();

                recordMap.put(id, privateKey);
            }
        } catch (IOException e){
            this.logger_AccessHandler.warn(e.getMessage());
        }

        // TODO encrypt privateKeys.json

        // Check for the correct identifier
        this.logger_AccessHandler.debug("Checking for correct identifier");
        for (String id : recordMap.keySet()){
            if (this.ch.verifyPassword(password, id)){
                this.privateKey = recordMap.get(id);
                break;
            }
        }

        // Check a private key has been found
        if (this.privateKey == null){
            this.logger_AccessHandler.error("Could not find private key for user " + user);
            throw(new Exception("Could not find private key for user " + user));
        }

        this.logger_AccessHandler.info("Authentication for {} passed", user);
        return true;

    }

    /**
     * Creates a new user, along with their keypair
     * @param user username
     * @param password password
     */
    public boolean createUser(String user, String password) {
        try {
            ph.insertLogin(user, password);

            // Derive asymmetric key
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(512);
            KeyPair keyPair = kpg.generateKeyPair();

            // Hash password, and use as identifier
            String identifier = ch.processPassword(password);

            // Write private key into JSON file
            this.logger_AccessHandler.debug("Adding private key to JSON file");
            Gson gson = new Gson();
            try (Reader reader = new FileReader(jsonPath_privateKey)) {
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

                JsonObject newUser = new JsonObject();
                newUser.addProperty("identifier", identifier);
                newUser.addProperty("privateKey", Arrays.toString(keyPair.getPrivate().getEncoded()));

                jsonArray.add(newUser);
                try (FileWriter writer = new FileWriter(jsonPath_privateKey)) {
                    gson.toJson(jsonArray, writer);
                }
            } catch (Exception e) {
                this.logger_AccessHandler.error(e.getMessage());
            }

            // Write public key into JSON file
            this.logger_AccessHandler.debug("Adding public key to JSON file");
            try (Reader reader = new FileReader(jsonPath_publicKey)) {
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

                JsonObject newUser = new JsonObject();
                newUser.addProperty("user", user);
                newUser.addProperty("publicKey", Arrays.toString(keyPair.getPublic().getEncoded()));

                jsonArray.add(newUser);
                try (FileWriter writer = new FileWriter(jsonPath_publicKey)) {
                    gson.toJson(jsonArray, writer);
                }
            } catch (Exception e) {
                this.logger_AccessHandler.error(e.getMessage());
            }

            this.logger_AccessHandler.info("User {} successfully created", user);
            return true;

        } catch (Exception e) {
            this.logger_AccessHandler.error(e.getMessage());
            return false;
        }
    }

    // Encrypt symmetric key with a user's asymmetric key


}
