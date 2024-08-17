//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.backend;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import sc2.compiler.CompilePass;
import sc2.compiler.CompilerLog;

/**
 *
 * @author yangjiandong
 */
public class BaseGenerator extends CompilePass {
    protected PrintStream writer;
    private boolean needIndent = false;
    private int indentation = 0;
    
    
    public BaseGenerator(CompilerLog log, String file) throws IOException {
        super(log);
        writer = new PrintStream(new FileOutputStream(file), true, "UTF-8");
    }
    
    public BaseGenerator(CompilerLog log, PrintStream writer) {
        super(log);
        this.writer = writer;
    }
    
        protected void indent()
    {
        indentation++;
    }

    protected void unindent()
    {
        indentation--;
        if (indentation < 0) indentation = 0;
    }
    
    protected BaseGenerator print(String str) {
        if (needIndent) {
            for (int i=0; i<indentation; ++i) {
                writer.print("    ");
            }
            needIndent = false;
        }
        writer.print(str);
        return this;
    }
    
    protected void newLine() {
        writer.println();
        needIndent = true;
        writer.flush();
    }
}
