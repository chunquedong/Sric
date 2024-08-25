/*
 * see license.txt
 */
package sc2.lsp;

import java.io.File;
import java.util.*;

import sc2.lsp.JsonRpc.*;

/**
 * Handles requests from the client
 * 
 * @author Tony
 *
 */
public class RequestHandler {

    private Workspace workspace;
    private MessageSender sender;
    private LspLogger log;
    
    public RequestHandler(Workspace workspace, 
                          MessageSender sender,
                          LspLogger log) {
        this.workspace = workspace;
        this.sender = sender;
        this.log = log;
    }

    public void handleInitialize(RpcRequest rpc, InitializationParams msg) {
        String path = msg.rootPath != null ? msg.rootPath : msg.rootUri;
        
        if(path != null) {
            File sourcePath = new File(path);
            //TODO
            this.workspace.setRoot(sourcePath);
            this.log.log("Workspace rootModule: '" + sourcePath + "'");
        }
        else {
            this.log.log("No root URI or path!");
        }
        
        Capabilities capabilities = new Capabilities();
        capabilities.capabilities = new ServerCapabilities();
        capabilities.capabilities.textDocumentSync = 2;
        capabilities.capabilities.definitionProvider = true;
        capabilities.capabilities.documentSymbolProvider = true;
        capabilities.capabilities.workspaceSymbolProvider = true;
        capabilities.capabilities.referencesProvider = true;
        capabilities.capabilities.completionProvider = new CompletionOptions();
        capabilities.capabilities.completionProvider.resolveProvider = true;
        capabilities.capabilities.completionProvider.triggerCharacters = new String[]{"."};
        
        RpcResponse response = new RpcResponse();
        response.id = rpc.id;
        response.result = capabilities;
        this.sender.sendMessage(response);
    }
    
    public void handleTextDocumentDidOpen(RpcRequest rpc, DidOpenParams params) {        
        this.workspace.addDocument(params.textDocument);        
        this.sender.sendDiagnostics(this.workspace, params.textDocument.uri);
    }
    
    public void handleTextDocumentDidClose(RpcRequest rpc, DidCloseParams params) {        
        this.workspace.removeDocument(params.textDocument.uri);        
    }
    
    public void handleTextDocumentDidChange(RpcRequest rpc, DidChangeParams params) {        
        this.workspace.changedDocument(params.textDocument.uri, params);
        this.sender.sendDiagnostics(this.workspace, params.textDocument.uri);
    }
    
    public void handleTextDocumentDidSave(RpcRequest rpc, DidSaveTextDocumentParams params) {        
        this.workspace.saveDocument(params);
        this.sender.sendDiagnostics(this.workspace, params.textDocument.uri);
    }
    
    public void handleTextDocumentCompletion(RpcRequest rpc, CompletionParams params) {
        Document doc = this.workspace.getDocument(params.textDocument.uri);
        List<CompletionItem> items = Collections.emptyList();
        if(doc != null) {
            items = doc.getAutoCompletionList(params.position);
        }
        
        RpcResponse resp = new RpcResponse();
        resp.id = rpc.id;
        resp.result = items;
        this.sender.sendMessage(resp);
    }
    
    public void handleTextDocumentDefinition(RpcRequest rpc, TextDocumentPositionParams params) {
        Document doc = this.workspace.getDocument(params.textDocument.uri);
        Location location = null;
        if(doc != null) {
            location = doc.getDefinitionLocation(params.position);
        }

        RpcResponse resp = new RpcResponse();
        resp.id = rpc.id;
        resp.result = location;
        this.sender.sendMessage(resp);
    }
    
    public void handleTextDocumentDocumentSymbol(RpcRequest rpc, DocumentSymbolParams params) {
        Document doc = this.workspace.getDocument(params.textDocument.uri);
        List<SymbolInformation> symbols = Collections.emptyList();
        if(doc != null) {
            symbols = doc.getSymbols();
        }
        
        RpcResponse resp = new RpcResponse();
        resp.id = rpc.id;
        resp.result = symbols;
        this.sender.sendMessage(resp);
    }
    
    public void handleTextDocumentReferences(RpcRequest rpc, ReferenceParams params) {
        Document doc = this.workspace.getDocument(params.textDocument.uri);
        List<Location> items = Collections.emptyList();
        if(doc != null) {
            items = doc.getReferences(params.position);
        }
        
        RpcResponse resp = new RpcResponse();
        resp.id = rpc.id;
        resp.result = items;
        this.sender.sendMessage(resp);
    }
    
    public void handleWorkspaceSymbol(RpcRequest rpc, WorkspaceSymbolParams params) {
        List<SymbolInformation> symbols = this.workspace.findAllSymbols(params.query);
        
        RpcResponse resp = new RpcResponse();
        resp.id = rpc.id;
        resp.result = symbols;
        this.sender.sendMessage(resp);
    }
}
