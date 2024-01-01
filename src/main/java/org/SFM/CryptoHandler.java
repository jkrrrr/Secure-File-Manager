package org.SFM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;


public class CryptoHandler {
    private final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    private final Argon2PasswordEncoder arg2;
    private final Logger logger_CryptoHandler;

    /**
     * Responsible for encrypting objects
     */
    public CryptoHandler() throws Exception {
        this.logger_CryptoHandler = LoggerFactory.getLogger(CryptoHandler.class);
        this.logger_CryptoHandler.info("EncryptionHandler logger instantiated");

        try{
            this.arg2 = new Argon2PasswordEncoder(16, 32, 1, 60000, 10);
        } catch (Exception e){
            this.logger_CryptoHandler.error(e.getMessage());
            throw new Exception();
        }
    }

    /**
     * Encrypts/Decrypts a single file
     * @param mode Mode.ENCRYPT for encryption, Mode.DECRYPT for decryption
     * @param file path to file
     * @param keyString 16-byte key
     * @param ivString 16-byte initialization vector
     */
    @SuppressWarnings("GrazieInspection")
    public void processFile(Mode mode, String file, String keyString, String ivString) {
        try{
            this.logger_CryptoHandler.info("Processing file %s (%s)".formatted(file, mode.toString()));

            // Create key and IV from strings
            this.logger_CryptoHandler.debug("   Creating key");
            SecretKey key = new SecretKeySpec(keyString.getBytes(), "AES");

            this.logger_CryptoHandler.debug("   Creating initialization vector");
            IvParameterSpec iv = new IvParameterSpec(ivString.getBytes());

            // Set correct cipher mode
            if (mode==Mode.ENCRYPT){
                this.cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            } else if (mode==Mode.DECRYPT){
                this.cipher.init(Cipher.DECRYPT_MODE, key, iv);
            } else{
                throw new Exception("Invalid process mode");
            }

            // Read file
            this.logger_CryptoHandler.debug("   Reading file");
            byte[] inBytes = Files.readAllBytes(Path.of(file));

            // Process file
            this.logger_CryptoHandler.debug("   Processing file");
            byte[] processed = cipher.doFinal(inBytes);

            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(processed);
            outputStream.close();
            this.logger_CryptoHandler.debug("   Finished processing file " + file);
        } catch (Exception e){
            this.logger_CryptoHandler.error(e.getMessage());
        }

    }

    /**
     * Generates a public/private keypair
     * Access using getPublic() and getPrivate()
     * @return KeyPair
     */
    public KeyPair generateKeypair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);

        return keyGen.generateKeyPair();
    }

    /**
     * Hashes a password
     * @param password to hash
     * @return the resulting hash in String form
     */
    public String processPassword(String password){
        this.logger_CryptoHandler.debug("Processing password");

        return this.arg2.encode(password);
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


}