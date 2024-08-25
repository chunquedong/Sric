//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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

        public String toString() {
            return name + " " + version;
        }
    }
    
    public String name;
    public String version;
    public HashMap<String, String> metaProps = null;
    
    public ArrayList<FileUnit> fileUnits = new ArrayList<>();
    public Scope scope = null;
    public ArrayList<Depend> depends = new ArrayList<>();
    
    public FileUnit findFileUnit(String file) {
        for (FileUnit v : fileUnits) {
            if (v.name.equals(file)) {
                return v;
            }
        }
        return null;
    }
    
    public static SModule fromProps(HashMap<String, String> props) {
        SModule m = new SModule();
        m.name = props.get("name");
        if (m.name == null) {
            throw new RuntimeException("Unknow name");
        }
        m.version = props.get("version");
        if (m.version == null) {
            throw new RuntimeException("Unknow version");
        }
        String dependsStr = props.get("depends");
        if (dependsStr == null) {
            throw new RuntimeException("Unknow depends");
        }
        
        if (dependsStr.length() > 0) {
            var dependsA = dependsStr.split(",");
            for (String depStr : dependsA) {
                depStr = depStr.trim();
                var fs = depStr.split(" ");
                if (fs.length != 2) {
                    throw new RuntimeException("parse depends error: "+depStr);
                }
                Depend depend = new Depend();
                depend.name = fs[0];
                depend.version = fs[1];
                m.depends.add(depend);
            }
        }
        
        m.metaProps = props;
        return m;
    }
    
    public HashMap<String, String> toMetaProps() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("name", name);
        map.put("version", version);
        
        StringBuilder sb = new StringBuilder();
        for (var d : depends) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(d.toString());
        }
        map.put("depends", sb.toString());
        
        if (metaProps != null) {
            map.put("summary", this.metaProps.get("summary"));
            map.put("license", this.metaProps.get("license"));
        }
        return map;
    }

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
