//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler;

import sc2.compiler.ast.Loc;

/**
 *
 * @author yangjiandong
 */
public class CompilerLog {
    public static class CompilerErr extends RuntimeException {
    
    }
    
    public CompilerErr err(String msg, Loc loc) {
        return new CompilerErr();
    }
}
