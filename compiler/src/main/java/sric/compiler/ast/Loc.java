//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

import sric.compiler.CompilerLog;

/**
 *
 * @author yangjiandong
 */
public class Loc {
    
    public final String file;
    //one base
    public final int line;
    //one base
    public final int col;
    //zero base global index
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
