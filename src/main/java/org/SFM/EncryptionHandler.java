package org.SFM;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

enum Mode{
    ENCRYPT,
    DECRYPT
}

public class EncryptionHandler {
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    public EncryptionHandler() throws NoSuchAlgorithmException, NoSuchPaddingException {
    }

    /**
     * Encrypts/Decrypts a single file
     * @param mode Mode.ENCRYPT for encryption, Mode.DECRYPT for decryption
     * @param file path to file
     * @param keyString 16-byte key
     * @param ivString 16-byte initialization vector
     * @throws Exception
     */
    public void processFile(Mode mode, String file, String keyString, String ivString) throws Exception {

        // Create key and IV from strings
        SecretKey key = new SecretKeySpec(keyString.getBytes(), "AES");
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
        byte[] inBytes = Files.readAllBytes(Path.of(file));
        // Process file
        byte[] processed = cipher.doFinal(inBytes);

        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(processed);
        outputStream.close();
    }

}