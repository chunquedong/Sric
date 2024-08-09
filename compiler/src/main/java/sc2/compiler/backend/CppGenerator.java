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
import sc2.compiler.ast.Expr;
import sc2.compiler.ast.Expr.*;
import sc2.compiler.ast.Stmt;
import sc2.compiler.ast.Stmt.*;
import sc2.compiler.ast.Type;
import sc2.compiler.CompilePass;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode.*;

/**
 *
 * @author yangjiandong
 */
public class CppGenerator extends CompilePass {
    
    protected PrintStream writer;
    private boolean needIndent = false;
    private int indentation = 0;
    
    protected boolean headMode = true;
    
    public CppGenerator(CompilerLog log, String file, boolean headMode) throws IOException {
        super(log);
        writer = new PrintStream(new FileOutputStream(file), true, "UTF-8");
        this.headMode = headMode;
    }
    
    public CppGenerator(CompilerLog log, PrintStream writer) {
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
            print("auto");
            return;
        }
        
        switch (type.id.name) {
            case "Void":
                print("void");
                return;
            case "*":
                print("Ptr");
                return;
            case "Int":
                print("int"+type.size+"_t");
                return;
            case "Float":
                if (type.size == 64) {
                    print("double");
                }
                else {
                    print("float");
                }
                return;
            case "Bool":
                print("bool");
                return;
        }
        
        printIdExpr(type.id);
    }


    @Override
    public void visitUnit(AstNode.FileUnit v) {
        v.walkChildren(this);
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        printLocalFieldDefAsExpr(v);
        print(";").newLine();
    }
    
    void printLocalFieldDefAsExpr(AstNode.FieldDef v) {
        printType(v.fieldType);
        print(" ");
        print(v.name);
        
        if (v.initExpr != null) {
            print(" = ");
            this.visit(v.initExpr);
        }
    }

    @Override
    public void visitFunc(AstNode.FuncDef v) {
        printType(v.prototype.returnType);
        print(" ");
        print(v.name);
        print("(");

        if (v.prototype.paramDefs != null) {
            int i = 0;
            for (ParamDef p : v.prototype.paramDefs) {
                if (i > 0) {
                    print(", ");
                }
                print(p.name);
                print(" : ");
                printType(p.paramType);
                if (p.defualtValue != null) {
                    print(" = ");
                    this.visit(p.defualtValue);
                }
                ++i;
            }
        }
        
        print(")");
        if (v.code == null) {
            print(";");
        }
        else {
            this.visit(v.code);
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {
        print("struct ");
        print(v.name);
        print(" {").newLine();
        indent();
        
        v.walkChildren(this);
        
        unindent();
        print("};").newLine();
    }

    @Override
    public void visitStmt(Stmt v) {
        if (v instanceof Block bs) {
            print("{").newLine();
            indent();
            bs.walkChildren(this);
            unindent();
            print("}").newLine();
        }
        else if (v instanceof IfStmt ifs) {
            print("if (");
            this.visit(ifs.condition);
            print(") ");
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                print("else ");
                this.visit(ifs.elseBlock);
            }
        }
        else if (v instanceof WhileStmt whiles) {
            print("while (");
            this.visit(whiles.condition);
            print(") ");
            this.visit(whiles.block);
        }
        else if (v instanceof ForStmt fors) {
            print("for (");
            if (fors.init != null) {
                if (fors.init instanceof FieldDef varDef) {
                    printLocalFieldDefAsExpr(varDef);
                }
                else if (fors.init instanceof ExprStmt s) {
                    this.visit(s.expr);
                }
                else {
                    err("Unsupport for init stmt", fors.init.loc);
                }
            }
            print("; ");
            
            if (fors.condition != null) {
                this.visit(fors.condition);
            }
            print("; ");
            
            if (fors.update != null) {
                this.visit(fors.update);
            }
            print(") ");
            this.visit(fors.block);
        }
        else if (v instanceof SwitchStmt switchs) {
            print("switch (");
            this.visit(switchs.condition);
            print(") {").newLine();
            
            this.indent();
            
            for (CaseBlock cb : switchs.cases) {
                this.unindent();
                print("case ");
                this.visit(cb.caseExpr);
                print(":").newLine();
                this.indent();
                
                this.visit(cb.block);
                
                if (!cb.fallthrough) {
                    print("break;").newLine();
                }
            }
            
            if (switchs.defaultBlock != null) {
                this.unindent();
                print("default:").newLine();
                this.indent();
                this.visit(switchs.defaultBlock);
            }
 
            this.unindent();
            print("}").newLine();
        }
        else if (v instanceof ExprStmt exprs) {
            this.visit(exprs.expr);
            print(";").newLine();
        }
        else if (v instanceof JumpStmt jumps) {
            print(jumps.opToken.symbol).print(";").newLine();
        }
        else if (v instanceof UnsafeBlock bs) {
            print("/*unsafe*/ ");
            this.visit(bs.block);
        }
        else if (v instanceof ReturnStmt rets) {
            if (rets.expr != null) {
                print("return ");
                this.visit(rets.expr);
                print(";").newLine();
            }
            else {
                print("return;");
            }
        }
        else {
            err("Unkown stmt:"+v, v.loc);
        }
    }

    @Override
    public void visitExpr(Expr v) {
        print("TODO");
    }
    
    private void printIdExpr(IdExpr id) {
        if (id.namespace != null) {
            printIdExpr(id.namespace);
            print("::");
        }
        print(id.name);
    }
    
}
