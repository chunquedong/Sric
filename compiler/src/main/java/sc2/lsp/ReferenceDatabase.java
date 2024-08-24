/*
 * see license.txt
 */
package sc2.lsp;

import java.util.*;
import java.util.stream.Collectors;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.compiler.ast.Expr;
import sc2.compiler.ast.SModule;

import sc2.lsp.JsonRpc.*;

/**
 * @author Tony
 *
 */
public class ReferenceDatabase {
    private LspLogger log;
    
    public ReferenceDatabase(LspLogger log) {
        this.log = log;
    }
    
    public void buildDatabase(SModule program) {
        //TODO
    }

    public List<CompletionItem> findCompletionItems(FileUnit module, Position pos, List<String> fields) {
        log.log("findCompletionItems");
        return Collections.emptyList();
    }

    public List<Location> findReferencesFromNode(AstNode node) {
        log.log("Finding reference from: " + (node != null ? node.getClass().getSimpleName() : ""));
        return Collections.emptyList();
    }

    public static CompletionItem toCompletionItem(AstNode sym) {
        CompletionItem item = new CompletionItem();
        if(sym == null) {
            return item;
        }
        
        String name = sym.toString();
        if (sym instanceof Expr.IdExpr id) {
            var decl = id.resolvedDef;
            name = id.name;
            if(decl != null && decl.loc != null && decl instanceof AstNode.TopLevelDef td) {
                if (td.comment != null) {
                    item.documentation = td.comment.getDoc();
                }
                item.deprecated = td.isDeprecated();
                name = td.name;
            }
        }

        item.kind = CompletionItemKind.fromSymbol(sym).getValue();
        item.detail = name;
        item.label = name;
        return item;
    }
}
