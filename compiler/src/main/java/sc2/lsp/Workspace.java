/*
 * see license.txt
 */
package sc2.lsp;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import sc2.compiler.CompilerLog;

import sc2.lsp.JsonRpc.*;

/**
 * Represents a project workspace, manages open documents and the corresponding AST
 */
public class Workspace {
    private String libPath;
    private Map<String, Document> documents;
    private String latestDocumentUri;
    private boolean isFullyBuilt;
    private ReferenceDatabase references;
    
    private LspLogger log;
    
    /**
     * 
     */
    public Workspace(String libPath, LspLogger log) {        
        this.libPath = libPath;
        this.log = log;
                
        this.documents = new HashMap<>();
        this.isFullyBuilt = false;
        
        this.references = new ReferenceDatabase(log);
    }
    
    public void setRoot(File sourceDir) {
        //TODO
        log.log("Source Directory: '" + sourceDir);
    }
    
    
    private String canonicalPath(String docUri) {
        return new File(URI.create(docUri)).toString();
    }
    
    private String getModuleId(String docUri) {
        return new File(URI.create(docUri)).getPath();
    }

    /**
     * @return the references
     */
    public ReferenceDatabase getReferences() {
        return references;
    }
            
    public void addDocument(TextDocument document) {
        this.latestDocumentUri = canonicalPath(document.uri);
        this.documents.put(this.latestDocumentUri, new Document(getModuleId(document.uri), document, this.log));
        this.isFullyBuilt = false;
    }
    
    public void removeDocument(String documentUri) {
        String moduleName = canonicalPath(documentUri);
        this.documents.remove(moduleName);
        this.isFullyBuilt = false;
    }

    public void changedDocument(String documentUri, DidChangeParams change) {
        Document document = this.documents.get(canonicalPath(documentUri));
        
        for(TextDocumentContentChangeEvent event : change.contentChanges) {
            if(event.range != null) {
                document.insert(event.range, event.text);
            }
            else {
                document.setText(event.text);
            }
        }
        
        this.isFullyBuilt = false;
    }
   
    public void saveDocument(DidSaveTextDocumentParams params) {        
        Document document = this.documents.get(canonicalPath(params.textDocument.uri));
        if(params.text != null) {
            document.setText(params.text);
        }
        
        log.log("Saving: " + params.textDocument.uri);
    }
    
    public Document getDocument(String documentUri) {
        return this.documents.get(canonicalPath(documentUri));
    }
    
    public List<Document> getDocuments() {
        return new ArrayList<>(this.documents.values());
    }
    
    private String getDocumentText(String moduleId) {
        Document document = this.documents.get(moduleId);
        if(document != null) {
            return document.getText();
        }
                       
        try {
            return new String(Files.readAllBytes(Path.of(moduleId)));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<SymbolInformation> findSymbols(String query) {
//        if(this.latestProgram == null) {
//            return Collections.emptyList();
//        }
//        
//        boolean endsWith = query.startsWith("*");
//        boolean contains = query.startsWith("*") && query.endsWith("*") && query.length() > 2;
//        String normalizedQuery = query.replace("*", "");
//        
//        Map<String, Symbol> symbols = new HashMap<>();
//        buildSymbols(this.latestProgram.getMainModule(), symbols, new HashSet<>());
//        
//        return symbols.values().stream()
//            .filter(sym -> {
//                if(contains) {
//                    return sym.name.contains(normalizedQuery);
//                }
//                
//                if(endsWith) {
//                    return sym.name.endsWith(normalizedQuery);
//                }
//                
//                return sym.name.startsWith(normalizedQuery) && !sym.isBuiltin();
//            })                
//            .map(sym -> LspUtil.fromSymbol(sym))
//            .sorted((a,b) -> a.name.compareTo(b.name))
//            .collect(Collectors.toList());
        return Collections.emptyList();
    }
    
    public boolean isFullyBuilt() {
        return isFullyBuilt;
    }
    
    public CompilerLog processSource() {
        this.isFullyBuilt = true;
        
        log.log("Doing full rebuild...");
        
        //TODO
        CompilerLog result = null;
        
        if(result.hasError()) {
            log.log("Rebuild failed with errors: " + result.toString());
        }
        else {
            log.log("Rebuild successfully");
        }
        
        return result;
    }
    
    public CompilerLog processSource(String documentUri) {
        return null;
    }
}
