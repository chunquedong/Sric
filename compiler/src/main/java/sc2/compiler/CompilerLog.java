//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler;

import java.util.ArrayList;
import sc2.compiler.ast.Loc;

/**
 *
 * @author yangjiandong
 */
public class CompilerLog {
    public ArrayList<CompilerErr> errors = new ArrayList<CompilerErr>();
    
    public static class CompilerErr extends RuntimeException {
        public Loc loc;
        public String msg;
        
        public CompilerErr(Loc loc, String msg) {
            this.loc = loc;
            this.msg = msg;
        }
    }
    
    public CompilerErr err(String msg, Loc loc) {
        return new CompilerErr(loc, msg);
    }
}
