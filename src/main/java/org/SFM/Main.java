package org.SFM;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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

        // Load classes
        DirectoryHandler dh = new DirectoryHandler(baseDir);
        Printer p = Printer.getInstance(dh);
        CryptoHandler eh = new CryptoHandler();
        PasswordHandler ph = PasswordHandler.getInstance(passwordPath);


    }
}