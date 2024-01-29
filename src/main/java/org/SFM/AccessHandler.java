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
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;


public class AccessHandler {
    private final Logger logger_AccessHandler;
    private PasswordHandler ph;
    private CryptoHandler ch;
    private byte[] privateKey;

    private final String salt = "BTtsI0zG7wFlQdT0";
    private final String jsonPath_publicKey = "publicKeys.json";
    private final String jsonPath_authentication = "authentication.json";

    private String test;

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

        // Retrieve private key from JSON
        // Create a HashMap of each identifier and associated private key
        Map<String, String> recordMap = new HashMap<>();
        try (Reader reader = new FileReader(this.jsonPath_authentication)){
            this.logger_AccessHandler.debug("Retrieving authentication.json");
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            for (JsonElement element : jsonArray){
                JsonObject jsonObject = element.getAsJsonObject();

                String id = jsonObject.get("authenticationString").getAsString();
                String privateKey = jsonObject.get("privateKey").getAsString();

                recordMap.put(id, privateKey);
            }
        } catch (IOException e){
            this.logger_AccessHandler.warn(e.getMessage());
        }

        // Check for the correct identifier
        this.logger_AccessHandler.debug("Checking for correct identifier");
        for (String id : recordMap.keySet()){
            if (this.ch.verifyPassword(user+password, id)){
                this.privateKey = this.ch.processByteArr(Mode.DECRYPT, Base64.getDecoder().decode(recordMap.get(id)), this.ch.hashString_SHA(password), null);
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
            // Derive asymmetric key
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(512);
            KeyPair keyPair = kpg.generateKeyPair();

            // Write private key into JSON file
            Gson gson = new Gson();
            try (Reader reader = new FileReader(jsonPath_authentication)) {
                // Hash username+password
                String authenticationString = this.ch.hashString(user+password);
                // Get private key
                byte[] privateKey = keyPair.getPrivate().getEncoded();
                // Encrypt private key
                byte[] encryptedPrivateKey = this.ch.processByteArr(Mode.ENCRYPT, privateKey, this.ch.hashString_SHA(password), null);

                // JSON processing
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

                JsonObject newUser = new JsonObject();
                newUser.addProperty("authenticationString", authenticationString);
                newUser.addProperty("privateKey", Base64.getEncoder().encodeToString(encryptedPrivateKey));

                jsonArray.add(newUser);
                try (FileWriter writer = new FileWriter(jsonPath_authentication)) {
                    gson.toJson(jsonArray, writer);
                }
            } catch (Exception e) {
                this.logger_AccessHandler.error(e.getMessage());
                return false;
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
