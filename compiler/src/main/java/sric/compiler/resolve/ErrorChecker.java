//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.resolve;


import java.util.HashMap;
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
    private FileUnit curUnit = null;
    private WithBlockExpr curItBlock = null;
    
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
        curUnit = v;
        v.walkChildren(this);
        curUnit = null;
    }
    
    private boolean isCopyable(Type type) {
        if (type.id.resolvedDef == null) {
            return false;
        }
        
        if (type.detail instanceof Type.PointerInfo p2) {
            if (p2.pointerAttr == Type.PointerAttr.own) {
                return false;
            }
        }
        
        if (type.id.resolvedDef instanceof StructDef sd) {
            if ((sd.flags & FConst.Noncopyable) != 0) {
                return false;
            }
            for (FieldDef f : sd.fieldDefs) {
                if (!isCopyable(f.fieldType)) {
                    return false;
                }
            }
            return true;
        }
        return true;
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
        
        AstNode resolvedDef = idResolvedDef(target);
        if (resolvedDef != null) {
            if (resolvedDef instanceof AstNode.FieldDef) {
                if (!isCopyable(target.resolvedType)) {
                    if (to.detail instanceof Type.PointerInfo p2) {
                        if (p2.pointerAttr == Type.PointerAttr.own) {
                            err("Miss move keyword", loc);
                        }
                    }
                    else {
                        err("Miss move keyword", loc);
                    }
                }
            }
        }
        
        if (target instanceof LiteralExpr lit && to.detail instanceof Type.PointerInfo p2) {
            if (p2.pointerAttr == Type.PointerAttr.own) {
                lit.nullPtrType = to;
            }
        }
        
        if (from.detail instanceof Type.PointerInfo p1 && to.detail instanceof Type.PointerInfo p2) {
            if (p1.pointerAttr != Type.PointerAttr.raw && p2.pointerAttr == Type.PointerAttr.raw) {
                target.implicitTypeConvertTo = to;
                target.isPointerConvert = true;
            }
            else if (p1.pointerAttr != Type.PointerAttr.ref && p2.pointerAttr == Type.PointerAttr.ref) {
                target.implicitTypeConvertTo = to;
                target.isPointerConvert = true;
            }
            else if (p1.pointerAttr != p2.pointerAttr) {
                err("Unknow convert", loc);
            }
        }
        
        if (!from.equals(to)) {
            target.implicitTypeConvertTo = to;
        }
    }
    
    public static AstNode idResolvedDef(Expr target) {
        if (target instanceof Expr.NonNullableExpr e) {
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
        
        if (v.initExpr != null && v.fieldType != null) {
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
            if (v.parent instanceof FileUnit) {
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
                    (v.flags & FConst.Virtual) != 0) {
                err("Invalid abstract or virtual flags", v.loc);
            }
        }
        
        if (v.code == null) {
            if ((v.flags & (FConst.Abstract|FConst.Virtual|FConst.Extern| FConst.ExternC)) == 0) {
                if (curStruct != null) {
                    if ((curStruct.flags & (FConst.Abstract|FConst.Virtual|FConst.Extern| FConst.ExternC)) == 0) {
                        err("Miss fun code", v.loc);
                    }
                }
                else {
                    err("Miss fun code", v.loc);
                }
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
            verifyOperatorDef(v);
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
                    if (i == 0) {
                        if (inh.id.resolvedDef instanceof StructDef superSd) {
                            if ((superSd.flags & FConst.Abstract) != 0 || (superSd.flags & FConst.Virtual) != 0) {
                                //ok
                            }
                            else {
                                err("Base struct must be abstract or virutal", inh.loc);
                            }
                        }
                        else if (inh.id.resolvedDef instanceof TraitDef) {
                            //ok
                        }
                        else {
                            err("Invalid inheritance", inh.loc);
                        }
                    }
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
            if (curItBlock != null) {
                err("Return from with block", v.loc);
            }
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
    
    private void verifyOperatorDef(AstNode.FuncDef f) {
        
        if (f.parent instanceof FileUnit) {
            err("Can't be static", f.loc);
        }
        
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
//        if (target.resolvedType.detail instanceof Type.PointerInfo pinfo) {
//            if (pinfo.isNullable) {
//                err("Maybe null", target.loc);
//            }
//        }
        
        boolean isImutable = target.resolvedType.isImutable;
        if (target.resolvedType.isPointerType() && target.resolvedType.genericArgs != null) {
            isImutable = target.resolvedType.genericArgs.get(0).isImutable;
        }
        
        if (resolvedSlotDef instanceof AstNode.FieldDef f) {
            if (isImutable && (f.flags & FConst.Const) == 0) {
                err("Const error", loc);
            }
        }
        else if (resolvedSlotDef instanceof AstNode.FuncDef f) {
            if (isImutable && (f.prototype.postFlags & FConst.Mutable) != 0) {
                err("Const error", loc);
            }
        }
    }
    
    private void verifyUnsafe(Expr target) {
        if (target instanceof IdExpr id) {
            if (id.name.equals(TokenKind.thisKeyword.symbol) || id.name.equals(TokenKind.superKeyword.symbol)
                    || id.name.equals(TokenKind.dot.symbol)) {
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
                        if (defNode != null) {
                            if (defNode instanceof AstNode.FieldDef f) {
                                if (!f.isLocalVar && !f.fieldType.isNullablePointerType()) {
                                    err("Can't move", e.loc);
                                }
                            }
                            else if (defNode instanceof AstNode.ParamDef f) {
                                //ok
                            }
                            else {
                                err("Invalid move", e.loc);
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
            //verifyInt(e.index);
            if (e.resolvedDef != null) {
                Type paramType = e.resolvedDef.prototype.paramDefs.get(0).paramType;
                verifyTypeFit(e.index, paramType, e.index.loc);
            }
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
        else if (v instanceof Expr.WithBlockExpr e) {
            resolveWithBlockExpr(e);
        }
        else if (v instanceof Expr.ArrayBlockExpr e) {
            resolveArrayBlockExpr(e);
        }
        else if (v instanceof Expr.ClosureExpr e) {
            this.visit(e.code);
        }
        else if (v instanceof Expr.NonNullableExpr e) {
            this.visit(e.operand);
            if (e.operand.resolvedType.detail instanceof Type.PointerInfo pinfo) {
                if (!pinfo.isNullable) {
                    err("Must nullable expr", v.loc);
                }
            }
            else {
                err("Must nullable expr", v.loc);
            }
        }
        else {
            err("Unkown expr:"+v, v.loc);
        }
    }

    private void resolveWithBlockExpr(Expr.WithBlockExpr e) {
        this.visit(e.target);
        
//        if (e._storeVar == null && e._isType) {
//            boolean ok = false;
//            if (e.target instanceof Expr.IdExpr id) {
//                ok = true;
//            }
//            if (!ok) {
//                err("Value type init block must in standalone assgin statement", e.loc);
//            }
//        }
        
        boolean hasFuncCall = false;
        if (e.block != null) {
            curItBlock = e;
            for (Stmt t : e.block.stmts) {
                this.visit(t);
                if (t instanceof ExprStmt exprStmt) {
                    if (exprStmt.expr instanceof CallExpr) {
                        hasFuncCall = true;
                    }
                }
            }
            curItBlock = null;
        }
        if (!e.target.isResolved()) {
            return;
        }
        
        AstNode.StructDef sd = e._structDef;
        if (sd != null) {            
            if (e._isType && (sd.flags & FConst.Abstract) != 0) {
                err("It's abstract", e.target.loc);
            }
            
            if (e.block != null && !hasFuncCall) {
                
                HashMap<String,FieldDef> fields = new HashMap<>();
                sd.getAllFields(fields);
                
                for (HashMap.Entry<String,FieldDef> entry : fields.entrySet()) {
                    AstNode.FieldDef f = entry.getValue();
                    if (f.initExpr != null) {
                        continue;
                    }
                    if (f.fieldType.isNullablePointerType()) {
                        continue;
                    }
                    if (f.fieldType.isNum()) {
                        continue;
                    }
                    
                    boolean found = false;
                    for (Stmt t : e.block.stmts) {
                        if (t instanceof ExprStmt exprStmt) {
                            if (exprStmt.expr instanceof BinaryExpr bexpr) {
                                if (bexpr.opToken == TokenKind.assign && bexpr.lhs instanceof IdExpr idExpr) {
                                    if (idExpr.resolvedDef == f) {
                                        found = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!found) {
                        err("Field not init:"+f.name, e.loc);
                    }
                }

//                for (Expr.CallArg t : e.args) {
//                    if (!fields.containsKey(t.name)) {
//                        err("Field not found:"+t.name, t.loc);
//                    }
//                }
            }
        }
    }
    
    private void resolveArrayBlockExpr(Expr.ArrayBlockExpr e) {
        
        if (!e.type.id.isResolved()) {
            return;
        }
        
        for (Expr t : e.args) {
            this.verifyTypeFit(t, e.type.genericArgs.get(0), t.loc);
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
            if ((slotFlags & FConst.Protected) != 0) {
                if (fu.module != this.module) {
                    err("It's private or protected", loc);
                }
            }
            
            if ((slotFlags & FConst.Private) != 0) {
                if (fu != this.curUnit) {
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
                case isKeyword: {
                    verifyMetType(e.rhs);
                    Type targetType = ((TypeExpr)e.rhs).type;
                    if (targetType.detail instanceof Type.PointerInfo pinfo) {
                        if (pinfo.isNullable) {
                            err("Must non-nullable", e.rhs.loc);
                        }
                    }
                }
                    break;
                case asKeyword: {
                    verifyMetType(e.rhs);
                }
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
                    else if (e.lhs.resolvedType.isPointerType() && e.rhs.resolvedType.isPointerType()) {
                        if (e.lhs.resolvedType.isNullType() || e.rhs.resolvedType.isNullType()) {
                            //OK
                        }
                        else if (e.lhs.resolvedType.detail instanceof Type.PointerInfo p1 && e.rhs.resolvedType.detail instanceof Type.PointerInfo p2) {
                            if (p1.pointerAttr != p2.pointerAttr) {
                                err("Cant compare different type", e.loc);
                            }
                        }
                    }
                    else if (e.resolvedOperator != null) {
                        Type paramType = e.resolvedOperator.prototype.paramDefs.get(0).paramType;
                        verifyTypeFit(e.rhs, paramType, e.rhs.loc);
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
                    if (e.resolvedOperator != null) {
                        Type paramType = e.resolvedOperator.prototype.paramDefs.get(0).paramType;
                        verifyTypeFit(e.rhs, paramType, e.rhs.loc);
                    }
                    verifyUnsafe(e.lhs);
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
                            ok = true;
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
                        if (indexExpr.resolvedDef != null && indexExpr.resolvedDef.prototype.paramDefs.size() > 1) {
                            Type paramType = indexExpr.resolvedDef.prototype.paramDefs.get(1).paramType;
                            verifyTypeFit(e.rhs, paramType, e.rhs.loc);
                        }
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