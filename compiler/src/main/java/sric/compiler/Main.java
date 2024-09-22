//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler;

import java.io.File;
import java.io.IOException;
import sric.compiler.ast.SModule;
import sric.lsp.LanguageServer;

/**
 *
 * @author yangjiandong
 */
public class Main {
    
    public static void main(String[] args) throws IOException {
        String sourcePath = "../library/test/module.scm";
        String libPath = "res/lib";
        boolean recursion = true;
        boolean lsp = false;
        for (int i = 1; i<args.length; ++i) {
            if (args[i].equals("-lib")) {
                ++i;
                libPath = args[i];
            }
            else if (args[i].equals("-lsp")) {
                lsp = true;
            }
            else if (args[i].equals("-r")) {
                recursion = true;
            }
            else {
                sourcePath = args[i];
            }
        }
        
        if (lsp) {
            LanguageServer ls = new LanguageServer(libPath);
            ls.start();
            return;
        }
        
        if ( !compile(sourcePath, libPath, recursion)) {
            System.out.println("ERROR");
        }
    }
    
    public static boolean compile(String sourcePath, String libPath, boolean recursion) throws IOException {
        Compiler compiler;
        if (sourcePath.endsWith(".scm")) {
            compiler = Compiler.fromProps(sourcePath, libPath);
        }
        else {
            compiler = Compiler.makeDefault(sourcePath, libPath);
        }
        
        if (recursion) {
            for (SModule.Depend dep: compiler.module.depends) {
                String libFile = libPath + "/" + dep.name;
                String propsPath = libFile+".meta";
                var props = Util.readProps(propsPath);
                String sourcePath2 = props.get("sourcePath");
                if (sourcePath2 != null) {
                    if (!compile(sourcePath2, libPath, recursion)) {
                        System.out.println("ERROR");
                        return false;
                    }
                }
            }
        }
        
        return compiler.run();
    }
}
