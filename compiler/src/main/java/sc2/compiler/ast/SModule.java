/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sc2.compiler.ast;

import java.util.ArrayList;

/**
 *
 * @author yangjiandong
 */
public class SModule extends AstNode {
    
    public static class Depend {
        public String name;
        public String version;
    }
    
    public String name;
    public String version;
    
    public ArrayList<FileUnit> fileUnits = new ArrayList<>();
    public Scope scope = null;
    public ArrayList<Depend> depends = new ArrayList<>();

    public Scope getScope() {
        if (scope == null) {
            scope = new Scope();
            for (FileUnit v : fileUnits) {
                for (FieldDef f : v.fieldDefs) {
                    scope.put(f.name, f);
                }
                for (FuncDef f : v.funcDefs) {
                    scope.put(f.name, f);
                }
                for (TypeDef f : v.typeDefs) {
                    scope.put(f.name, f);
                }
            }
        }
        return scope;
    }
    
    @java.lang.Override
    public void walk(Visitor visitor) {
        for (FileUnit v : fileUnits) {
            v.walk(visitor);
        }
    }
}
