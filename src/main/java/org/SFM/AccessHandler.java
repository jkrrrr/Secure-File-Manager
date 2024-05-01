package org.SFM;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;


public class AccessHandler {
    private static AccessHandler instance = null;
    private final Argon2PasswordEncoder arg2;
    private final Logger logger_AccessHandler;
    private KeyPair keyPair;
    private Signature sig;

    private final String jsonPath_authentication = "authentication.json";
    private final String jsonPath_publicKeys = "publicKeys.json";
    private String path;
    private final HashMap<String, String> recordMap;
    public final HashMap<String, PublicKey> publicKeys;

    /**
     * Responsible for authenticating user and creating new accounts
     * @throws Exception singleton error
     */
    private AccessHandler() throws Exception {
        this.logger_AccessHandler = LoggerFactory.getLogger(AccessHandler.class);
        this.sig = Signature.getInstance("SHA256withRSA");

        try{
            this.arg2 = new Argon2PasswordEncoder(16, 32, 1, 60000, 10);
        } catch (Exception e){
            this.logger_AccessHandler.error(e.getMessage());
            throw new Exception();
        }
        
        try{
            this.recordMap = new HashMap<>();
        } catch (Exception e){
            this.logger_AccessHandler.error(e.getMessage());
            throw new Exception();
        }

        this.publicKeys = new HashMap<>();
        updatePublicKeys();

    }

    /**
     * Retrieves the public keys and updates them in the field
     */
    public void updatePublicKeys(){
        try (Reader reader = new FileReader(jsonPath_publicKeys)){
            this.logger_AccessHandler.debug("Retrieving public keys");
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            for (JsonElement element : jsonArray){
                JsonObject jsonObject = element.getAsJsonObject();

                String user = jsonObject.get("user").getAsString();
                String publicKeyString = jsonObject.get("publicKey").getAsString();

                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString));
                KeyFactory kf = KeyFactory.getInstance("RSA");
                PublicKey publicKey = kf.generatePublic(keySpec);

                this.publicKeys.put(user, publicKey);
            }
            this.logger_AccessHandler.debug("Updated public keys");
        } catch (Exception e){
            this.logger_AccessHandler.error(e.getMessage());
        }
    }

    public static synchronized AccessHandler getInstance() throws Exception {
        if (instance == null)
            instance = new AccessHandler();
        return instance;
    }

    /**
     * Used for debugging - deletes all authentication data
     */
    public void reset(){
        String[] filePaths = {"authentication.json", "publicKeys.json", "logs/recent.log"};
        for (String filePath : filePaths) {
            try (FileWriter writer = new FileWriter(filePath)) {
                if (filePath.endsWith(".json")) {
                    writer.write("[]"); // Write an empty array to JSON files
                } else {
                    writer.write(""); // Write an empty string to other files
                }
            } catch (Exception e){
                this.logger_AccessHandler.error(e.getMessage());
            }
        }
    }

    /**
     * Sets the path of the password file
     * @param path path of the password file
     */
    public void setPath(String path){
        this.path = path;
        this.logger_AccessHandler.info("Set path to " + path);
        updateHashes();
    }

    /**
     * Refreshes the class' list of passwords based on the ones in the file
     */
    private void updateHashes() {
        this.logger_AccessHandler.info("Updating hashes");
        try (Reader reader = new FileReader(this.path)){
            this.logger_AccessHandler.debug("Retrieving authentication.json");
            Gson gson = new Gson();
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            for (JsonElement element : jsonArray){
                JsonObject jsonObject = element.getAsJsonObject();

                String id = jsonObject.get("authenticationString").getAsString();
                String privateKey = jsonObject.get("privateKey").getAsString();

                this.recordMap.put(id, privateKey);
            }
            this.logger_AccessHandler.debug("Updated hashes");
        } catch (Exception e){
            this.logger_AccessHandler.error(e.getMessage());
        }
    }
    
    /**
     * Checks to see if the given password is contained within the password file
     * @param password password to check
     * @return true if in password file, otherwise false
     */
    public boolean checkLogin(String username, String password){
        this.updateHashes();
        try{
            for (String key : this.recordMap.keySet()){
                if (this.verifyPassword(username+password, key))
                    return true;
            }
        } catch (Exception e){
            this.logger_AccessHandler.error(e.getMessage());
        }
        return false;
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
        if (!this.checkLogin(user, password))
            return false;

        // Retrieve private key from JSON
        // Create a HashMap of each identifier and associated private key
        String username = null;
        Gson gson = new Gson();
        String id = null;
        String privateKey = null;
        boolean validLogin = false;
        try (Reader reader = new FileReader(this.jsonPath_authentication)){
            this.logger_AccessHandler.debug("Retrieving authentication.json");
            JsonArray jsonArray_authentication = gson.fromJson(reader, JsonArray.class);

            for (JsonElement element : jsonArray_authentication){
                JsonObject jsonObject_authentication = element.getAsJsonObject();

                id = jsonObject_authentication.get("authenticationString").getAsString();
                privateKey = jsonObject_authentication.get("privateKey").getAsString();

                if (this.verifyPassword(user+password, id)){
                    this.logger_AccessHandler.info("Verified password");
                    validLogin = true;
                    break;
                    }
                }
            }

        if (!validLogin)
            return false;

        try (Reader reader2 = new FileReader(this.jsonPath_publicKeys)){
            JsonArray jsonArray_publicKeys = gson.fromJson(reader2, JsonArray.class);

            for (JsonElement usernamePair : jsonArray_publicKeys){
                username = usernamePair.getAsJsonObject().get("user").getAsString();
                if (username.matches(user)){
                    System.out.println("Match found");
                    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                    String publicKeyString = usernamePair.getAsJsonObject().get("publicKey").getAsString();
                    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyString));

                    // Generate PublicKey from its bytes
                    PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

                    // Generate PrivateKey from its bytes
                    PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(CryptoHandler.processByteArr(Mode.DECRYPT, Base64.getDecoder().decode(privateKey), CryptoHandler.hashString(password), null));
                    PrivateKey privateKeyNew = keyFactory.generatePrivate(privateKeySpec);

                    // Create KeyPair
                    this.keyPair = new KeyPair(publicKey, privateKeyNew);

                    this.logger_AccessHandler.info("Password assigned");
                    break;
                }
            }
        } catch (IOException e){
            System.out.println(e.getMessage());
            this.logger_AccessHandler.warn(e.getMessage());
        }

        // Check a private key has been found
        if (this.keyPair.getPrivate() == null){
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
            KeyPair keyPair = CryptoHandler.generateKeypair();

            // Write private key into JSON file
            Gson gson = new Gson();
            try (Reader reader = new FileReader(jsonPath_authentication)) {
                // Hash username+password
                String authenticationString = this.kdf(user + password);
                // Get private key
                byte[] privateKey = keyPair.getPrivate().getEncoded();
                // Encrypt private key
                byte[] encryptedPrivateKey = CryptoHandler.processByteArr(Mode.ENCRYPT, privateKey, CryptoHandler.hashString(password), null);

                // JSON processing
                JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

                JsonObject newUser = new JsonObject();
                newUser.addProperty("authenticationString", authenticationString);
                newUser.addProperty("privateKey", Base64.getEncoder().encodeToString(encryptedPrivateKey));
//                newUser.addProperty("privateKey", Arrays.toString(encryptedPrivateKey));

                jsonArray.add(newUser);
                try (FileWriter writer = new FileWriter(jsonPath_authentication)) {
                    gson.toJson(jsonArray, writer);
                } catch (Exception e){
                    System.out.println("Writing error (private key): " + e.getMessage());
                    this.logger_AccessHandler.error("Writing error (private key): " + e.getMessage());
                }
            } catch (Exception e) {
                System.out.println("Error in JSON processing: " + Arrays.toString(e.getStackTrace()));
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
                } catch (Exception e){
                    System.out.println("Writing error (public key): " + e.getMessage());
                    this.logger_AccessHandler.error("Writing error (public key): " + e.getMessage());
                }
                this.updatePublicKeys();
                this.logger_AccessHandler.info("User {} successfully created", user);
                return true;

            } catch (Exception e) {
                System.out.println("1: " + e.getMessage());
                this.logger_AccessHandler.error(e.getMessage());
                return false;
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println("2: " + e.getMessage());
            this.logger_AccessHandler.error(e.getMessage());
            return false;
        }
    }

    /**
     * Applies the Argon2 KDF
     * @param string string to hash
     * @return the resulting hash in String form
     */
    public String kdf(String string){
        return this.arg2.encode(string);
    }

    /**
     * Checks a plaintext password matches a hashed password
     * @param raw plaintext password
     * @param hashed hashed password
     * @return true if it does, else false
     */
    public boolean verifyPassword(String raw, String hashed){
        return this.arg2.matches(raw, hashed);
    }
    
    public PrivateKey getPrivateKey() {
        return this.keyPair.getPrivate();
    }

    /**
     * Creates a digital signature from a string
     * @param filePath path of file to sign
     * @param sigPath signature output path
     */
    public void sign(String filePath, String sigPath){
        try {
            // Read filePath file
            byte[] toSign = Files.readAllBytes(Paths.get(filePath));

            // Generate signature
            this.sig.initSign(this.keyPair.getPrivate());
            this.sig.update(toSign);
            byte[] sigFinal = this.sig.sign();

            // Prepare signature file
            FileOutputStream fileOutputStream = new FileOutputStream(sigPath);
            fileOutputStream.write(sigFinal);
            fileOutputStream.close();
            this.logger_AccessHandler.info("Created signature");
        } catch (Exception e){
            logger_AccessHandler.error(e.getLocalizedMessage());
        }
    }
    
    /**
     * Verfies a digital signature
     * @param filePath path of the document the signature is verifying
     * @param sigPath path of signature to verify
     * @param user username of the sender
     * @return true if the signature is verified, false otherwise
     */
    public boolean verify(String filePath, String sigPath, String user){
        try {
            byte[] sig = Files.readAllBytes(Paths.get(sigPath));
            byte[] data = Files.readAllBytes(Paths.get(filePath));

            this.sig.initVerify(this.publicKeys.get(user));
            this.sig.update(data);
            return this.sig.verify(sig);
        } catch (Exception e){
            this.logger_AccessHandler.error(e.getMessage());
        }
        return false;
    }
}
