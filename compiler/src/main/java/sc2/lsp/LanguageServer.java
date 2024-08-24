/*
 * see license.txt
 */
package sc2.lsp;

import java.io.*;

import com.google.gson.*;

import sc2.lsp.JsonRpc.*;

/**
 * The litaC language server
 */
public class LanguageServer {

    private LspLogger log;
    private Gson gson;
    private boolean isInitialized;
    
    private RequestHandler handler;
    private MessageSender sender;
    
    public LanguageServer(String libPath) {        
        this.isInitialized = false;
        
        this.gson = new GsonBuilder()
                .serializeNulls()
                .create();
        
        this.log = new LspLogger(true);
        
        this.sender = new MessageSender(this.gson, this.log);
        this.handler = new RequestHandler(new Workspace(libPath, log), sender, this.log);
    }
    
    
    private String readContents(BufferedReader reader, int contentLength) throws IOException {
        char[] contents = new char[contentLength];
        int bytesRead = 0;
        do {
            int r = reader.read(contents, bytesRead, contentLength - bytesRead);
            if(r < 0) {
                break;
            }
            bytesRead += r;
        } 
        while(bytesRead < contentLength);
        
        String raw = new String(contents);
        return raw;
    }
    
    
    public void start() throws IOException {        
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            boolean isRunning = true;
            while(isRunning) {      
                log.log("Waiting for request...");
                
                String line = reader.readLine().trim();
                log.log("Received line: " + line);
                
                final String prefix = "Content-Length: ";
                if(!line.startsWith(prefix)) {
                    log.log("Received an invalid message '" + line + "'");
                    break;
                }
                
                final int contentLength = Integer.valueOf(line.substring(prefix.length()));
                if(contentLength < 0) {
                    log.log("Received an invalid content length '" + contentLength + "'");
                    break;
                }
                
                
                final String emptyLine = reader.readLine().trim();
                if(!emptyLine.equals("")) {
                    log.log("Received an invalid message format 'Missing new line'");
                    break;    
                }
                
                
                final String raw = readContents(reader, contentLength);
                log.log("Received message: '" + raw + "'");                
                
                final RpcRequest msg = gson.fromJson(raw, RpcRequest.class);
                switch(msg.method) {
                    case "initialize": {
                        InitializationParams init = gson.fromJson(msg.params, InitializationParams.class);
                        this.handler.handleInitialize(msg, init);
                        this.isInitialized = true;
                        break;
                    }
                    case "shutdown": {                        
                        break;
                    }
                    case "exit": {
                        isRunning = false;
                        break;
                    }
                    default: {
                        if(!this.isInitialized) {
                            RpcResponse response = new RpcResponse();
                            response.id = msg.id;
                            response.error = new ResponseError();
                            response.error.code = ErrorCodes.ServerNotInitialized.getValue();
                            this.sender.sendMessage(response);
                            break;
                        }
                        
                        switch(msg.method) {
                            case "textDocument/didOpen": {     
                                this.handler.handleTextDocumentDidOpen(msg, gson.fromJson(msg.params, DidOpenParams.class));
                                break;
                            }
                            case "textDocument/didClose": {
                                this.handler.handleTextDocumentDidClose(msg, gson.fromJson(msg.params, DidCloseParams.class));
                                break;
                            }
                            case "textDocument/didChange": {
                                this.handler.handleTextDocumentDidChange(msg, gson.fromJson(msg.params, DidChangeParams.class));
                                break;
                            }
                            case "textDocument/didSave": {
                                this.handler.handleTextDocumentDidSave(msg, gson.fromJson(msg.params, DidSaveTextDocumentParams.class));
                                break;
                            }
                            case "textDocument/definition": {
                                this.handler.handleTextDocumentDefinition(msg, gson.fromJson(msg.params, TextDocumentPositionParams.class));
                                break;
                            }
                            case "textDocument/documentSymbol": {
                                this.handler.handleTextDocumentDocumentSymbol(msg, gson.fromJson(msg.params, DocumentSymbolParams.class));
                                break;
                            }
                            case "textDocument/completion": {
                                this.handler.handleTextDocumentCompletion(msg, gson.fromJson(msg.params, CompletionParams.class));
                                break;
                            }
                            case "textDocument/references": {
                                this.handler.handleTextDocumentReferences(msg, gson.fromJson(msg.params, ReferenceParams.class));
                                break;
                            }
                            case "workspace/symbol": {
                                this.handler.handleWorkspaceSymbol(msg, gson.fromJson(msg.params, WorkspaceSymbolParams.class));
                                break;
                            }
                        }
                        
                        break;
                    }
                }
                    
            }
        }
        
        log.log("Normal shutdown");  
    }
    
    public void shutdown() {
        
    }
}
