/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sc2.compiler.backend;

import java.io.IOException;
import java.io.PrintStream;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.ClosureExpr;
import sc2.compiler.ast.Expr;
import sc2.compiler.ast.SModule;
import sc2.compiler.ast.Stmt;
import sc2.compiler.ast.Type;

/**
 *
 * @author yangjiandong
 */
public class ScLibGenerator extends BaseGenerator {
    public ScLibGenerator(CompilerLog log, String file) throws IOException {
        super(log, file);
    }
    
    public ScLibGenerator(CompilerLog log, PrintStream writer) {
        super(log, writer);
    }
    
    public void run(SModule module) {
        module.walkChildren(this);
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

    private void printIdExpr(Expr.IdExpr id) {
        if (id.namespace != null) {
            printIdExpr(id.namespace);
            print("::");
        }
        print(id.name);
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

        printFuncPrototype(v.prototype, false);

        print(";");
    }
    
    private void printFuncPrototype(AstNode.FuncPrototype prototype, boolean isLambda) {
        print("(");
        if (prototype != null && prototype.paramDefs != null) {
            int i = 0;
            for (AstNode.ParamDef p : prototype.paramDefs) {
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
        
        if (isLambda && prototype != null) {
            if (prototype.returnType != null && prototype.returnType.isVoid()) {
                print("->");
                printType(prototype.returnType);
            }
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {
        if (v instanceof AstNode.EnumDef edef) {
            print("enum class ");
            print(v.name);
            print(" {").newLine();
            indent();

            int i = 0;
            for (AstNode.FieldDef f : edef.enumDefs) {
                if (i > 0) {
                    print(",").newLine();
                }
                print(f.name);
                if (f.initExpr != null) {
                    print(" = ");
                    this.visit(f.initExpr);
                    print(";");
                }
                ++i;
            }
            newLine();

            unindent();
            print("};").newLine();
            return;
        }
        
        print("struct ");
        print(v.name);
        print(" {").newLine();
        indent();
        
        v.walkChildren(this);
        
        unindent();
        newLine();
        print("};").newLine();
    }

    @Override
    public void visitStmt(Stmt v) {
        if (v instanceof AstNode.Block bs) {
            print("{").newLine();
            indent();
            bs.walkChildren(this);
            unindent();
            print("}").newLine();
        }
        else if (v instanceof Stmt.IfStmt ifs) {
            print("if (");
            this.visit(ifs.condition);
            print(") ");
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                print("else ");
                this.visit(ifs.elseBlock);
            }
        }
        else if (v instanceof Stmt.LocalDefStmt e) {
            this.visit(e.fieldDef);
        }
        else if (v instanceof Stmt.WhileStmt whiles) {
            print("while (");
            this.visit(whiles.condition);
            print(") ");
            this.visit(whiles.block);
        }
        else if (v instanceof Stmt.ForStmt fors) {
            print("for (");
            if (fors.init != null) {
                if (fors.init instanceof Stmt.LocalDefStmt varDef) {
                    printLocalFieldDefAsExpr(varDef.fieldDef);
                }
                else if (fors.init instanceof Stmt.ExprStmt s) {
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
        else if (v instanceof Stmt.SwitchStmt switchs) {
            print("switch (");
            this.visit(switchs.condition);
            print(") {").newLine();
            
            this.indent();
            
            for (Stmt.CaseBlock cb : switchs.cases) {
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
        else if (v instanceof Stmt.ExprStmt exprs) {
            this.visit(exprs.expr);
            print(";").newLine();
        }
        else if (v instanceof Stmt.JumpStmt jumps) {
            print(jumps.opToken.symbol).print(";").newLine();
        }
        else if (v instanceof Stmt.UnsafeBlock bs) {
            print("/*unsafe*/ ");
            this.visit(bs.block);
        }
        else if (v instanceof Stmt.ReturnStmt rets) {
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
        boolean isPrimitive = false;
        if (v instanceof Expr.IdExpr || v instanceof Expr.LiteralExpr || v instanceof Expr.CallExpr) {
            isPrimitive = true;
        }
        else {
            print("(");
        }
        
        if (v instanceof Expr.IdExpr e) {
            this.printIdExpr(e);
        }
        else if (v instanceof Expr.AccessExpr e) {
            this.visit(e.target);
            print(".");
            print(e.name);
        }
        else if (v instanceof Expr.LiteralExpr e) {
            printLiteral(e);
        }
        else if (v instanceof Expr.BinaryExpr e) {
            this.visit(e.lhs);
            print(e.opToken.symbol);
            this.visit(e.rhs);
        }
        else if (v instanceof Expr.CallExpr e) {
            this.visit(e.target);
            print("(");
            if (e.args != null) {
                int i = 0;
                for (Expr.CallArg t : e.args) {
                    if (i > 0) print(", ");
                    this.visit(t.argExpr);
                    ++i;
                }
            }
            print(")");
        }
        else if (v instanceof Expr.UnaryExpr e) {
            print(e.opToken.symbol);
            this.visit(e.operand);
        }
        else if (v instanceof Expr.TypeExpr e) {
            this.printType(e.type);
        }
        else if (v instanceof Expr.IndexExpr e) {
            this.visit(e.target);
            print("[");
            this.visit(e.index);
            print("]");
        }
        else if (v instanceof Expr.GenericInstance e) {
            this.visit(e.target);
            print("<");
            int i = 0;
            for (Type t : e.genericArgs) {
                if (i > 0) print(", ");
                this.visit(t);
                ++i;
            }
            print(" >");
        }
        else if (v instanceof Expr.IfExpr e) {
            this.visit(e.condition);
            print("?");
            this.visit(e.trueExpr);
            print(":");
            this.visit(e.falseExpr);
        }
        else if (v instanceof Expr.InitBlockExpr e) {
            printInitBlockExpr(e);
        }
        else if (v instanceof ClosureExpr e) {
            printClosureExpr(e);
        }
        else {
            err("Unkown expr:"+v, v.loc);
        }
        
        if (!isPrimitive) {
            print(")");
        }
    }
    
    void printLiteral(Expr.LiteralExpr e) {
        if (e.value instanceof Long li) {
            print(li.toString()).print("lld");
        }
        else if (e.value instanceof Double li) {
            print(li.toString());
        }
        else if (e.value instanceof Boolean li) {
            print(li.toString());
        }
        else if (e.value instanceof String li) {
            print("\"");
            for (int i=0; i<li.length(); ++i) {
                char c = li.charAt(i);
                printChar(c);
            }
            print("\"");
        }
    }

    void printChar(char c) {
        switch (c) {
            case '\b':
                print("\\b");
                break;
            case '\n':
                print("\\n");
                break;
            case '\r':
                print("\\r");
                break;
            case '\t':
                print("\\t");
                break;
            case '\"':
                print("\\\"");
                break;
            case '\'':
                print("\\\'");
                break;
            case '\\':
                writer.print('\\');
                break;
            default:
                writer.print(c);
        }
    }
    
    void printInitBlockExpr(Expr.InitBlockExpr e) {
        this.visit(e.target);
        for (Expr.CallArg t : e.args) {
            print(",");
            this.visit(t.argExpr);
        }
    }
    
    void printClosureExpr(ClosureExpr expr) {
        print("[");
        
        int i = 0;
        if (expr.defaultCapture != null) {
            print(expr.defaultCapture.symbol);
            ++i;
        }
        
        for (Expr t : expr.captures) {
            if (i > 0) print(", ");
            this.visit(t);
            ++i;
        }
        print("]");
        
        this.printFuncPrototype(expr.prototype, true);
        
        this.visit(expr.code);
    }
}
