//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.Loc;
import sric.compiler.resolve.ExprTypeResolver;

/**
 *
 * @author yangjiandong
 */
public class Scope extends AstNode {
    
    public HashMap<String, ArrayList<AstNode>> symbolTable = new HashMap<>();

    public void put(String name, AstNode node) {
        ArrayList<AstNode> nodes = symbolTable.get(name);
        if (nodes == null) {
            nodes = new ArrayList<AstNode>();
            symbolTable.put(name, nodes);
        }
        for (AstNode anode : nodes) {
            if (anode == node) {
                return;
            }
        }
        nodes.add(node);
    }
    
    public boolean contains(String name) {
        return symbolTable.containsKey(name);
    }

    public AstNode get(String name, Loc loc, CompilerLog log) {
        ArrayList<AstNode> nodes = symbolTable.get(name);
        if (nodes == null) {
            return null;
        }
        if (nodes.size() > 1) {
            log.err("Mulit define " + nodes.get(0).loc + "," + nodes.get(1).loc, loc);
        }
        return nodes.get(0);
    }
    
    public void addAll(Scope other) {
        for (HashMap.Entry<String, ArrayList<AstNode>> entry : other.symbolTable.entrySet()) {
            for (AstNode anode : entry.getValue()) {
                put(entry.getKey(), anode);
            }
        }
    }
    
}
