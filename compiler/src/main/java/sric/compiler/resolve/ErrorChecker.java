//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.resolve;


import sric.compiler.CompilePass;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.*;
import sric.compiler.ast.Expr.*;
import sric.compiler.ast.Stmt.*;
import sric.compiler.ast.Token.*;
import static sric.compiler.ast.Token.TokenKind.*;
import sric.compiler.ast.Type.*;
import sric.compiler.ast.*;
/**
 *
 * @author yangjiandong
 */
public class ErrorChecker extends CompilePass {
    
    private SModule module;
    private AstNode.StructDef curStruct = null;
    private int inUnsafe = 0;
    
    public ErrorChecker(CompilerLog log, SModule module) {
        super(log);
        this.module = module;
        this.log = log;
    }
    
    public void run() {
        module.walkChildren(this);
    }

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        v.walkChildren(this);
    }
    
    private void verifyTypeFit(Expr target, Type to, Loc loc) {
        Type from = target.resolvedType;
        if (from == null) {
            return;
        }
        
        if (!from.fit(to)) {
            err("Type mismatch", loc);
            return;
        }
        
        if (from.detail instanceof Type.PointerInfo p1 && to.detail instanceof Type.PointerInfo p2) {
            if (p1.pointerAttr == Type.PointerAttr.own && p2.pointerAttr == Type.PointerAttr.own) {
                AstNode resolvedDef = idResolvedDef(target);
                if (resolvedDef != null) {
                    if (resolvedDef instanceof AstNode.FieldDef) {
                        err("Miss move keyword", loc);
                    }
                }
            }
        }
    }
    
    public static AstNode idResolvedDef(Expr target) {
        if (target instanceof Expr.OptionalExpr e) {
            target = e.operand;
        }

        if (target instanceof Expr.IdExpr e) {
            return e.resolvedDef;
        }
        else if (target instanceof Expr.AccessExpr e) {
            return e.resolvedDef;
        }
        else if (target instanceof Expr.IndexExpr e) {
            return e.resolvedDef;
        }
        return null;
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        if (v.initExpr != null) {
            this.visit(v.initExpr);
        }
        
        if (v.fieldType == null) {
            err("Unkonw field type", v.loc);
        }
        
        if (v.initExpr == null && v.fieldType != null) {
            verifyTypeFit(v.initExpr, v.fieldType, v.loc);
        }
        
        if (v.initExpr == null && v.fieldType != null && v.fieldType.detail instanceof Type.PointerInfo pt) {
            if (!pt.isNullable) {
                if (v.parent instanceof AstNode.StructDef) {
                    //OK
                }
                else {
                    err("Non-nullable pointer must inited", v.loc);
                }
            }
        }
        
        if (v.fieldType != null) {
            boolean isStatic = false;
            if (v.parent instanceof FileUnit || (v.flags & FConst.Static) != 0) {
                isStatic = true;
            }
            if (isStatic && !v.fieldType.isImutable) {
                if ((v.flags & FConst.Unsafe) == 0) {
                    err("Static var must be const", v.loc);
                }
            }
        }
    }


    @Override
    public void visitFunc(AstNode.FuncDef v) {
        if (v.parent instanceof StructDef sd) {
            if ((v.flags & FConst.Virtual) != 0) {
                if ((sd.flags & FConst.Virtual) != 0 || (sd.flags & FConst.Abstract) != 0) {
                    //ok
                }
                else {
                    err("Struct must be virtual or abstract", v.loc);
                }
            }
            else if ((v.flags & FConst.Abstract) != 0) {
                if ((sd.flags & FConst.Abstract) != 0) {
                    //ok
                }
                else {
                    err("Struct must be abstract", v.loc);
                }
                if (v.code != null) {
                    err("abstract method must no code", v.loc);
                }
            }
        }
        else if (v.parent instanceof TraitDef tt) {
            if ((v.flags & FConst.Abstract) != 0) {
                if (v.code != null) {
                    err("abstract method must no code", v.loc);
                }
            }
        }
        else {
            if ((v.flags & FConst.Abstract) != 0 ||
                    (v.flags & FConst.Virtual) != 0 ||
                    (v.flags & FConst.Static) != 0) {
                err("Invalid flags", v.loc);
            }
        }
        
        if (v.prototype.paramDefs != null) {
            boolean hasDefaultValue = false;
            boolean hasVararg = false;
            for (ParamDef p : v.prototype.paramDefs) {
                if (p.defualtValue != null) {
                    if (hasDefaultValue) {
                        err("Default param must at last", p.loc);
                    }
                    hasDefaultValue = true;
                }
                if (p.paramType.isVarArgType()) {
                    if (hasVararg) {
                        err("Vararg must at last", p.loc);
                    }
                    hasVararg = true;
                }
            }
        }
        
        if ((v.flags & FConst.Readonly) != 0) {
            err("Invalid flags", v.loc);
        }
        
        if ((v.flags & FConst.Operator) != 0) {
            verifyOperator(v);
        }
        
        if (v.code != null) {
            if ((v.flags & FConst.Unsafe) != 0) {
                ++inUnsafe;
            }
            this.visit(v.code);
            if ((v.flags & FConst.Unsafe) != 0) {
                --inUnsafe;
            }
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {
        if (v instanceof StructDef sd) {
            curStruct = sd;
            
            if (sd.inheritances != null) {
                int i = 0;
                for (Type inh : sd.inheritances) {
                    if (i > 0) {
                        if (inh.id.resolvedDef != null) {
                            if (!(inh.id.resolvedDef instanceof TraitDef)) {
                                err("Unsupport multi struct inheritance", inh.loc);
                            }
                        }
                    }
                    ++i;
                }
            }
        }
        v.walkChildren(this);

        curStruct = null;
    }

    @Override
    public void visitStmt(Stmt v) {
        if (v instanceof AstNode.Block bs) {
            bs.walkChildren(this);
        }
        else if (v instanceof Stmt.IfStmt ifs) {
            this.visit(ifs.condition);
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                this.visit(ifs.elseBlock);
            }
            verifyBool(ifs.condition);
        }
        else if (v instanceof Stmt.LocalDefStmt e) {
            this.visit(e.fieldDef);
        }
        else if (v instanceof Stmt.WhileStmt whiles) {
            this.visit(whiles.condition);
            this.visit(whiles.block);
            verifyBool(whiles.condition);
        }
        else if (v instanceof Stmt.ForStmt fors) {
            if (fors.init != null) {
                if (fors.init instanceof Stmt.LocalDefStmt varDef) {
                    this.visit(varDef.fieldDef);
                }
                else if (fors.init instanceof Stmt.ExprStmt s) {
                    this.visit(s.expr);
                }
                else {
                    //err("Unsupport for init stmt", fors.init.loc);
                }
            }
            
            if (fors.condition != null) {
                this.visit(fors.condition);
                verifyBool(fors.condition);
            }
            
            if (fors.update != null) {
                this.visit(fors.update);
            }
            this.visit(fors.block);
        }
        else if (v instanceof Stmt.SwitchStmt switchs) {
            this.visit(switchs.condition);
            verifyInt(switchs.condition);
            
            for (Stmt.CaseBlock cb : switchs.cases) {
                this.visit(cb.caseExpr);
                this.visit(cb.block);
            }
            
            if (switchs.defaultBlock != null) {
                this.visit(switchs.defaultBlock);
            }
        }
        else if (v instanceof Stmt.ExprStmt exprs) {
            this.visit(exprs.expr);
        }
        else if (v instanceof Stmt.JumpStmt jumps) {

        }
        else if (v instanceof Stmt.UnsafeBlock bs) {
            ++inUnsafe;
            this.visit(bs.block);
            --inUnsafe;
        }
        else if (v instanceof Stmt.ReturnStmt rets) {
            if (rets.expr != null) {
                this.visit(rets.expr);
            }
        }
        else {
            err("Unkown stmt:"+v, v.loc);
        }
    }
    
    private void verifyBool(Expr condition) {
        if (condition.resolvedType != null && !condition.resolvedType.isBool()) {
            err("Must be Bool", condition.loc);
        }
    }
    
    private void verifyInt(Expr e) {
        if (e.resolvedType != null && !e.resolvedType.isInt()) {
            err("Must be Int type", e.loc);
        }
    }
    
    private void verifyMetType(Expr e) {
        if (e.resolvedType != null && !e.resolvedType.isMetaType()) {
            err("Type required", e.loc);
        }
    }
    
    private void verifyOperator(AstNode.FuncDef f) {
        if (f.name.equals("plus") || f.name.equals("minus") || 
                f.name.equals("mult") || f.name.equals("div")) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (f.prototype.returnType.isVoid()) {
                err("Must has return", f.loc);
            }
        }
        else if (f.name.equals("compare")) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (!f.prototype.returnType.isInt()) {
                err("Must return Int", f.loc);
            }
        }
        else if (f.name.equals(Buildin.getOperator)) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (f.prototype.returnType.isVoid()) {
                err("Must has return", f.loc);
            }
        }
        else if (f.name.equals(Buildin.setOperator)) {
            if (f.prototype.paramDefs.size() != 2) {
                err("Must 1 params", f.loc);
            }
        }
    }
    
    private void verifyAccess(Expr target, AstNode resolvedSlotDef, Loc loc) {
        if (resolvedSlotDef instanceof AstNode.FieldDef f) {
            if (target.resolvedType.isImutable && (f.flags & FConst.Const) == 0) {
                err("Const error", loc);
            }
        }
        else if (resolvedSlotDef instanceof AstNode.FuncDef f) {
            if (target.resolvedType.isImutable && (f.prototype.postFlags & FConst.Mutable) != 0) {
                err("Const error", loc);
            }
        }
    }
    
    private void verifyUnsafe(Expr target) {
        if (target instanceof IdExpr id) {
            if (id.name.equals("this")) {
                return;
            }
        }
        if (target.resolvedType != null && target.resolvedType.detail instanceof Type.PointerInfo pt) {
            if (pt.pointerAttr == Type.PointerAttr.raw) {
                if (inUnsafe == 0) {
                    err("Expect unsafe block", target.loc);
                }
            }
        }
        
        AstNode resolvedDef = idResolvedDef(target);
        if (resolvedDef != null) {
            if (resolvedDef instanceof AstNode.FuncDef f) {
                if ((f.flags & FConst.Unsafe) != 0) {
                    err("Expect unsafe block", target.loc);
                }
            }
            if (resolvedDef instanceof AstNode.FieldDef f) {
                if ((f.flags & FConst.Unsafe) != 0) {
                    err("Expect unsafe block", target.loc);
                }
            }
        }
    }

    @Override
    public void visitExpr(Expr v) {
        if (v instanceof Expr.IdExpr e) {
            if (e.resolvedDef != null) {                
                if (e.resolvedDef instanceof AstNode.FieldDef f) {
                    checkProtection(f, f.parent, v.loc, e.inLeftSide);
                }
                else if (e.resolvedDef instanceof AstNode.FuncDef f) {
                    checkProtection(f, f.parent, v.loc, e.inLeftSide);
                }
            }
        }
        else if (v instanceof Expr.AccessExpr e) {
            this.visit(e.target);
            verifyUnsafe(e.target);
            verifyAccess(e.target, e.resolvedDef, e.loc);
            if (e.resolvedDef != null) {                
                if (e.resolvedDef instanceof AstNode.FieldDef f) {
                    checkProtection(f, f.parent, v.loc, e.inLeftSide);
                }
                else if (e.resolvedDef instanceof AstNode.FuncDef f) {
                    checkProtection(f, f.parent, v.loc, e.inLeftSide);
                }
            }
        }
        else if (v instanceof Expr.LiteralExpr e) {
        }
        else if (v instanceof Expr.BinaryExpr e) {
            resolveBinaryExpr(e);
        }
        else if (v instanceof Expr.CallExpr e) {
            resolveCallExpr(e);
        }
        else if (v instanceof Expr.UnaryExpr e) {
            this.visit(e.operand);
            if (e.operand.isResolved()) {
                Token.TokenKind curt = e.opToken;
                switch (curt) {
                    //~
                    case tilde:
                        verifyInt(e.operand);
                        break;
                    //!
                    case bang:
                        verifyBool(e.operand);
                        break;
                    //+, -
                    case plus:
                    case minus:
                        break;
                    //*
                    case star:
                        if (e.resolvedType != null) {
                            verifyUnsafe(e.operand);
                        }
                        break;
                    //++, --
                    case increment:
                    case decrement:
                        verifyInt(e.operand);
                        break;
                    //&
                    case amp:
                        break;
                    case awaitKeyword:
                        break;
                    case moveKeyword:
                        AstNode defNode = idResolvedDef(e.operand);
                        if (defNode != null && defNode instanceof AstNode.FieldDef f) {
                            if (!f.isLocalVar) {
                                err("Can't move", e.loc);
                            }
                        }
                        else {
                            err("Invalid move", e.loc);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        else if (v instanceof Expr.TypeExpr e) {
        }
        else if (v instanceof Expr.IndexExpr e) {
            this.visit(e.target);
            verifyUnsafe(e.target);
            this.visit(e.index);
            verifyInt(e.index);
        }
        else if (v instanceof Expr.GenericInstance e) {
            this.visit(e.target);
        }
        else if (v instanceof Expr.IfExpr e) {
            this.visit(e.condition);
            this.visit(e.trueExpr);
            this.visit(e.falseExpr);
            verifyBool(e.condition);
            if (e.trueExpr.isResolved() && e.falseExpr.isResolved()) {
                if (!e.trueExpr.resolvedType.equals(e.falseExpr.resolvedType)) {
                    err("Type must equals", e.falseExpr.loc);
                }
            }
        }
        else if (v instanceof Expr.InitBlockExpr e) {
            resolveInitBlockExpr(e);
        }
        else if (v instanceof Expr.ClosureExpr e) {
            this.visit(e.code);
        }
        else if (v instanceof Expr.OptionalExpr e) {
            this.visit(e.operand);
        }
        else {
            err("Unkown expr:"+v, v.loc);
        }
    }

    private void resolveInitBlockExpr(Expr.InitBlockExpr e) {
        this.visit(e.target);
        if (e.args != null) {
            for (Expr.CallArg t : e.args) {
                this.visit(t.argExpr);
            }
        }
        if (!e.target.isResolved()) {
            return;
        }
        
        AstNode.StructDef sd = null;
        if (e.target instanceof Expr.IdExpr id) {
            if (id.resolvedDef instanceof AstNode.StructDef) {
                sd = (AstNode.StructDef)id.resolvedDef;
            }
        }
        else if (e.target instanceof Expr.CallExpr call) {
            AstNode rdef = e.target.resolvedType.id.resolvedDef;
            if (rdef != null) {
                if (rdef instanceof AstNode.StructDef) {
                    sd = (AstNode.StructDef)rdef;
                }
            }
        }
        else if (e.target instanceof Expr.TypeExpr te) {
            if (te.type.detail instanceof Type.ArrayInfo at) {
                for (Expr.CallArg t : e.args) {
                    if (t.name != null) {
                        err("Invalid name for array", t.loc);
                    }
                }
            }
        }

        if (sd != null) {
            if ((sd.flags & FConst.Abstract) != 0) {
                err("It's abstract", e.target.loc);
            }
            if (e.args != null) {
                for (AstNode.FieldDef f : sd.fieldDefs) {
                    if (f.initExpr != null) {
                        continue;
                    }
                    boolean found = false;
                    for (Expr.CallArg t : e.args) {
                        if (t.name.equals(f.name)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        err("Field not init:"+f.name, e.loc);
                    }
                }

                for (Expr.CallArg t : e.args) {
                    boolean found = false;
                    for (AstNode.FieldDef f : sd.fieldDefs) {
                        if (t.name.equals(f.name)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        err("Field not found:"+t.name, t.loc);
                    }
                }
            }
        }
    }
    
    private void resolveCallExpr(Expr.CallExpr e) {
        this.visit(e.target);
        verifyUnsafe(e.target);
        if (e.args != null) {
            for (Expr.CallArg t : e.args) {
                this.visit(t.argExpr);
            }
        }
        if (e.target.isResolved()) {
            if (e.target.resolvedType.detail instanceof Type.FuncInfo f) {
                
                if (e.args != null) {
                    if (f.prototype.paramDefs == null) {
                        err("Args error", e.loc);
                    }
                    else {
                        int i = 0;
                        for (Expr.CallArg t : e.args) {
                            if (t.name != null) {
                                if (!t.name.equals(f.prototype.paramDefs.get(i).name)) {
                                    err("Arg name error", t.loc);
                                }
                            }
                            verifyTypeFit(t.argExpr, f.prototype.paramDefs.get(i).paramType, t.loc);
                            ++i;
                        }
                        if (i < f.prototype.paramDefs.size()) {
                            if (f.prototype.paramDefs.get(i).defualtValue == null && !f.prototype.paramDefs.get(i).paramType.isVarArgType()) {
                                err("Arg number error", e.loc);
                            }
                        }
                    }
                }
                else if (f.prototype.paramDefs != null) {
                    if (f.prototype.paramDefs.get(0).defualtValue == null) {
                        err("Arg number error", e.loc);
                    }
                }
            }
            else {
                err("Call a non-function type:"+e.target, e.loc);
            }
        }
        else {
            return;
        }
    }
    
    private boolean checkProtection(AstNode.TopLevelDef slot, AstNode parent, Loc loc, boolean isSet) {
        int slotFlags = slot.flags;
        if (isSet && slot instanceof AstNode.FieldDef f) {
            if ((f.flags & FConst.Readonly) != 0) {
                slotFlags |= FConst.Private;
            }
        }
        
        if (parent instanceof AstNode.TypeDef tparent) {
            if (parent != curStruct) {
                if ((slotFlags & FConst.Private) != 0) {
                    err("It's private", loc);
                    return false;
                }

                if ((slotFlags & FConst.Protected) != 0) {
                    if (curStruct == null || !curStruct.isInheriteFrom(tparent)) {
                        err("It's protected", loc);
                    }
                }
            }
        }
        else if (parent instanceof AstNode.FileUnit fu) {
            if ((slotFlags & FConst.Private) != 0 || (slotFlags & FConst.Protected) != 0) {
                if (fu.module != this.module) {
                    err("It's private or protected", loc);
                }
            }
        }
        return false;
    }

    private void resolveBinaryExpr(Expr.BinaryExpr e) {

        this.visit(e.lhs);
        this.visit(e.rhs);
        
        if (e.lhs.isResolved() && e.rhs.isResolved()) {
            Token.TokenKind curt = e.opToken;
            switch (curt) {
                case isKeyword:
                    verifyMetType(e.rhs);
                    break;
                case asKeyword:
                    verifyMetType(e.rhs);
                    break;
                case eq:
                case notEq:
                case same:
                case notSame:
                case lt:
                case gt:
                case ltEq:
                case gtEq:
                    if ((e.lhs.resolvedType.isFloat() && e.rhs.resolvedType.isInt()) ||
                            (e.lhs.resolvedType.isInt() && e.rhs.resolvedType.isFloat())) {
                        if (curt == Token.TokenKind.eq || curt == Token.TokenKind.notEq || curt == Token.TokenKind.same || curt == Token.TokenKind.notSame) {
                            err("Cant compare different type", e.loc);
                        }
                    }
                    else if (!e.lhs.resolvedType.equals(e.rhs.resolvedType)) {
                        err("Cant compare different type", e.loc);
                    }
                    break;
                case doubleAmp:
                case doublePipe:
                    verifyBool(e.lhs);
                    verifyBool(e.rhs);
                    break;
                case leftShift:
                case rightShift:
                case pipe:
                case caret:
                case amp:
                case percent:
                    verifyInt(e.lhs);
                    verifyInt(e.rhs);
                    break;
                case plus:
                case minus:
                case star:
                case slash:

                    
                    break;
                case assign:
                case assignPlus:
                case assignMinus:
                case assignStar:
                case assignSlash:
                case assignPercent:
                    boolean ok = false;
                    if (e.lhs instanceof Expr.IdExpr idExpr) {
                        if (idExpr.resolvedDef instanceof AstNode.FieldDef f) {
                            //if (checkProtection(f, f.parent, f.loc, true)) {
                                ok = true;
                            //}
                        }
                        else {
                            err("Not assignable", e.lhs.loc);
                        }
                    }
                    else if (e.lhs instanceof Expr.AccessExpr accessExpr) {
                        if (accessExpr.resolvedDef instanceof AstNode.FieldDef f) {
                            //if (checkProtection(f, f.parent, f.loc, true)) {
                                ok = true;
                            //}
                        }
                    }
                    else if (e.lhs instanceof Expr.IndexExpr indexExpr) {
                        ok = true;
                        return;
                    }
                    else {
                        err("Not assignable", e.lhs.loc);
                    }
                    
                    if (ok) {
                        if (curt == Token.TokenKind.assign) {
                            verifyTypeFit(e.rhs, e.lhs.resolvedType, e.loc);
                            if (e.lhs instanceof IdExpr lr && e.rhs instanceof IdExpr ri) {
                                if (lr.namespace == ri.namespace) {
                                    if (lr.name.equals(ri.name)) {
                                        err("Self assign", e.loc);
                                    }
                                }
                            }
                        }
                        else if (!e.lhs.resolvedType.equals(e.rhs.resolvedType)) {
                            err("Type mismatch", e.loc);
                        }

                        if (e.resolvedType != null && e.resolvedType.isImutable) {
                            err("Const error", e.loc);
                        }
                    }
                    
                    break;
                default:
                    break;
            }
        }
    }
}