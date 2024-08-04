//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import sc2.compiler.CompilerLog;

/**
 *
 * @author yangjiandong
 */
public class Loc {
    
    public final String file;
    public final int line;
    public final int col;
    public final int offset;

    public Loc(String file, int line, int col, int offset) {
        this.file = file;
        this.line = line;
        this.col = col;
        this.offset = offset;
    }
   
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(file).append("(").append(line).append(",").append(col).append(")");
        return sb.toString();
    }
}
