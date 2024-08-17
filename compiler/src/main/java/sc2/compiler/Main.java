//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler;

import java.io.IOException;

/**
 *
 * @author yangjiandong
 */
public class Main {
    

    public static void main(String[] args) throws IOException {
        String sourceDir = "res/code/testInherit.sc";
        String libPath = "res/lib";
        
        for (int i = 1; i<args.length; ++i) {
            if (args[i].equals("-lib")) {
                ++i;
                libPath = args[i];
            }
            else {
                sourceDir = args[i];
            }
        }
        
        Compiler compiler = Compiler.makeDefault(sourceDir, libPath);
        compiler.run();
    }
}
