/*
 * see license.txt
 */
package sc2.lsp;

import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.lsp.JsonRpc.*;

/**
 * Finds the Ast node definition (represented by a {@link Location}) from a {@link Position}
 */
public class SourceToAst {

    private Position pos;
    private FileUnit module;
    private LspLogger log;
    
    public AstNode findSourceNode(LspLogger log, FileUnit module, Position pos) {
        this.log = log;
        this.module = module;
        this.pos = pos;
        
        //TODO
        return null;
    }

}
