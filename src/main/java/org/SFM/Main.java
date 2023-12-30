package org.SFM;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalResizeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;
import java.util.logging.LogManager;

public class Main {
    public static void main(String[] args) throws Exception {

        try (InputStream is = Main.class.getClassLoader().getResourceAsStream("logging.properties")) {
            LogManager.getLogManager().readConfiguration(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger logger_Main = LoggerFactory.getLogger(Main.class);
        logger_Main.info("Main logger initialized");

        Properties properties = new Properties();
        FileInputStream input = new FileInputStream("config.properties");
        properties.load(input);

        String baseDir = properties.getProperty("baseDir");
        String passwordPath = properties.getProperty("passwordPath");

        Printer p = Printer.getInstance();
        DirectoryHandler dh = new DirectoryHandler(baseDir);
        CryptoHandler eh = new CryptoHandler();
        PasswordHandler ph = PasswordHandler.getInstance(passwordPath);

        logger_Main.info("Initializing GUI");
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        Terminal terminal = null;

        try {
            terminal = defaultTerminalFactory.createTerminal();
            terminal.enterPrivateMode();
            terminal.clearScreen();
            terminal.setCursorVisible(false);

            final TextGraphics textGraphics = terminal.newTextGraphics();
            textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
            textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);

            textGraphics.putString(2, 1, "SFM - Press ESC to exit", SGR.BOLD);

            terminal.addResizeListener(new TerminalResizeListener() {
                @Override
                public void onResized(Terminal terminal, TerminalSize newSize) {
                    // Be careful here though, this is likely running on a separate thread. Lanterna is threadsafe in
                    // a best-effort way so while it shouldn't blow up if you call terminal methods on multiple threads,
                    // it might have unexpected behavior if you don't do any external synchronization
                    textGraphics.drawLine(5, 3, newSize.getColumns() - 1, 3, ' ');
                    textGraphics.putString(5, 3, "Terminal Size: ", SGR.BOLD);
                    textGraphics.putString(5 + "Terminal Size: ".length(), 3, newSize.toString());
                    try {
                        terminal.flush();
                    } catch (IOException e) {
                        logger_Main.error(Arrays.toString(e.getStackTrace()));
                    }
                }
            });

//            textGraphics.putString(5, 4, "Last Keystroke: ", SGR.BOLD);
//            textGraphics.putString(5 + "Last Keystroke: ".length(), 4, "<Pending>");

            ArrayList<String> toPrint = new ArrayList<>(dh.getDirContent());
            int offset = 5;
            char pointer = '>';
            int currentHighlight = 0;
            textGraphics.putString(5, (offset), "> " + toPrint.get(0));
            for (int i = 1; i <= toPrint.size()-1; i++){
                textGraphics.putString(5, (i+offset), "  " + toPrint.get(i));
            }
            terminal.flush();

            KeyStroke keyStroke = terminal.readInput();

            while (keyStroke.getKeyType() != KeyType.Escape) {
                logger_Main.debug("Inside main loop");

                // Print current directory
                toPrint = new ArrayList<>(dh.getDirContent());

                switch(keyStroke.getCharacter()){
                    case 'j':
                        if (currentHighlight + 1 < toPrint.size())
                            currentHighlight++;
                        break;
                    case 'k':
                        if (currentHighlight - 1 >= 0)
                            currentHighlight--;
                        break;
                }

                for (int i = 0; i <= toPrint.size()-1; i++){
                    if (i == currentHighlight){
                        textGraphics.putString(5, (i+offset), pointer + " " + toPrint.get(i));
                    } else{
                        textGraphics.putString(5, (i+offset), "  " + toPrint.get(i));
                    }
                }

                terminal.flush();


                // Read key input
                keyStroke = terminal.readInput();
            }
        } catch (IOException e) {
            logger_Main.error(Arrays.toString(e.getStackTrace()));
        } finally {
            if (terminal != null) {
                try {
                    terminal.close();
                } catch (IOException e) {
                    logger_Main.error(Arrays.toString(e.getStackTrace()));
                }
            }
        }

    }
}