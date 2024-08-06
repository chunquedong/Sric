//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.backend;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.Expr.*;
import sc2.compiler.ast.Stmt;
import sc2.compiler.ast.Type;
import sc2.compiler.ast.Visitor;

/**
 *
 * @author yangjiandong
 */
public class CppGenerator implements Visitor {
    
    PrintStream writer;
    private boolean needIndent = false;
    private int indentation = 0;
    
    public CppGenerator(String file) throws IOException {
        writer = new PrintStream(new FileOutputStream(file), true, "UTF-8");
    }
    
    public CppGenerator(PrintStream writer) {
        this.writer = writer;
    }

    void indent()
    {
        indentation++;
    }

    void unindent()
    {
        indentation--;
        if (indentation < 0) indentation = 0;
    }
    
    private CppGenerator print(String str) {
        if (needIndent) {
            for (int i=0; i<indentation; ++i) {
                writer.print("    ");
            }
            needIndent = false;
        }
        writer.print(str);
        return this;
    }
    
    private void newLine() {
        writer.println();
        needIndent = true;
        writer.flush();
    }
    
    private void printType(Type type) {
        if (type == null) {
            print("void");
            return;
        }
        printIdExpr(type.id);
    }

    @Override
    public boolean deepLevel() {
        return true;
    }

    @Override
    public void enterUnit(AstNode.FileUnit v) {
        
    }

    @Override
    public void exitUnit(AstNode.FileUnit v) {
        
    }

    @Override
    public void enterField(AstNode.FieldDef v) {
        printType(v.fieldType);
        print(" ");
        print(v.name);
    }

    @Override
    public void exitField(AstNode.FieldDef v) {
        print(";").newLine();
    }

    @Override
    public void enterFunc(AstNode.FuncDef v) {
        printType(v.prototype.returnType);
        print(" ");
        print(v.name);
        print("() {").newLine();
        
        indent();
    }

    @Override
    public void exitFunc(AstNode.FuncDef v) {
        unindent();
        print("}").newLine();
    }

    @Override
    public void enterTypeDef(AstNode.TypeDef v) {
        print("class ");
        print(v.name);
        print(" {").newLine();
        indent();
    }

    @Override
    public void exitTypeDef(AstNode.TypeDef v) {
        unindent();
        print("}").newLine();
    }

    @Override
    public void enterStmt(Stmt v) {
        
    }

    @Override
    public void exitStmt(Stmt v) {
        
    }

    @Override
    public void enterExpr(Stmt v) {
        
    }

    @Override
    public void exitExpr(Stmt v) {
        
    }
    
    
    private void printIdExpr(IdExpr id) {
        if (id.namespace != null) {
            printIdExpr(id.namespace);
            print("::");
        }
        print(id.name);
    }
    
}
