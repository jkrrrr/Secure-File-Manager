package org.SFM;

import org.apache.commons.codec.digest.Crypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.LogManager;

public class Main {
    public static void main(String[] args) throws Exception {


        // Create logger
        Logger logger_Main = LoggerFactory.getLogger(Main.class);
        logger_Main.info("Main logger initialized");

        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
           logger_Main.error(Arrays.toString(e.getStackTrace()));
        }

        // Load properties
        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("config.properties");
        properties.load(input);

        String baseDir = properties.getProperty("baseDir");
        String passwordPath = properties.getProperty("passwordPath");
        String keyPath = properties.getProperty("keyPath");
        String sigPath = properties.getProperty("sigPath");

        // Load classes
        DirectoryHandler dh = new DirectoryHandler(System.getProperty("user.dir"));
        AccessHandler ah = AccessHandler.getInstance();
        ah.setPath(System.getProperty("user.dir") + "/authentication.json");

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        // Reset database
        ah.reset();

        Printer p = Printer.getInstance(dh);

    }
}
