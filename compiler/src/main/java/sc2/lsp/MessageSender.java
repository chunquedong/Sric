/*
 * see license.txt
 */
package sc2.lsp;

import java.util.*;

import com.google.gson.*;
import java.io.File;
import sc2.compiler.CompilerLog;
import sc2.compiler.CompilerLog.CompilerErr;
import sc2.compiler.ast.Loc;

import sc2.lsp.JsonRpc.*;

/**
 * Sends messages back to the client
 * 
 * @author Tony
 *
 */
public class MessageSender {

    private Gson gson;
    private LspLogger log;
    
    public MessageSender(Gson gson, LspLogger log) {
        this.gson = gson;
        this.log = log;
    }

    public void sendMessage(Object msg) {
        String message = gson.toJson(msg);
        String output = String.format("Content-Length: %d\r\n\r\n%s", message.length(), message);        
        System.out.print(output);
        System.out.flush();        
        log.log("Sent: " + output);
    }
    
    public void sendDiagnostics(Workspace workspace, String documentUri) {
        CompilerLog result = workspace.processSource(documentUri);
        var errors = result.errors;
        
        PublishDiagnosticsParams params = new PublishDiagnosticsParams();
        params.uri = documentUri;
        params.diagnostics = Collections.emptyList();
        if(!errors.isEmpty()) {
            for(CompilerLog.CompilerErr error : errors) {
                
                String uri = new File(error.loc.file).toURI().toString();
                Document doc = workspace.getDocument(uri);
                if(doc == null) {
                    continue;
                }
                
                doc.errors.add(error);
            }
            
            Document doc = workspace.getDocument(documentUri);
            if(doc != null) {
                params.diagnostics = new ArrayList<>();
                for(CompilerErr error : doc.errors) {
                    Diagnostic d = new Diagnostic();                
                    d.message = error.msg;
                    d.severity = 1;
                    d.source = error.loc.file;
                    d.range = LspUtil.fromSrcPosLine(error.loc);
                    
                    params.diagnostics.add(d);
                }
            }
            
            doc.errors.clear();
        }
        
        RpcNotificationMessage notification = new RpcNotificationMessage();
        notification.method = "textDocument/publishDiagnostics";
        notification.params = params;
        
        sendMessage(notification);
    }
}
