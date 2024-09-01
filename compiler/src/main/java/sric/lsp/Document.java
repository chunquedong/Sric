/*
 * see license.txt
 */
package sric.lsp;

import java.io.File;
import java.util.*;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.FileUnit;
import sric.compiler.ast.Expr;
import sric.compiler.ast.Expr.*;
import sric.compiler.resolve.ErrorChecker;
import sric.lsp.JsonRpc.*;

/**
 * Represents a litac module document
 */
public class Document {

    public DocumentText textBuffer;
    public FileUnit ast;
    public sric.compiler.Compiler compiler;
    private LspLogger log;
    
    public Document(TextDocument document, LspLogger log, sric.compiler.Compiler compiler, String file) {
        this.log = log;
        this.compiler = compiler;
        this.ast = compiler.module.findFileUnit(file);
        textBuffer = new DocumentText();
        textBuffer.setText(document.text);
    }

    public void insert(JsonRpc.Range range, String text) {
        textBuffer.insert(range, text);
    }
    
    public void setText(String text) {
        textBuffer.setText(text);
    }
    
    private AstNode getAstNodeAt(Position pos) {
        AstFinder sta = new AstFinder(null);
        int index = textBuffer.getPosIndex(pos);
        return sta.findSourceNode(ast, index);
    }
    
    public Location getDefinitionLocation(Position pos) {
        AstNode node = getAstNodeAt(pos);
        if(node == null) {
            return null;
        }
        if (node instanceof Expr e) {
            AstNode def = ErrorChecker.idResolvedDef(e);
            if(def == null) {
                return null;
            }
            return LspUtil.locationFromNode(def);
        }
        return null;
    }
    
    public List<Location> getReferences(Position pos) {
        
        AstNode node = getAstNodeAt(pos);
        if(node == null) {
            log.log("No source location found");
            return Collections.emptyList();
        }
        
        ReferenceFinder database = new ReferenceFinder(null);
        ArrayList<AstNode> refs = database.findRefs(ast.module, node);
        List<Location> locations = refs.stream().map((x)->LspUtil.locationFromNode(x)).toList();
        return locations;
    }
    
    public List<CompletionItem> getAutoCompletionList(Position pos) {
        FileUnit funit = ast;
        
        AstNode node = getAstNodeAt(pos);

        CompletionFinder database = new CompletionFinder();
        int index = textBuffer.getPosIndex(pos);
        String fields = findIdentifier(index);
        ArrayList<AstNode> refs = database.findSugs(funit, node, fields);
        List<CompletionItem> items = refs.stream().map((x)->LspUtil.toCompletionItem(x)).toList();
        return items;
    }
    
    private String findIdentifier(int index) {
        StringBuilder sb = new StringBuilder();
        while(index > -1) {
            char c = textBuffer.buffer.charAt(index);

            if ((Character.isAlphabetic(c) || Character.isDigit(c) || c == '_' || c > 256)) {
                sb.append(c);
            }
            else {
                break;
            }
            index--;
        }
        
        String name = sb.toString();
        log.log("findIdentifier: " + name);
        return name;
    }
    
    public List<SymbolInformation> getSymbols() {

        FileUnit module = ast;
        if(module == null) {
            return null;
        }
        
        ArrayList<SymbolInformation> list = new ArrayList<SymbolInformation>();
        for (AstNode.TypeAlias typeAlias : module.typeAlias) {
            list.add(LspUtil.toSymbolInfo(typeAlias));
        }
        for (AstNode.TypeDef typeDef : module.typeDefs) {
            list.add(LspUtil.toSymbolInfo(typeDef));
        }
        for (AstNode.FieldDef field : module.fieldDefs) {
            list.add(LspUtil.toSymbolInfo(field));
        }
        for (AstNode.FuncDef func : module.funcDefs) {
            list.add(LspUtil.toSymbolInfo(func));
        }
        
        return list;
    }

}
