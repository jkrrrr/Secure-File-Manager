package org.SFM;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class CryptoHandler {
    private static CryptoHandler instance = null;
    private final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    private final Argon2PasswordEncoder arg2;
    private final Logger logger_CryptoHandler;
    private ExecutorService executorService = null;

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

    public static CryptoHandler getInstance() throws Exception {
        if (instance == null)
            instance = new CryptoHandler();
        return instance;
    }


    /**
     * Encrypts/Decrypts a single file
     * @param mode Mode.ENCRYPT for encryption, Mode.DECRYPT for decryption
     * @param file path to file
     * @param keyString 16-byte key
     * @param ivString 16-byte initialization vector
     */
    public synchronized void processFile(Mode mode, String file, String keyString, String ivString) {
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

    /**
     * TO ENCRYPT A FOLDER:
     * 1. Create metadata.json
     * 2. Go down tree, keep note of position of each file in comparison to top folder and which dir it belongs to. Repeat until bottom reached.
     * 3. Move all files to be in top dir. Delete all empty dirs
     * 4. Encrypt files, including metadata.json
     *
     * TO DECRYPT A FOLDER:
     * 1. Decrypt files
     * 2. Create dirs and place files back in original place
     * 3. Delete metadata.json
     *
     * @param mode Mode.ENCRYPT for encryption, Mode.DECRYPT for decryption
     * @param vaultDir top-level dir to create the vault from
     * @param keyString 16-byte key
     * @param ivString 16-byte initialization vector
     */
    public void processDirectory(Mode mode, String vaultDir, String keyString, String ivString) throws Exception {
        Path vaultDirPath = Paths.get(vaultDir);
        String dir = vaultDirPath.getParent().toString();
        DirectoryHandler dh = new DirectoryHandler(dir);
        if (mode == Mode.ENCRYPT){
            this.logger_CryptoHandler.info("ENCRYPTING VAULT");

            // Create new directory
            File newDirectory = new File(dir + "/vault");
            if (newDirectory.exists()){
                this.logger_CryptoHandler.warn(dir + "/vault already exists!");
            } else {
                try {
                   newDirectory.mkdirs();
                } catch (Exception e){
                    this.logger_CryptoHandler.error(e.getMessage());
                }
            }

            // Create list of files
            this.logger_CryptoHandler.info("Created metadata.json for dir " + dir);

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
            this.executorService = Executors.newFixedThreadPool(fileObjs.size()); // Adjust the pool size as needed
            for (FileObj file : fileObjs) {
                executorService.submit(() -> {
                    try {
                        Files.copy(Paths.get(file.getPath()), Paths.get(dir + "/vault/" + file.getNewName()), StandardCopyOption.REPLACE_EXISTING);
                        this.logger_CryptoHandler.debug(Thread.currentThread().getName() + ": Copied " + file.getPath());
                    } catch (IOException e) {
                        this.logger_CryptoHandler.error(e.getMessage());
                    }
                });
            }
            executorService.shutdown();
            this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            // Delete empty directories
            ArrayList<Path> dirPaths = dh.getDirContent(vaultDir, "dir");
            for (Path dirPath : dirPaths){
                Files.walkFileTree(dirPath, new DeletingFileVisitor());
            }

            // Encrypt files in new directory
            this.executorService = Executors.newFixedThreadPool(fileObjs.size()); // Adjust the pool size as needed
            filePaths = dh.getDirContent((dir + "/vault"), "file");
            System.out.println("Encrypting files");
            for (Path file : filePaths) {
                executorService.submit(() -> {
                    try {
                        logger_CryptoHandler.debug("Encrypting " + file);
                        processFile(Mode.ENCRYPT, file.toString(), keyString, ivString);
                        this.logger_CryptoHandler.debug(Thread.currentThread().getName() + ": Encrypted " + file);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
            executorService.shutdown();
            this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } else if (mode == Mode.DECRYPT){
            this.logger_CryptoHandler.info("DECRYPTING VAULT");
            Gson gson = new Gson();
            // Get the files from the vault
            ArrayList<Path> filePaths = dh.getDirContent(dir + "/vault", "file");
            System.out.println("Decrypting files");
            // Decrypt each file
            this.executorService = Executors.newFixedThreadPool(filePaths.size()); // Adjust the pool size as needed
            for (Path file : filePaths) {
                executorService.submit(() -> {
                    this.processFile(Mode.DECRYPT, file.toString(), keyString, ivString);
                });
            }
            this.executorService.shutdown();
            this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

            // Get content from manifest.json
            String jsonPath = dir + "/vault/manifest.json";
            System.out.println("Getting info from manifest");
            String existingJson = Files.readString(Paths.get(jsonPath));

            Type listType = new TypeToken<List<FileObj>>() {}.getType();
            ArrayList<FileObj> fileObjs = gson.fromJson(existingJson, listType);

            // Put each file back to where it belongs
            for (FileObj fileObj : fileObjs){
                Files.createDirectories(Paths.get(fileObj.getPath()).getParent());
                Files.copy(Paths.get(dir + "/vault/" + fileObj.getNewName()), Paths.get(fileObj.getPath()), StandardCopyOption.REPLACE_EXISTING);
            }

            // Delete the vault
            Files.walkFileTree(Path.of(dir + "/vault"), new DeletingFileVisitor());

        }

    }

    static class DeletingFileVisitor extends SimpleFileVisitor<Path>{
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