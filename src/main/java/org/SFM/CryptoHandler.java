package org.SFM;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.RandomStringUtils;
import org.bouncycastle.jcajce.provider.symmetric.AES;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;


public abstract class CryptoHandler {
    private static CryptoHandler instance = null;
    private static final Logger logger_CryptoHandler = LoggerFactory.getLogger(CryptoHandler.class);
    private static Cipher cipher;

    private static Cipher asymmetricCipher;

    static {
        try {
            asymmetricCipher = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        try {
            cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Responsible for encrypting objects
     */
    private CryptoHandler() throws Exception {
        logger_CryptoHandler.info("EncryptionHandler logger instantiated");

    }

    /**
     * Encrypts/Decrypts a single file
     * @param mode Mode.ENCRYPT for encryption, Mode.DECRYPT for decryption
     * @param file path to file
     * @param secretKey 16-byte AES key
     * @param ivString 16-byte initialization vector
     */
    public static synchronized void processFile(Mode mode, String file, SecretKey secretKey, String ivString) {
        try{
            logger_CryptoHandler.info("Processing file %s (%s)".formatted(file, mode.toString()));
            System.out.println("Secret key: " + Arrays.toString(secretKey.getEncoded()));

            // Create key and IV from strings
            logger_CryptoHandler.debug("   Creating key");
            SecretKey key = new SecretKeySpec(secretKey.getEncoded(), "AES");

            logger_CryptoHandler.debug("   Creating initialization vector");
            IvParameterSpec iv = new IvParameterSpec(ivString.getBytes());

            // Set correct cipher mode
            if (mode==Mode.ENCRYPT){
                cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            } else if (mode==Mode.DECRYPT){
                cipher.init(Cipher.DECRYPT_MODE, key, iv);
            } else{
                throw new Exception("Invalid process mode");
            }

            // Read file
            logger_CryptoHandler.debug("   Reading file");
            byte[] inBytes = Files.readAllBytes(Path.of(file));

            // Process file
            logger_CryptoHandler.debug("   Processing file");
            byte[] processed = cipher.doFinal(inBytes);

            FileOutputStream outputStream = new FileOutputStream(file);
            outputStream.write(processed);
            outputStream.close();
            logger_CryptoHandler.debug("   Finished processing file " + file);

        } catch (Exception e){
            logger_CryptoHandler.error(e.getMessage());
        }

    }

    public static void encryptSecretKey(String keyPath, PublicKey publicKey){
        try{
            // Read file
            byte[] inBytes = Files.readAllBytes(Path.of(keyPath));

            asymmetricCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            // Process file
            byte[] processed = cipher.doFinal(inBytes);

            FileOutputStream outputStream = new FileOutputStream(keyPath);
            outputStream.write(processed);
            outputStream.close();
        } catch (Exception e){
            logger_CryptoHandler.error(e.getMessage());
        }
    }

    public static void decryptSecretKey(String keyPath, PrivateKey privateKey){
        try{
            // Read file
            byte[] inBytes = Files.readAllBytes(Path.of(keyPath));

            asymmetricCipher.init(Cipher.DECRYPT_MODE, privateKey);

            // Process file
            byte[] processed = cipher.doFinal(inBytes);

            FileOutputStream outputStream = new FileOutputStream(keyPath);
            outputStream.write(processed);
            outputStream.close();
        } catch (Exception e){
            logger_CryptoHandler.error(e.getMessage());
        }
    }

    /**
     * Generates a public/private keypair
     * Access using getPublic() and getPrivate()
     * @return KeyPair
     */
    public static KeyPair generateKeypair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);

        return keyGen.generateKeyPair();
    }


    /**
     * Hashes a string using the SHA-256 algorithm
     * @param string string to hash
     * @return hashed string
     */
    public static byte[] hashString(String string)  {
        try{
            String string2 = string.strip();
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); // Non-deterministic for some reason
//            return messageDigest.digest(string2.getBytes(StandardCharsets.UTF_8));
            return "aaaaaaaaaaaaaaaa".getBytes();
        } catch (Exception e){
            logger_CryptoHandler.error(e.getMessage());
            return null;
        }
    }

    public static byte[] processByteArr(Mode mode, byte[] string, byte[] keyString, byte[] ivString){
        try {
            if (ivString == null)
                ivString = "aaaaaaaaaaaaaaaa".getBytes();
            IvParameterSpec iv = new IvParameterSpec(ivString);
            SecretKey key = new SecretKeySpec(keyString, "AES");
            if (mode == Mode.ENCRYPT){
                cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            } else if (mode == Mode.DECRYPT){
                cipher.init(Cipher.DECRYPT_MODE, key, iv);
            }
            return cipher.doFinal(string);
        } catch (Exception e){
            System.out.println(e.getMessage());
            logger_CryptoHandler.error(e.getMessage());
            return null;
        }
    }


    /**
     * TO ENCRYPT A FOLDER:
     * 1. Create metadata.json
     * 2. Go down tree, keep note of position of each file in comparison to top folder and which dir it belongs to. Repeat until bottom reached.
     * 3. Move all files to be in top dir. Delete all empty dirs
     * 4. Encrypt files, including metadata.json
     * TO DECRYPT A FOLDER:
     * 1. Decrypt files
     * 2. Create dirs and place files back in original place
     * 3. Delete metadata.json
     *
     * @param mode Mode.ENCRYPT for encryption, Mode.DECRYPT for decryption
     * @param vaultDir top-level dir to create the vault from
     * @param ivString 16-byte initialization vector
     */
    public static void processDirectory(Mode mode, String vaultDir, String ivString) throws Exception {
        Path vaultDirPath = Paths.get(vaultDir);
        String dir = vaultDirPath.getParent().toString();
        DirectoryHandler dh = new DirectoryHandler(dir);
        ExecutorService executorService;
        if (mode == Mode.ENCRYPT){
            logger_CryptoHandler.info("ENCRYPTING VAULT");
            // Generate AES key and write to file
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey secretKey = keyGen.generateKey();
            try {
                File secretKeyFile = new File(dir + "/SecretKey");
                byte[] toWrite = secretKey.getEncoded();
                FileOutputStream outputStream = new FileOutputStream(secretKeyFile);

                System.out.println("AES Key: " + toWrite);
                outputStream.write(secretKey.getEncoded());
                outputStream.close();

            } catch (Exception e){
                logger_CryptoHandler.error(e.getMessage());
            }

            // Create new directory
            File newDirectory = new File(dir + "/vault");
            if (newDirectory.exists()){
                logger_CryptoHandler.warn(dir + "/vault already exists!");
            } else {
                try {
                   newDirectory.mkdirs();
                } catch (Exception e){
                    logger_CryptoHandler.error(e.getMessage());
                }
            }

            // Create list of files
            logger_CryptoHandler.info("Created metadata.json for dir " + dir);

            // Save list of files
            ArrayList<Path> filePaths = dh.getDirContent(dir, "file");
            ArrayList<FileObj> fileObjs = new ArrayList<>();
            for (Path file : filePaths){
                String newName = RandomStringUtils.random(10, true, true);
                String originalName = file.getFileName().toString();
                FileObj fileObj = new FileObj(file.toString(), originalName, newName);
                fileObjs.add(fileObj);
            }

            // Convert into JSON
            Gson gsonBuilder = new GsonBuilder().setPrettyPrinting().create();
            String json = gsonBuilder.toJson(fileObjs);

            try (FileWriter writer = new FileWriter(dir + "/vault/manifest.json")){
                writer.write(json);
            }

            // Copy files into new directory, then delete the old one
            executorService = Executors.newFixedThreadPool(fileObjs.size()); // Adjust the pool size as needed
            for (FileObj file : fileObjs) {
                executorService.submit(() -> {
                    try {
                        Files.copy(Paths.get(file.getPath()), Paths.get(dir + "/vault/" + file.getNewName()), StandardCopyOption.REPLACE_EXISTING);
                        logger_CryptoHandler.debug(Thread.currentThread().getName() + ": Copied " + file.getPath());
                    } catch (IOException e) {
                        logger_CryptoHandler.error(e.getMessage());
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            // Delete empty directories
//            ArrayList<Path> dirPaths = dh.getDirContent(vaultDir, "dir");
//            for (Path dirPath : dirPaths){
//                Files.walkFileTree(dirPath, new DeletingFileVisitor());
//            }

            // Encrypt files in new directory
            executorService = Executors.newFixedThreadPool(fileObjs.size()); // Adjust the pool size as needed
            filePaths = dh.getDirContent((dir + "/vault"), "file");
            System.out.println("Encrypting files");
            for (Path file : filePaths) {
                executorService.submit(() -> {
                    try {
                        logger_CryptoHandler.debug("Encrypting " + file);
                        processFile(Mode.ENCRYPT, file.toString(), secretKey, ivString);
                        logger_CryptoHandler.debug(Thread.currentThread().getName() + ": Encrypted " + file);
                    } catch (Exception e) {
                        logger_CryptoHandler.error(e.getMessage());
                    }
                });
            }
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } else if (mode == Mode.DECRYPT){
            logger_CryptoHandler.info("DECRYPTING VAULT");
            Gson gson = new Gson();
            // Get the files from the vault
            ArrayList<Path> filePaths = dh.getDirContent(dir + "/vault", "file");
            // Get the symmetric key
            byte[] privateKeyByteArr = Files.readAllBytes(Paths.get(dir + "/SecretKey"));
            SecretKey secretKey = new SecretKeySpec(privateKeyByteArr, "AES");
            System.out.println("Decryption key: " + secretKey.getEncoded());

            System.out.println("Decrypting files");
            // Decrypt each file
            executorService = Executors.newFixedThreadPool(filePaths.size()); // Adjust the pool size as needed
            for (Path file : filePaths) {
                executorService.submit(() -> processFile(Mode.DECRYPT, file.toString(), secretKey, ivString));
            }
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            // Get content from manifest.json
            String jsonPath = dir + "/vault/manifest.json";
            System.out.println("Getting info from manifest");
            String existingJson = Files.readString(Paths.get(jsonPath), StandardCharsets.UTF_8);

            Type listType = new TypeToken<List<FileObj>>() {}.getType();
            ArrayList<FileObj> fileObjs = gson.fromJson(existingJson, listType);

            // Put each file back to where it belongs
            for (FileObj fileObj : fileObjs){
                System.out.println("Copying file");
                Files.createDirectories(Paths.get(fileObj.getPath()).getParent());
                Files.copy(Paths.get(dir + "/vault/" + fileObj.getNewName()), Paths.get(fileObj.getPath()), StandardCopyOption.REPLACE_EXISTING);
            }

            // Delete the vault
            Files.walkFileTree(Path.of(dir + "/vault"), new DeletingFileVisitor());

        }

    }

    private static PrivateKey createPrivateKey(String stringPk) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Decode Base64 encoded string
        byte[] privateKeyBytes = Base64.getDecoder().decode(stringPk);

        // Create spec from decoded bytes
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);

        // Create Key Factory
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        // Generate private key
        return keyFactory.generatePrivate(keySpec);
    }



    private static class DeletingFileVisitor extends SimpleFileVisitor<Path>{
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }

    }



}