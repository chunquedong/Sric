/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sc2.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.Loc;
import sc2.compiler.resolve.ResolveExprType;

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
        nodes.add(node);
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
            ArrayList<AstNode> list = symbolTable.get(entry.getKey());
            if (list == null) {
                list = new ArrayList<AstNode>();
                symbolTable.put(entry.getKey(), list);
            }
            list.addAll(entry.getValue());
        }
    }
    
}
