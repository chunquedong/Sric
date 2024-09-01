/*
 * see license.txt
 */
package sric.lsp;

import java.io.File;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.Expr;
import sric.compiler.ast.Loc;
import sric.compiler.ast.Stmt.LocalDefStmt;
import sric.lsp.JsonRpc.*;

/**
 */
public class LspUtil {

    public static Location locationFromNode(AstNode node) {
        if(node == null) {
            return null;
        }
        
        String uri = new File(node.loc.file).toURI().toString();
        
        Location location = new Location();
        location.uri = uri;
        location.range = LspUtil.fromSrcPosLine(node.loc, node.len);
        return location;
    }
    
    public static Range fromSrcPosLine(Loc srcPos, int len) {
        int lineNumber = Math.max(0, srcPos.line - 1);
                
        Range range = new Range();
        range.start = new Position();
        range.start.line = lineNumber;
        range.start.character = srcPos.col-1;
        
        range.end = new Position();
        range.end.line = lineNumber;    
        range.end.character = srcPos.col-1 + len;
        return range;
    }

    public static SymbolInformation toSymbolInfo(AstNode sym) {
        SymbolInformation info = new SymbolInformation();
        if (sym instanceof Expr.IdExpr id) {
            info.name = id.name;
        }
        else if (sym instanceof Expr.AccessExpr id) {
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
    
    public static CompletionItem toCompletionItem(AstNode decl) {
        CompletionItem item = new CompletionItem();
        if(decl == null) {
            return item;
        }
        
        String name = "";
        if(decl instanceof AstNode.TopLevelDef td) {
            if (td.comment != null) {
                item.documentation = td.comment.getDoc();
            }
            item.deprecated = td.isDeprecated();
            name = td.name;
        }
        else if (decl instanceof LocalDefStmt ld) {
            name = ld.fieldDef.name;
        }

        item.kind = CompletionItemKind.fromSymbol(decl).getValue();
        item.detail = name;
        item.label = name;
        return item;
    }
    
    public static File findModuleFile(File file) {
        String name = file.getName();
        if (name.startsWith(".scm")) {
            return file;
        }
        File dir = file.getParentFile();
        for (File f : dir.listFiles()) {
            if (f.getName().startsWith(".scm")) {
                return file;
            }
        }
        
        return findModuleFile(dir);
    }
}
