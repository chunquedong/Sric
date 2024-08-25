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
        Document doc = workspace.getDocument(documentUri);
        ArrayList<CompilerErr> errors = doc.compiler.log.errors;
        
        PublishDiagnosticsParams params = new PublishDiagnosticsParams();
        params.uri = documentUri;
        
        if(!errors.isEmpty()) {
            params.diagnostics = new ArrayList<>();
            for(CompilerErr error : errors) {
                Diagnostic d = new Diagnostic();                
                d.message = error.msg;
                d.severity = 1;
                d.source = error.loc.file;
                d.range = LspUtil.fromSrcPosLine(error.loc, 0);

                params.diagnostics.add(d);
            }
        }
        else {
            params.diagnostics = Collections.emptyList();
        }
        
        RpcNotificationMessage notification = new RpcNotificationMessage();
        notification.method = "textDocument/publishDiagnostics";
        notification.params = params;
        
        sendMessage(notification);
    }
}
