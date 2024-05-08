package org.SFM;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

public class Printer {
    private static Printer instance = null;
    private final Logger logger_Printer;
    private final DirectoryHandler dh;
    private Terminal terminal;
    private TextGraphics textGraphics;
    private int currentHighlight;
    private ArrayList<String> dirToPrint;
    private final int offset;
    private final char pointer;
    private int rows;
    private int columns;
    private final AccessHandler ah;


    /**
     * Responsible for graphics printing
     */
    private Printer(DirectoryHandler dh) throws Exception {
        this.logger_Printer = LoggerFactory.getLogger(Printer.class);
        logger_Printer.info("Printer logger instantiated");
        
        this.dh = dh;
        this.ah = AccessHandler.getInstance();
        this.currentHighlight = 0;
        this.dirToPrint = new ArrayList<>();

        this.offset = 5;
        this.pointer = '>';

        // Terminal creation
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();

        try {
            this.logger_Printer.debug("   Creating terminal");
            // Terminal configuration
            this.terminal = defaultTerminalFactory.createTerminal();
            this.terminal.enterPrivateMode();
            this.terminal.clearScreen();
            this.terminal.setCursorVisible(false);

            this.logger_Printer.debug("   Creating graphics");
            // Instantiate graphics
            this.textGraphics = this.terminal.newTextGraphics();

            // Background
            this.textGraphics.setForegroundColor(TextColor.ANSI.WHITE);
            this.textGraphics.setBackgroundColor(TextColor.ANSI.BLACK);

            // Opening title
            this.textGraphics.putString(2, 1, "SFM - Press ESC to exit", SGR.BOLD);

            // Get terminal size
            TerminalSize terminalSize = terminal.getTerminalSize();
            this.rows = terminalSize.getRows();
            this.columns = terminalSize.getColumns();

            this.logger_Printer.debug("   Creating window resize listener");
            // Check for window resizing - boilerplate code from lanterna docs
            this.terminal.addResizeListener((terminal1, newSize) -> {
                // Be careful here though, this is likely running on a separate thread. Lanterna is threadsafe in
                // a best-effort way so while it shouldn't blow up if you call terminal methods on multiple threads,
                // it might have unexpected behavior if you don't do any external synchronization
                try {
                    this.rows = terminalSize.getRows();
                    this.columns = terminalSize.getColumns();

//                    this.textGraphics.drawLine(5, 3, newSize.getColumns() - 1, 3, ' ');
//                    this.textGraphics.putString(5, 3, "Terminal Size: ", SGR.BOLD);
//                    this.textGraphics.putString(5 + "Terminal Size: ".length(), 3, newSize.toString());

                    this.display_controls();

                    terminal1.flush();
                } catch (IOException e) {
                    this.logger_Printer.error(e.getMessage());
                }

            });

            this.screen_opening();

            this.screen_directory();

        } catch (IOException e) {
            logger_Printer.error(e.getMessage());
        } finally {
            if (this.terminal != null) {
                try {
                    this.terminal.close();
                } catch (IOException e) {
                    logger_Printer.error(e.getMessage());
                }
            }
        }
    }

    private boolean screen_opening(){
        try {
            this.logger_Printer.debug("Displaying opening screen");
            textGraphics.putString(5, 2, this.pointer + " Log in");
            textGraphics.putString(5, 3, "  Create user");
            this.terminal.flush();

            KeyStroke keyStroke = this.terminal.readInput();
            while (true) {
                switch(keyStroke.getCharacter()){
                    // Index down
                    case 'j':
                        if (this.currentHighlight + 1 <= 1)
                            this.currentHighlight++;
                        break;
                    // Index up
                    case 'k':
                        if (this.currentHighlight - 1 >= 0)
                            this.currentHighlight--;
                        break;
                    // Enter directory
                    case 'a':
                        // TO-DO Do something
                        this.terminal.clearScreen();
                        if (this.currentHighlight == 0){
                            this.screen_authentication();
                            return true;
                        } else {
                            this.screen_createUser();
                        }
                        this.currentHighlight = 0;
                        break;
                    default:
                        this.logger_Printer.warn("Unknown key pressed " + keyStroke.getCharacter());
                        break;
                }

                this.logger_Printer.debug("Current highlight: " + this.currentHighlight);
                if (this.currentHighlight == 0){
                    textGraphics.putString(5, 2, this.pointer + " Log in");
                    textGraphics.putString(5, 3, "  Create user");
                    this.terminal.flush();
                } else {
                    textGraphics.putString(5, 2, "  Log in");
                    textGraphics.putString(5, 3, this.pointer + " Create user");
                    this.terminal.flush();
                }

                // Read key input
                keyStroke = this.terminal.readInput();
            }

        } catch (Exception e){
            this.logger_Printer.error(e.getMessage());
        }
        return false;
    }

    private void screen_createUser(){
        this.logger_Printer.debug("Displaying create user screen");
        try {
            StringBuilder currentlyTypedUsername = new StringBuilder();
            textGraphics.putString(5, 2, "Username: " + currentlyTypedUsername);
            textGraphics.putString(5, 3, "Password: ");
            this.terminal.flush();

            KeyStroke keyStroke = this.terminal.readInput();
            this.logger_Printer.info("Entering user creation username loop");
            while (keyStroke.getKeyType() != KeyType.Enter) {
                this.terminal.flush();
                if (keyStroke.getCharacter() == '\b') {
                    currentlyTypedUsername.setLength(Math.max(currentlyTypedUsername.length() - 1, 0));
                } else {
                    this.terminal.flush();
                    if (keyStroke.getKeyType() == KeyType.Backspace && !currentlyTypedUsername.isEmpty()) {
                        currentlyTypedUsername.deleteCharAt(currentlyTypedUsername.length() - 1);
                    } else {
                        currentlyTypedUsername.append(keyStroke.getCharacter());
                    }
                }

                this.terminal.clearScreen();
                textGraphics.putString(5, 2, "Username: " + currentlyTypedUsername);
                textGraphics.putString(5, 3, "Password: ");
                this.terminal.flush();

                keyStroke = this.terminal.readInput();
            }

            // Password
            StringBuilder currentlyTypedPassword = new StringBuilder();
            keyStroke = this.terminal.readInput();
            this.logger_Printer.info("Entering authentication password loop");
            while (keyStroke.getKeyType() != KeyType.Enter) {
                this.terminal.flush();
                if (keyStroke.getKeyType() == KeyType.Backspace && !currentlyTypedPassword.isEmpty()) {
                    currentlyTypedPassword.deleteCharAt(currentlyTypedPassword.length() - 1);
                } else {
                    currentlyTypedPassword.append(keyStroke.getCharacter());
                }
                textGraphics.putString(5, 3, "Password: " + (currentlyTypedPassword));
                this.terminal.flush();

                keyStroke = this.terminal.readInput();
            }

            ah.createUser(currentlyTypedUsername.toString(), currentlyTypedPassword.toString());
            this.terminal.clearScreen();
        } catch (Exception e){
            this.logger_Printer.error(e.getMessage());
        }

    }

    private void screen_authentication(){
        this.logger_Printer.debug("Displaying authentication screen");
        try {
            while (true){
                StringBuilder currentlyTypedUsername = new StringBuilder();
                textGraphics.putString(5, 2, "Username: " + currentlyTypedUsername);
                textGraphics.putString(5, 3, "Password: ");
                this.terminal.flush();

                KeyStroke keyStroke = this.terminal.readInput();
                this.logger_Printer.info("Entering authentication username loop");
                while (keyStroke.getKeyType() != KeyType.Enter){
                    this.terminal.flush();
                    if (keyStroke.getCharacter() == '\b'){
                        currentlyTypedUsername.setLength(Math.max(currentlyTypedUsername.length() - 1, 0));
                    } else {
                        this.terminal.flush();
                        if (keyStroke.getKeyType() == KeyType.Backspace && !currentlyTypedUsername.isEmpty()){
                            currentlyTypedUsername.deleteCharAt(currentlyTypedUsername.length() - 1);
                        } else {
                            currentlyTypedUsername.append(keyStroke.getCharacter());
                        }
                    }

                    this.terminal.clearScreen();
                    textGraphics.putString(5, 2, "Username: " + currentlyTypedUsername);
                    textGraphics.putString(5, 3, "Password: ");
                    this.terminal.flush();

                    keyStroke = this.terminal.readInput();
                }

                // Password
                StringBuilder currentlyTypedPassword = new StringBuilder();
                keyStroke = this.terminal.readInput();
                this.logger_Printer.info("Entering authentication password loop");
                while (keyStroke.getKeyType() != KeyType.Enter){
                    this.terminal.flush();
                    if (keyStroke.getKeyType() == KeyType.Backspace && !currentlyTypedPassword.isEmpty()){
                        currentlyTypedPassword.deleteCharAt(currentlyTypedPassword.length() - 1);
                    } else {
                        currentlyTypedPassword.append(keyStroke.getCharacter());
                    }
                    textGraphics.putString(5, 3, "Password: " + (currentlyTypedPassword));
                    this.terminal.flush();

                    keyStroke = this.terminal.readInput();
                }

                if (this.ah.authenticate(currentlyTypedUsername.toString(), currentlyTypedPassword.toString())){
                    this.logger_Printer.info("Authentication complete");
                    break;
                }
                textGraphics.putString(5, 5, "Incorrect username/password");
            }
            this.terminal.clearScreen();
        } catch (Exception e) {
            this.logger_Printer.error(e.getMessage());
        }
    }

    private void screen_directory(){
        this.logger_Printer.debug("Displaying directory screen");
        try {
            this.display_controls();
            this.display_directory();

            this.logger_Printer.debug("   Reading key input");
            // Read key input
            KeyStroke keyStroke = this.terminal.readInput();

            while (keyStroke.getKeyType() != KeyType.Escape) {
                this.logger_Printer.debug("Inside directory browser loop");

                // Print current directory
                this.dirToPrint = new ArrayList<>(this.getDirContent());

                switch(keyStroke.getCharacter()){
                    // Index down
                    case 'j':
                        if (this.currentHighlight + 1 < this.dirToPrint.size())
                            this.currentHighlight++;
                        break;
                    // Index up
                    case 'k':
                        if (this.currentHighlight - 1 >= 0)
                            this.currentHighlight--;
                        break;
                    // Enter directory
                    case 'a':
                        if (!dh.enterDir(this.currentHighlight))
                            break;
                        this.dirToPrint = new ArrayList<>(this.getDirContent());
                        this.currentHighlight = 0;
                        this.terminal.clearScreen();
                        break;
                    // Exit directory
                    case 's':
                        dh.enterDir(-1);
                        this.dirToPrint = new ArrayList<>(this.getDirContent());
                        this.currentHighlight = 0;
                        this.terminal.clearScreen();
                        break;
                    case 'w':
                        CryptoHandler.processDirectory(Mode.ENCRYPT, dh.currentDirPath, "aaaaaaaaaaaaaaaa");
                        this.dirToPrint = new ArrayList<>(this.getDirContent());
                        this.terminal.clearScreen();
                        break;
                    case 'e':
                        CryptoHandler.processDirectory(Mode.DECRYPT, dh.currentDirPath, "aaaaaaaaaaaaaaaa");
                        this.terminal.clearScreen();
                        break;
                    default:
                        this.logger_Printer.warn("Unknown key pressed " + keyStroke.getCharacter());
                        break;
                }

                this.display_directory();
                this.display_controls();

                // Read key input
                keyStroke = this.terminal.readInput();
            }
        } catch (Exception e) {
            this.logger_Printer.error(e.getMessage());
        }

    }

    /**
     * Singleton checking
     */
    public static Printer getInstance(DirectoryHandler dh) throws Exception {
        if (instance == null)
            instance = new Printer(dh);
        return instance;
    }

    /**
     * Prints all files and directories in the directory, given as an array of objects
     */
    public ArrayList<String> getDirContent(){
        ArrayList<String> toReturn = new ArrayList<>();
        try {
            this.logger_Printer.info("Printing dir content");
            for (FileObject child : this.dh.getDirContent()){
                // Can cause log files to be very large
//                this.logger_Printer.debug("   Printing " + child.getName().getBaseName());
                if (child.getType() == FileType.FOLDER)
                    toReturn.add("□ " + child.getName().getBaseName());
                if (child.getType() == FileType.FILE)
                    toReturn.add("  " + child.getName().getBaseName());
            }
        } catch (Exception e){
            this.logger_Printer.error(e.getMessage());
        }
        return toReturn;
    }

    /**
     * Displays directory content based on the DirectoryHandler
     */
    private void display_directory(){
        try {
            this.logger_Printer.debug("Retrieving content"); // Get directory content to display
            this.dirToPrint = new ArrayList<>(this.getDirContent());

            // Display directory content
            for (int i = 0; i <= this.dirToPrint.size()-1; i++){
                if (i == currentHighlight){
                    textGraphics.putString(5, (i+this.offset), pointer + " " + this.dirToPrint.get(i));
                } else{
                    textGraphics.putString(5, (i+this.offset), "  " + this.dirToPrint.get(i));
                }
            }
            this.terminal.flush();
        } catch (Exception e){
            this.logger_Printer.error(e.getMessage());
        }
    }

    /**
     * Displays controls at the bottom of the screen
     */
    private void display_controls(){
        try{
            this.logger_Printer.debug("Printing controls");
            textGraphics.putString(1, this.rows - 2, "j - up, k - down, a - enter dir, s - exit dir, w - encrypt dir, e - decrypt dir");
            this.terminal.flush();
        } catch (Exception e){
            this.logger_Printer.error(e.getMessage());
        }
    }

}
