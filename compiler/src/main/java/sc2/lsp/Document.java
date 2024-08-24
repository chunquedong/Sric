/*
 * see license.txt
 */
package sc2.lsp;

import java.util.*;
import java.util.stream.Collectors;
import sc2.compiler.CompilerLog.CompilerErr;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.compiler.ast.Expr;

import sc2.lsp.JsonRpc.*;

/**
 * Represents a litac module document
 */
public class Document {

    private List<Integer> lineMap;
    private StringBuilder buffer;
    
    public TextDocument document;
    public List<CompilerErr> errors;
    public final String moduleId;
    
    private LspLogger log;
    
    public Document(String moduleId, TextDocument document, LspLogger log) {
        this.moduleId = moduleId;
        this.document = document;
        this.log = log;
        this.lineMap = new ArrayList<>();
        this.errors = new ArrayList<>();
        
        setText(document.text);
    }

    public int getLineStart(int lineNumber) {
        return this.lineMap.get(lineNumber);
    }
        
    public void insert(Range range, String text) {
        int fromIndex = getLineStart(range.start.line) + range.start.character;
        int toIndex = getLineStart(range.end.line) + range.end.character;
        
        this.buffer.replace(fromIndex, toIndex, text);   
        refreshLineMap();
    }
    
    public void setText(String text) {
        this.buffer = new StringBuilder(text);
        refreshLineMap();
    }
    
    private void refreshLineMap() {
        this.lineMap.clear();
        this.lineMap.add(0); // first line starts with the first character
         
        for(int i = 0; i < this.buffer.length(); i++) {
            char c = this.buffer.charAt(i);
            if(c == '\n') {
                this.lineMap.add(i + 1);
            }
        }       
        
        this.document.text = this.buffer.toString();
    }
    
    private FileUnit getModule(Workspace workspace, boolean doFullBuild) {
        if(doFullBuild && !workspace.isFullyBuilt()) {
            workspace.processSource();
        }
        
        //TODO
        return null;
    }
    
    private AstNode findSourceNode(Workspace workspace, Position pos, boolean doFullBuild) {
        FileUnit module = getModule(workspace, doFullBuild);
        if(module == null) {
            return null;
        }
        
        SourceToAst sta = new SourceToAst();
        return sta.findSourceNode(log, module, pos);
    }
    
    public String getText() {
        return this.document.text;
    }
    
    public Location getDefinitionLocation(Workspace workspace, Position pos) {
        AstNode node = findSourceNode(workspace, pos, false);
        if(node == null) {
            return null;
        }
        
        return LspUtil.locationFromNode(node);
    }
    
    public List<Location> getReferences(Workspace workspace, Position pos) {
        
        AstNode location = findSourceNode(workspace, pos, true);
        if(location == null) {
            log.log("No source location found");
            return Collections.emptyList();
        }
        
        ReferenceDatabase database = workspace.getReferences();
        List<Location> locations = database.findReferencesFromNode(location);                
        return locations;
    }
    
    public List<CompletionItem> getAutoCompletionList(Workspace workspace, Position pos) {
        FileUnit module = getModule(workspace, true);
        if(module == null) {
            log.log("No program built");
            return Collections.emptyList();
        }
        
        ReferenceDatabase database = workspace.getReferences();
        List<String> fields = findIdentifier(pos);
        return database.findCompletionItems(module, pos, fields);              
    }
    
    private List<String> findIdentifier(Position pos) {
        List<String> fields = new ArrayList<>();
        
        int index = getLineStart(pos.line) + pos.character - 1;
        StringBuffer sb = new StringBuffer();
        while(index > -1) {
            char c = this.buffer.charAt(index);
            
            if(Character.isWhitespace(c)) {                
                index = skipWhitespace(index);
                if(index < 0) {
                    break;
                }
                
                continue;
            }
            
            if(!(Character.isAlphabetic(c) || Character.isDigit(c) || c == '_' || c > 256) && c != '.') {        
                break;
            }
            
            index--;
            sb.append(c);
        }
                
        String split[] = sb.reverse().toString().split("\\.");
        log.log("Splits: " + Arrays.toString(split) + " vs " + sb.reverse().toString());
        fields.addAll(Arrays.asList(split));
                
        return fields;
    }
    
    private int skipWhitespace(int index) {
        while(index > -1) {
            char c = this.buffer.charAt(index);
            if(Character.isWhitespace(c)) {
                index--;
                continue;
            }
            if(c == '.') {
                return index;
            }
            return -1;
        }
        
        return index;
    }
    
    public List<SymbolInformation> getSymbols(Workspace workspace) {

        FileUnit module = getModule(workspace, true);
        if(module == null) {
            return null;
        }
        
        ArrayList<SymbolInformation> list = new ArrayList<SymbolInformation>();
        for (AstNode.TypeAlias typeAlias : module.typeAlias) {
            list.add(fromSymbol(typeAlias));
        }
        for (AstNode.TypeDef typeDef : module.typeDefs) {
            list.add(fromSymbol(typeDef));
        }
        for (AstNode.FieldDef field : module.fieldDefs) {
            list.add(fromSymbol(field));
        }
        for (AstNode.FuncDef func : module.funcDefs) {
            list.add(fromSymbol(func));
        }
        
        return list;
    }
    
    public static SymbolInformation fromSymbol(AstNode sym) {
        SymbolInformation info = new SymbolInformation();
        if (sym instanceof Expr.IdExpr id) {
            info.name = id.name;
        }
        else {
            info.name = sym.toString();
        }
        info.kind = SymbolKind.fromSymbol(sym).getValue();
        
        if (sym instanceof Expr.IdExpr id) {
            var decl = id.resolvedDef;
            if(decl != null) {
                info.location = LspUtil.locationFromNode(decl);
            }
            if(decl instanceof AstNode.TopLevelDef td) {
                info.deprecated = td.isDeprecated();
            }
        }
        return info;
    }
}
