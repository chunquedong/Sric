//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import java.io.File;
import java.util.ArrayList;
import sc2.compiler.Util;

/**
 *
 * @author yangjiandong
 */
public class SModule extends AstNode {
    
    public static class Depend {
        public String name;
        public String version;
        public SModule cache;
        
        public static Depend fromFile(File file) {
            String basename = Util.getBaseName(file.getName());
            String[] fs = basename.split("-");
            if (fs.length == 2) {
                Depend depend = new Depend();
                depend.name = fs[0];
                depend.version = fs[1];
                return depend;
            }
            return null;
        }
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
                for (TypeAlias f : v.typeAlias) {
                    scope.put(f.name, f);
                }
            }
        }
        return scope;
    }
    
    @java.lang.Override
    public void walkChildren(Visitor visitor) {
        for (FileUnit v : fileUnits) {
            visitor.visit(v);
        }
    }
}
