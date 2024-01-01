package org.SFM;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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


    /**
     * Responsible for graphics printing
     */
    private Printer(DirectoryHandler dh){
        this.logger_Printer = LoggerFactory.getLogger(Printer.class);
        logger_Printer.info("Printer logger instantiated");
        
        this.dh = dh;
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

            this.logger_Printer.debug("   Creating window resize listener");
            // Check for window resizing - boilerplate code from lanterna docs
            this.terminal.addResizeListener((terminal1, newSize) -> {
                // Be careful here though, this is likely running on a separate thread. Lanterna is threadsafe in
                // a best-effort way so while it shouldn't blow up if you call terminal methods on multiple threads,
                // it might have unexpected behavior if you don't do any external synchronization
                this.textGraphics.drawLine(5, 3, newSize.getColumns() - 1, 3, ' ');
                this.textGraphics.putString(5, 3, "Terminal Size: ", SGR.BOLD);
                this.textGraphics.putString(5 + "Terminal Size: ".length(), 3, newSize.toString());
                try {
                    terminal1.flush();
                } catch (IOException e) {
                    logger_Printer.error(Arrays.toString(e.getStackTrace()));
                }
            });

            this.display_directory();

            this.logger_Printer.debug("   Reading key input");
            // Read key input
            KeyStroke keyStroke = this.terminal.readInput();

            while (keyStroke.getKeyType() != KeyType.Escape) {
                logger_Printer.debug("Inside main loop");

                // Print current directory
                this.dirToPrint = new ArrayList<>(this.getDirContent());

                switch(keyStroke.getCharacter()){
                    // Index up
                    case 'j':
                        if (this.currentHighlight + 1 < this.dirToPrint.size())
                            this.currentHighlight++;
                        break;
                    // Index down
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
                }

                this.display_directory();

                // Display content
                this.terminal.flush();

                // Read key input
                keyStroke = this.terminal.readInput();
            }
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

    /**
     * Singleton checking
     */
    public static Printer getInstance(DirectoryHandler dh){
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
                this.logger_Printer.debug("   Printing " + child.getName().getBaseName());
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
            this.logger_Printer.debug("   Retrieving content"); // Get directory content to display
            this.dirToPrint = new ArrayList<>(this.getDirContent());

            // Display directory content
            for (int i = 0; i <= this.dirToPrint.size()-1; i++){
                if (i == currentHighlight){
                    textGraphics.putString(5, (i+offset), pointer + " " + this.dirToPrint.get(i));
                } else{
                    textGraphics.putString(5, (i+offset), "  " + this.dirToPrint.get(i));
                }
            }
            this.terminal.flush();
        } catch (Exception e){
            this.logger_Printer.error(e.getMessage());
        }
    }

}
