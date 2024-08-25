/*
 * see license.txt
 */
package sc2.lsp;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.compiler.ast.SModule;
import sc2.compiler.ast.Scope;

import sc2.lsp.JsonRpc.*;

/**
 * Represents a project workspace, manages open documents and the corresponding AST
 */
public class Workspace {
    private String libPath;
    private Map<String, Document> documents;
    
    private LspLogger log;
    
    private Map<String, sc2.compiler.Compiler> moduleList;

    
    public Workspace(String libPath, LspLogger log) {        
        this.libPath = libPath;
        this.log = log;
        this.documents = new HashMap<>();
    }
    
    public void setRoot(File sourceDir) {
        log.log("Source Directory: '" + sourceDir);
    }

    private String canonicalPath(String docUri) {
        return new File(URI.create(docUri)).toString();
    }
    
    private sc2.compiler.Compiler build(String file, boolean force) throws IOException {
        File jfile = new File(file);
        File moduleFile = LspUtil.findModuleFile(jfile);
        if (moduleFile == null) {
            moduleFile = jfile;
        }
        String key = moduleFile.getPath();
        
        if (!force) {
            sc2.compiler.Compiler sm = moduleList.get(key);
            if (sm != null) {
                return sm;
            }
        }

        sc2.compiler.Compiler compiler;
        if (key.endsWith(".props")) {
            compiler = sc2.compiler.Compiler.fromProps(key, libPath);
        }
        else {
            compiler = sc2.compiler.Compiler.makeDefault(key, libPath);
        }
        compiler.genCode = false;
        compiler.run();
        
        moduleList.put(key, compiler);
        return compiler;
    }
    
    public void addDocument(TextDocument document) {
        try {
            String latestDocumentUri = canonicalPath(document.uri);
            sc2.compiler.Compiler unit = build(latestDocumentUri, false);
            this.documents.put(latestDocumentUri, new Document(document, this.log, unit, latestDocumentUri));
        } catch (IOException ex) {
            log.log("ERROR:"+ex.getMessage());
        }
    }
    
    public void removeDocument(String documentUri) {
        String moduleName = canonicalPath(documentUri);
        this.documents.remove(moduleName);
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
        
        if (document.compiler != null) {
            try {
                document.compiler.updateFile(document.ast.name, document.textBuffer.getText());
            } catch (IOException ex) {
                log.log("ERROR:"+ex.getMessage());
            }
        }
    }
   
    public void saveDocument(DidSaveTextDocumentParams params) {
        try {
            String latestDocumentUri = canonicalPath(params.textDocument.uri);
            Document document = this.documents.get(latestDocumentUri);
            if(params.text != null) {
                document.setText(params.text);
            }
            build(latestDocumentUri, true);
            log.log("Saving: " + params.textDocument.uri);
        } catch (IOException ex) {
            log.log("ERROR:"+ex.getMessage());
        }
    }
    
    public Document getDocument(String documentUri) {
        return this.documents.get(canonicalPath(documentUri));
    }

    public List<SymbolInformation> findAllSymbols(String query) {

        boolean endsWith = query.startsWith("*");
        boolean contains = query.startsWith("*") && query.endsWith("*") && query.length() > 2;
        String normalizedQuery = query.replace("*", "");
        
        ArrayList<SymbolInformation> list = new ArrayList<SymbolInformation>();
        for (var sm : moduleList.entrySet()) {
            Scope scope = sm.getValue().module.getScope();
            for (HashMap.Entry<String, ArrayList<AstNode>> entry : scope.symbolTable.entrySet()) {
                String name = entry.getKey();
                
                boolean ok = false;
                if(contains) {
                    if (name.contains(normalizedQuery)) {
                        ok = true;
                    }
                }
                if(endsWith) {
                    if (name.endsWith(normalizedQuery)) {
                        ok = true;
                    }
                }
                for (AstNode anode : entry.getValue()) {
                    list.add(LspUtil.toSymbolInfo(anode));
                }
            }
        }

        return list;
    }
}
