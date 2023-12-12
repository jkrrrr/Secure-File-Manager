package org.SFM;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class CryptoHandler {
    private final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    private final Logger logger_CryptoHandler;

    /**
     * Responsible for encrypting objects
     */
    public CryptoHandler() throws NoSuchAlgorithmException, NoSuchPaddingException {
        this.logger_CryptoHandler = LoggerFactory.getLogger(Main.class);
        logger_CryptoHandler.info("EncryptionHandler logger instantiated");
    }

    /**
     * Encrypts/Decrypts a single file
     * @param mode Mode.ENCRYPT for encryption, Mode.DECRYPT for decryption
     * @param file path to file
     * @param keyString 16-byte key
     * @param ivString 16-byte initialization vector
     */
    public void processFile(Mode mode, String file, String keyString, String ivString) throws Exception {
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

    }

    /**
     * Hashes a password
     * @param password to hash
     * @return the resulting hash in String form
     * @throws NoSuchAlgorithmException
     */
    public String processPassword(String password) throws NoSuchAlgorithmException {
        this.logger_CryptoHandler.debug("Processing password");

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
        return(new String(hash, StandardCharsets.UTF_8));
    }


}