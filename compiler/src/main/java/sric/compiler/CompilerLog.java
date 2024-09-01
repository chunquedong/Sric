//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler;

import java.util.ArrayList;
import sric.compiler.ast.Loc;

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
        CompilerErr e = new CompilerErr(loc, msg);
        errors.add(e);
        return e;
    }
    
    public boolean hasError() {
        return errors.size() > 0;
    }
    
    public boolean printError() {
        if (hasError()) {
            System.err.print(toString());
            return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CompilerErr e : errors) {
            sb.append(e.loc).append(": ");
            sb.append(e.msg);
            sb.append('\n');
        }
        return sb.toString();
    }
}
