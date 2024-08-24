//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler;

import java.io.File;
import java.io.IOException;
import sc2.compiler.ast.SModule;

/**
 *
 * @author yangjiandong
 */
public class Main {
    
    

    public static void main(String[] args) throws IOException {
        String sourcePath = "../library/std/module.props";
        String libPath = "res/lib";
        
        for (int i = 1; i<args.length; ++i) {
            if (args[i].equals("-lib")) {
                ++i;
                libPath = args[i];
            }
            else {
                sourcePath = args[i];
            }
        }
        
        Compiler compiler;
        if (sourcePath.endsWith(".props")) {
            compiler = Compiler.fromProps(sourcePath, libPath);
        }
        else {
            compiler = Compiler.makeDefault(sourcePath, libPath);
        }
        compiler.run();
    }
}
