/*
 * see license.txt
 */
package sric.lsp;

import java.util.*;

import com.google.gson.JsonElement;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.FieldDef;
import sric.compiler.ast.AstNode.FuncDef;
import sric.compiler.ast.AstNode.TypeDef;


/**
 * Json RPC - Language Server Protocol types
 */
public class JsonRpc {
    public static enum MessageType {
        Error(1),
        Warning(2),
        Info(3),
        Log(4)
        ;
        
        private int value;
        MessageType(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    public static enum ErrorCodes {
        ParseError(-32700),
        InvalidRequest(-32600),
        MethodNotFound(-32601),
        InvalidParams(-32602),
        InternalError(-32603),
        ServerErrorStart(-32099),
        ServerErrorEnd(-32000),
        ServerNotInitialized(-32002),
        UnknownErrorCode(-32001),
    
        // Defined by the protocol.
        RequestCancelled(-32800),
        ContentModified(-32801),
    
        ;
        
        private int value;
        ErrorCodes(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }
    
    public static enum SymbolKind {
        File(1),
        Module(2),
        Namespace(3),
        Package(4),
        Class(5),
        Method(6),
        Property(7),
        Field(8),
        Constructor(9),
        Enum(10),
        Interface(11),
        Function(12),
        Variable(13),
        Constant(14),
        String(15),
        Number(16),
        Boolean(17),
        Array(18),
        Object(19),
        Key(20),
        Null(21),
        EnumMember(22),
        Struct(23),
        Event(24),
        Operator(25),
        TypeParameter(26),
        
        ;
        
        private int value;
        SymbolKind(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static SymbolKind fromSymbol(AstNode sym) {            
            if (sym instanceof FieldDef) {
                return Field;
            }
            else if (sym instanceof FuncDef) {
                return Function;
            }
            else if (sym instanceof TypeDef) {
                return Struct;
            }
            return Variable;            
        }
    }
    
    public static enum CompletionItemKind {
        Text(1),
        Method(2),
        Function(3),
        Constructor(4),
        Field(5),
        Variable(6),
        Class(7),
        Interface(8),
        Module(9),
        Property(10),
        Unit(11),
        Value(12),
        Enum(13),
        Keyword(14),
        Snippet(15),
        Color(16),
        File(17),
        Reference(18),
        Folder(19),
        EnumMember(20),
        Constant(21),
        Struct(22),
        Event(23),
        Operator(24),
        TypeParameter(25),
        ;
        public int value;
                
        private CompletionItemKind(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static CompletionItemKind fromSymbol(AstNode sym) {
            if (sym instanceof FieldDef) {
                return Field;
            }
            else if (sym instanceof FuncDef) {
                return Function;
            }
            else if (sym instanceof TypeDef) {
                return Struct;
            }
            return Field;
        }
    }
    
//    public static class RpcMessage {
//        public String jsonrpc = "2.0";       
//    }
    
    public static class RpcRequest {
        public String jsonrpc = "2.0"; 
        public Integer id;
        public String method;
        public JsonElement params;
    }
    
    public static class RpcResponse {
        public String jsonrpc = "2.0"; 
        public Integer id;
        public Object result;
        public ResponseError error;
    }
    
    public static class RpcNotificationMessage {
        public String jsonrpc = "2.0"; 
        public String method;
        public Object params;
    }
    
    public static class ResponseError {
        public Integer code;
        public String message;
        public Object data;
    }
    
    public static class WorkspaceFolder {
        public String uri;
        public String name;
    }
    
    public static class InitializationParams {
        public Integer processId;
        public String rootPath;
        public String rootUri;
        public WorkspaceFolder[] workspaceFolders;
    }
    
    public static class DidOpenParams {
        public TextDocument textDocument;
    }
    
    public static class DidCloseParams {
        public TextDocumentIdentifier textDocument;
    }
    
    public static class DidChangeParams {
        public VersionedTextDocumentIdentifier textDocument;
        public TextDocumentContentChangeEvent[] contentChanges;
    }
    
    public static class VersionedTextDocumentIdentifier extends TextDocumentIdentifier {
        public Integer version;
    }
    
    public static class TextDocumentContentChangeEvent {
        public Range range;
        public Integer rangeLength;
        public String text;
    }
    
    public static class Range {
        public Position start;
        public Position end;
        
        @Override
        public int hashCode() {
            return Objects.hash(end, start);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Range other = (Range) obj;
            return Objects.equals(end, other.end) && Objects.equals(start, other.start);
        }
    }
    
    public static class Position {
        /**
	 * Line position in a document (zero-based).
	 */
        public Integer line;
        
        /**
	 * Character offset on a line in a document (zero-based). The meaning of this
	 * offset is determined by the negotiated `PositionEncodingKind`.
	 *
	 * If the character value is greater than the line length it defaults back
	 * to the line length.
	 */
        public Integer character;
        
        
        @Override
        public int hashCode() {
            return Objects.hash(character, line);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Position other = (Position) obj;
            return Objects.equals(character, other.character) && Objects.equals(line, other.line);
        }
        
        
    }
    
    public static class Location {
        public String uri;
        public Range range;
        
        
        @Override
        public int hashCode() {
            return Objects.hash(range, uri);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Location other = (Location) obj;
            return Objects.equals(range, other.range) && Objects.equals(uri, other.uri);
        }
    }
    
    public static class TextDocumentIdentifier {
        public String uri;
    }
        
    public static class TextDocument {
        public String uri;
        public String languageId;
        public Integer version;
        public String text;
    }
    
    public static class ShowMessageParams {        
        public int type;
        public String message;
    }
    
    public static class Capabilities {
        public ServerCapabilities capabilities;
    }
    
    public static class ServerCapabilities {
        public int textDocumentSync;
        public boolean definitionProvider;
        public boolean documentSymbolProvider;
        public boolean workspaceSymbolProvider;
        public boolean referencesProvider;
        public CompletionOptions completionProvider;
    }
        
    
    public static class CompletionRegistrationOptions {
        public String[] triggerCharacters;
        public String[] allCommitCharacters;
        public Boolean resolveProvider;
    }
    
    public static class CompletionOptions {
        public Boolean resolveProvider;
        public String[] triggerCharacters;        
    }
    
    public static class Diagnostic {
        public Range range;
        public int severity;
        public String code;
        public String source;
        public String message;
        // public DiagnosticRelatedInformation[] relatedInformation;
        
    }
    
    public static class PublishDiagnosticsParams {
        public String uri;
        public List<Diagnostic> diagnostics;
    }
    
    public static class TextDocumentPositionParams {
        public TextDocumentIdentifier textDocument;
        public Position position;
    }
    
    public static class CompletionContext {
        public int triggerKind; // 1 Invoked, 2 TriggerCharacter, 3 TriggerForIncompleteCompletions
        public String triggerCharacter;
    }
    
    public static class CompletionParams extends TextDocumentPositionParams {
        public CompletionContext context;
    }
    
    public static class ReferenceContext {
        public Boolean includeDeclaration;
    }
    
    public static class ReferenceParams extends TextDocumentPositionParams {
        public ReferenceContext context;
    }
    
    public static class DidSaveTextDocumentParams {
        public TextDocumentIdentifier textDocument;
        public String text;
    }
    
    public static class DocumentSymbolParams {
        public TextDocumentIdentifier textDocument;
    }
    
    public static class WorkspaceSymbolParams {
        public String query;
    }
    
    public static class SymbolInformation {
        public String name;
        public int kind;
        public boolean deprecated;
        public Location location;
        public String containerName;
    }
    
    public static class CompletionItem {
        public String label;
        public Integer kind;
        public String detail;
        public String documentation;
        public Boolean deprecated;
        public Boolean preselect;
        public String sortText;
        public String filterText;
        public String insertText;
        public Object/*InsertTextFormat*/ insertTextFormat;
        public Object/*TextEdit*/ textEdit;
        public Object/*TextEdit[]*/ additionalTextEdits;
        public String[] commitCharacters;
        public Object/*Command*/ command;
        public Object data;
    }
    
    public static class CompletionList {
        
    }
    
    public static class ProgressToken {
        // number | string
    }
    
    public static class WorkDoneProgressParams {
        public Object/*ProgressToken*/ workDoneToken;
    }
    
    public static class PartialResultParams {
        public Object /* ProgressToken*/ partialResultToken;
    }
}
