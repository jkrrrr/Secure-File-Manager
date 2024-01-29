package org.SFM;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


public class AccessHandler {
    private final Logger logger_AccessHandler;
    private final PasswordHandler ph;
    private final CryptoHandler ch;
    private byte[] privateKey;

    private final String jsonPath_authentication = "authentication.json";
    private final String jsonPath_publicKeys = "publicKeys.json";


    /**
     * Responsible for authenticating user and creating new accounts
     * @throws Exception singleton error
     */
    public AccessHandler() throws Exception {
        this.logger_AccessHandler = LoggerFactory.getLogger(AccessHandler.class);
        this.ph = PasswordHandler.getInstance();
        this.ch = CryptoHandler.getInstance();
    }

    /**
     * Authenticates a user into the system, then sets their private key
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
        try (Reader reader = new FileReader(this.jsonPath_authentication)){
            this.logger_AccessHandler.debug("Retrieving authentication.json");
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            for (JsonElement element : jsonArray){
                JsonObject jsonObject = element.getAsJsonObject();

                String id = jsonObject.get("authenticationString").getAsString();
                String privateKey = jsonObject.get("privateKey").getAsString();

                if (this.ch.verifyPassword(user+password, id)){
                    this.privateKey = this.ch.processByteArr(Mode.DECRYPT, Base64.getDecoder().decode(privateKey), this.ch.hashString_SHA(password), null);
                    break;
                }
            }
        } catch (IOException e){
            this.logger_AccessHandler.warn(e.getMessage());
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
            // Get keypair
            KeyPair keyPair = this.ch.generateKeypair();

            // Write private key into JSON file
            Gson gson = new Gson();
            try (Reader reader = new FileReader(jsonPath_authentication)) {
                // Hash username+password
                String authenticationString = this.ch.hashString(user + password);
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
                this.logger_AccessHandler.error("Error in JSON processing:\n" + e.getMessage());
                return false;
            }

            try (Reader reader = new FileReader(jsonPath_publicKeys)) {
                // JSON processing
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

                JsonObject newUser = new JsonObject();
                newUser.addProperty("user", user);
                newUser.addProperty("publicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

                jsonArray.add(newUser);
                try (FileWriter writer = new FileWriter(jsonPath_publicKeys)) {
                    gson.toJson(jsonArray, writer);
                }
                this.logger_AccessHandler.info("User {} successfully created", user);
                return true;

            } catch (Exception e) {
                this.logger_AccessHandler.error(e.getMessage());
                return false;
            }
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

        // Encrypt symmetric key with a user's asymmetric key


}
