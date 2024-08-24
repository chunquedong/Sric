/*
 * see license.txt
 */
package sc2.lsp;

import java.io.*;

/**
 * Logger for LSP (as stdout is already in use)
 */
public class LspLogger {

    private RandomAccessFile log;
    private boolean enableLog;
    
    public LspLogger(boolean enableLog) {
        try {
            this.enableLog = enableLog;
            if(enableLog) {
                this.log = new RandomAccessFile(new File("./sc2-lsp.log"), "rw");
                this.log.setLength(0);
            }
        }
        catch(Exception e) {
            System.err.println("Unable to create litac log file: " + e);
            throw new RuntimeException(e);
        }
    }


    public void log(String message) {
        try {
            if(this.enableLog) {
                this.log.writeBytes(message);
                this.log.writeBytes("\n");
            }
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
}
