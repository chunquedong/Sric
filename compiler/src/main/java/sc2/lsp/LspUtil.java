/*
 * see license.txt
 */
package sc2.lsp;

import java.io.File;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.Loc;

import sc2.lsp.JsonRpc.*;

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
        location.range = LspUtil.fromSrcPosLine(node.loc);
        location.range.end.character = node.len;
        return location;
    }
    
    public static Range fromSrcPosLine(Loc srcPos) {
        int lineNumber = Math.max(0, srcPos.line - 1);
                
        Range range = new Range();
        range.start = new Position();
        range.start.line = lineNumber;
        range.start.character = 0;
        
        range.end = new Position();
        range.end.line = lineNumber;    
        range.end.character = 1;
        return range;
    }
}
