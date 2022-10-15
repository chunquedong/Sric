//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   2 Dec 05  Brian Frank  Creation
//

**
** Walk the AST to resolve:
**   - Manage local variable scope
**   - Resolve loop for breaks and continues
**   - Resolve LocalDefStmt.init into full assignment expression
**   - Resolve Expr.ctype
**   - Resolve UknownVarExpr -> LocalVarExpr, FieldExpr, or CallExpr
**   - Resolve CallExpr to their MethodDef
**
class ResolveExpr : CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  new make(CompilerContext compiler)
    : super(compiler)
  {
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  override Void run()
  {
    //log.debug("ResolveExpr")
    walkUnits(VisitDepth.expr)
    bombIfErr
  }


//////////////////////////////////////////////////////////////////////////
// Stmt
//////////////////////////////////////////////////////////////////////////


  override Stmt[]? visitStmt(Stmt stmt)
  {
    switch (stmt.id)
    {
      case StmtId.expr:         resolveExprStmt((ExprStmt)stmt)
      case StmtId.forStmt:      resolveFor((ForStmt)stmt)
      case StmtId.breakStmt:    resolveBreak((BreakStmt)stmt)
      case StmtId.continueStmt: resolveContinue((ContinueStmt)stmt)
      case StmtId.localDef:     resolveLocalVarDef((LocalDefStmt)stmt)
    }
    return null
  }

  private Void resolveExprStmt(ExprStmt stmt)
  {
    // stand alone expr statements, shouldn't be left on the stack
    stmt.expr = stmt.expr.noLeave
  }

  private Void resolveLocalVarDef(LocalDefStmt def)
  {
    // check for type inference
    if (def.ctype == null)
      def.var_v.ctype = def.init.ctype//.inferredAs

    // bind to scope as a method variable
    bindToMethodVar(def)
    
    def.var_v.scopeLevel = this.scopeLevel

    // if init is null, then we default the variable to null (Fan
    // doesn't do true definite assignment checking since most local
    // variables use type inference anyhow)
    if (def.init == null && !def.isCatchVar) {
      def.init = LiteralExpr.makeDefaultLiteral(def.loc, def.ctype)
      if (def.init != null) def.init.scopeLevel = this.scopeLevel
    }
    // turn init into full assignment
    if (def.init != null)
      def.init = BinaryExpr.makeAssign(LocalVarExpr(def.loc, def.var_v), def.init)
    
  }

  private Void resolveFor(ForStmt stmt)
  {
    // don't leave update expression on the stack
    if (stmt.update != null) stmt.update = stmt.update.noLeave
  }

  private Void resolveBreak(BreakStmt stmt)
  {
    // find which loop we're inside of (checked in CheckErrors)
    stmt.loop = findLoop
  }

  private Void resolveContinue(ContinueStmt stmt)
  {
    // find which loop we're inside of (checked in CheckErrors)
    stmt.loop = findLoop
  }

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////

  override Expr visitExpr(Expr expr)
  {
    // resolve the expression
    expr = resolveExpr(expr)

    // expr type must be resolved at this point
    if ((Obj?)expr.ctype == null)
      throw err("Expr type not resolved: ${expr.id}: ${expr}", expr.loc)
      
    if (!expr.ctype.isResolved)
      ResolveType.doResolveType(this, expr.ctype)

    // if we resolved to a generic parameter like V or K,
    // then use its real underlying type
    //if (expr.ctype.typeDef is GeneriParamDefDef)
    //  expr.ctype = expr.ctype.physicalType

    // if this expression performs assignment against a local
    // variable, then note the reassignment so that we know it
    // is not a final variable (final being like Java semanatics)
    assignTarget := expr.assignTarget as LocalVarExpr
    if (assignTarget != null && assignTarget.var_v != null)
      assignTarget.var_v.reassigned

    return expr
  }

  private Expr resolveExpr(Expr expr)
  {
    switch (expr.id)
    {
      case ExprId.nullLiteral:      // LiteralExpr
        expr.ctype = ns.objType.toNullable
      case ExprId.trueLiteral:
        expr.ctype = ns.boolType
      case ExprId.falseLiteral:
        expr.ctype = ns.boolType
      case ExprId.intLiteral:
        expr.ctype = ns.intType
      case ExprId.floatLiteral:
        expr.ctype = ns.floatType
      case ExprId.strLiteral:
        expr.ctype = ns.strType
      case ExprId.typeLiteral:
//        expr.ctype = ns.typeType
//        LiteralExpr e := expr
//        TypeRef t := e.val
//        ResolveType.doResolveType(this, t)
//      case ExprId.localeLiteral:    // LocaleLiteralExpr
//        expr = resolveLocaleLiteral(expr)
//      case ExprId.slotLiteral:      // SlotLiteralExpr
//        expr = resolveSlotLiteral(expr)
      case ExprId.listLiteral:      // ListLiteralExpr
        expr = resolveList(expr)

      case ExprId.boolNot:          // UnaryExpr
      case ExprId.cmpNull:
      case ExprId.cmpNotNull:
        expr.ctype = ns.boolType

      case ExprId.elvis:
        expr = resolveElvis(expr)
      case ExprId.assign:           // BinaryExpr
        expr = resolveAssign(expr)

      case ExprId.same:
      case ExprId.notSame:
      case ExprId.boolOr:           // CondExpr
      case ExprId.boolAnd:
        expr.ctype = ns.boolType
      
      case ExprId.isExpr:           // TypeCheckExpr
      //case ExprId.isnotExpr:
      case ExprId.asExpr:
      case ExprId.coerce:
        expr = resolveTypeCheck(expr)

      case ExprId.call:             // CallExpr
        expr = resolveCall(expr)

      //case ExprId.construction:
      //  expr = resolveConstruction(expr)
      case ExprId.shortcut:         // ShortcutExpr (has ShortcutOp)
        expr = resolveShortcut(expr)

      case ExprId.field:            // FieldExpr
        expr = resolveField(expr)
      case ExprId.localVar:         // LocalVarExpr
        expr.ctype = ((LocalVarExpr)expr).var_v.ctype

      case ExprId.thisExpr:         // ThisExpr
        expr = resolveThis(expr)
      case ExprId.superExpr:        // SuperExpr
        expr = resolveSuper(expr)
      case ExprId.itExpr:           // ItExpr
        expr = resolveIt(expr)
      case ExprId.staticTarget:     // StaticTargetExpr
        expr.ctype = ((StaticTargetExpr)expr).target
      case ExprId.unknownVar:       // UnknownVarExpr
        expr = resolveVar(expr)
//      case ExprId.storage:
//        expr = resolveStorage(expr)
      case ExprId.ternary:          // TernaryExpr
        expr = resolveTernary(expr)
      case ExprId.complexLiteral:   // ComplexLiteral
        expr.ctype = ((ComplexLiteral)expr).target
      case ExprId.closure:          // ClosureExpr
        expr = resolveClosure(expr)
      //case ExprId.dsl:              // DslExpr
        //expr = resolveDsl(expr)
      case ExprId.throwExpr:        // ThrowExpr
        expr.ctype = ns.nothingType
//      case ExprId.awaitExpr:
//        expr = resolveAwait(expr)
      case ExprId.sizeOfExpr:
        expr.ctype = ns.intType
      case ExprId.addressOfExpr:
        expr = resolveAddressOf(expr)
      case ExprId.InitBlockExpr:
        expr = resolveInitListExpr(expr)
    }

    if (expr.scopeLevel == -1) expr.scopeLevel = this.scopeLevel
    return expr
  }
  
  
  private Expr resolveTypeCheck(TypeCheckExpr expr) {
    ResolveType.doResolveType(this, expr.check)
    switch (expr.id) {
      case ExprId.isExpr:           // TypeCheckExpr
      //case ExprId.isnotExpr:
        expr.ctype = ns.boolType

      case ExprId.asExpr:
        expr.ctype = ((TypeCheckExpr)expr).check
      case ExprId.coerce:
        expr.ctype = ((TypeCheckExpr)expr).check
    }
    return expr
  }
  
  private Expr resolveField(FieldExpr expr) {
    expr.field = expr.target.ctype.typeDef.slot(expr.name)
    if (expr.field == null) {
      expr.ctype = ns.error
    }
    else {
      expr.ctype = expr.field.fieldType
    }
    return expr
  }
  
  **
  ** If this is a standalone name without a base target
  ** such as "Foo" and the name maps to a type name, then
  ** this is a type literal.
  **
  private Expr? resolveStaticTypeTarget(NameExpr expr)
  {
    if (expr.target == null)
    {
      stypes := curUnit.imported[expr.name]

      // if more then, one first try to exclude those internal to other pods
      if (stypes != null && stypes.size > 1)
        stypes = stypes.exclude |t| { t.isInternal && t.podName != this.podName }

      if (stypes != null && !stypes.isEmpty)
      {
        if (stypes.size > 1)
          log.err("Ambiguous type: " + stypes.join(", "), expr.loc)
        
        typeDef := stypes.first as TypeDef
        if (typeDef != null) {
            type := TypeRef.makeResolvedType(typeDef, expr.loc)
            type.len = expr.name.size
            staticTargetExpr := StaticTargetExpr(expr.loc, type)
            staticTargetExpr.ctype = typeDef.asRef
            staticTargetExpr.len = expr.len
            call := expr as CallExpr
            if (call != null) {
              nexpr := CallExpr(expr.loc, staticTargetExpr, "<ctor>", ExprId.construction)
              ((CallExpr)nexpr).args = call.args
              nexpr.len = expr.len
              return nexpr
            }
            else {
              return staticTargetExpr
            }
        }
      }
    }
    return null
  }

//  private Expr resolveAwait(AwaitExpr yexpr) {
//    //yexpr.expr = resolveExpr(yexpr.expr)
//    if (yexpr.expr.ctype.fits(ns.promiseType)) {
//      awaitType := (yexpr.expr.ctype).genericArgs.first
//      if (awaitType != null) {
//        yexpr.ctype = awaitType.toNullable
//      } else {
//        yexpr.ctype = ns.objType.toNullable
//      }
//    }
//    else {
//      yexpr.ctype = yexpr.expr.ctype
//    }
//    return yexpr
//  }

  private Expr resolveAddressOf(AddressOfExpr expr) {
    //expr.var_v = resolveExpr(expr.var_v)
    expr.ctype = TypeRef.pointerType(expr.loc, expr.var_v.ctype, PtrType.temp_ptr)
    ResolveType.doResolveType(this, expr.ctype)
    expr.scopeLevel = expr.var_v.scopeLevel
    return expr
  }
  
  private Expr resolveInitListExpr(InitBlockExpr expr) {
    ResolveType.doResolveType(this, expr.baseType)
    if (expr.isPointer) {
        expr.ctype = TypeRef.pointerType(expr.loc, expr.baseType)
        expr.scopeLevel = 0
    }
    else {
        expr.ctype = expr.baseType
    }
    return expr
  }

  **
  ** Resolve slot literal
  **
//  private Expr resolveSlotLiteral(SlotLiteralExpr expr)
//  {
//    ResolveType.doResolveType(this, expr.parent)
//    slot := expr.parent.slot(expr.name)
//    if (slot == null)
//    {
//      err("Unknown slot literal '${expr.parent.signature}.${expr.name}'", expr.loc)
//      expr.ctype = ns.error
//      return expr
//    }
//    expr.ctype = slot is FieldDef ? ns.fieldType : ns.methodType
//    expr.slot = slot
//    return expr
//  }

  **
  ** Resolve list literal
  **
  private Expr resolveList(ListLiteralExpr expr)
  {
//    if (expr.explicitType != null)
//    {
      expr.ctype = expr.explicitType
//    }
//    else
//    {
//      // infer from list item expressions
//      v := CommonType.commonType(ns, expr.vals)
//      expr.ctype = TypeRef.listType(expr.loc, v)
//    }
    return expr
  }

  **
  ** Resolve this keyword expression
  **
  private Expr resolveThis(ThisExpr expr)
  {
    if (curType == null || curMethod == null || curMethod.isStatic) {
        err("Cannot access 'this' in static context", expr.loc)
    }
    
    expr.ctype = curType.asRef
    return expr
  }

  **
  ** Resolve super keyword expression
  **
  private Expr resolveSuper(SuperExpr expr)
  {
    if (inClosure)
    {
      // it would be nice to support super from within a closure,
      // but the Java VM has the stupid restriction that invokespecial
      // cannot be used outside of the class - we could potentially
      // work around this using a wrapper method - but for now we will
      // just disallow it
      err("Invalid use of 'super' within closure", expr.loc)
      expr.ctype = ns.error
      return expr
    }

    if (expr.explicitType != null)
      expr.ctype = expr.explicitType
    else
      expr.ctype = curType.base

    return expr
  }

  **
  ** Resolve it keyword expression
  **
  private Expr resolveIt(ItExpr expr)
  {
    // can't use it keyword outside of an it-block
    if (!inClosure)
    {
      err("Invalid use of 'it' outside of it-block", expr.loc)
      expr.ctype = ns.error
      return expr
    }

    // closure's itType should be defined at this point
    //expr.ctype = curType.itType
    return expr
  }

  **
  ** Resolve an assignment operation
  **
  private Expr resolveAssign(BinaryExpr expr)
  {
    // if lhs has synthetic coercion we need to remove it;
    // this can occur when resolving a FFI field - in order
    // for this to work there are only two possible allowed
    // coercions: 1) a TypeCheckExpr or 2) a CallExpr where
    // the non-coerced expression is the last argument
    if (expr.lhs.synthetic)
    {
      if (expr.lhs.id === ExprId.coerce)
        expr.lhs = ((TypeCheckExpr)expr.lhs).target
      else if (expr.lhs.id === ExprId.call)
        expr.lhs = ((CallExpr)expr.lhs).args.last
      else
        throw Err("Unexpected LHS synthetic expr: $expr [$expr.loc.toLocStr]")
    }

    // check for left hand side the [] shortcut, because []= is set
    shortcut := expr.lhs as ShortcutExpr
    if (shortcut != null && shortcut.op == ShortcutOp.get)
    {
      shortcut.op = ShortcutOp.set
      shortcut.name = "set"
      shortcut.args.add(expr.rhs)
      shortcut.method = null
      return resolveCall(shortcut)
    }

    // assignment is typed by lhs
    expr.ctype = expr.lhs.ctype

    return expr
  }

  **
  ** Resolve an UnknownVar to its replacement node.
  **
  private Expr resolveVar(UnknownVarExpr var_v)
  {
    if (var_v.target == null) {
        binding := resolveSymbol(var_v.name, var_v.loc)
        if (binding != null) {
          if (binding is MethodVar) {
            res := LocalVarExpr(var_v.loc, binding)
            res.len = var_v.len
            return res
          }
          else if (binding is FieldDef) {
            f := binding as FieldDef
            Expr? res
            if (f.isStatic || curType == null) {
                res = FieldExpr(var_v.loc, null, f)
            }
            else {
                thisExpr := ThisExpr(var_v.loc, curType.asRef)
                res = FieldExpr(var_v.loc, thisExpr, f)
            }
            res.len = var_v.len
            return res
          }
          else if (binding is MethodDef) {
            f := binding as MethodDef
            Expr? res
            if (f.isStatic || curType == null) {
                res = CallExpr(var_v.loc, null, f)
            }
            else {
                thisExpr := ThisExpr(var_v.loc, curType.asRef)
                res = CallExpr(var_v.loc, thisExpr, f)
            }
            res.len = var_v.len
            return res
          }
        }
    }
    else {
        binding := var_v.target.ctype.typeDef.slot(var_v.name)
           if (binding is FieldDef) {
            f := binding as FieldDef
            Expr res = FieldExpr(var_v.loc, var_v.target, f)
            res.len = var_v.len
            return res
          }
          else if (binding is MethodDef) {
            f := binding as MethodDef
            Expr res = CallExpr(var_v.loc, var_v.target, f)
            res.len = var_v.len
            return res
          }
    }
    return var_v
  }

  **
  ** Resolve "x ?: y" expression
  **
  private Expr resolveElvis(BinaryExpr expr)
  {
    if (expr.lhs.ctype != expr.rhs.ctype) {
        err("invalid elvis expr")
    }
    expr.ctype = expr.lhs.ctype
    return expr
  }

  **
  ** Resolve "x ? y : z" ternary expression
  **
  private Expr resolveTernary(TernaryExpr expr)
  { 
    if (expr.trueExpr.id === ExprId.nullLiteral)
      expr.ctype = expr.falseExpr.ctype.toNullable
    else if (expr.falseExpr.id === ExprId.nullLiteral)
      expr.ctype = expr.trueExpr.ctype.toNullable
    else {
        if (expr.trueExpr.ctype != expr.falseExpr.ctype) {
            err("invalid ternary expr")
        }
        expr.ctype = expr.trueExpr.ctype
    }
    return expr
  }

  **
  ** Resolve a call to it's Method and return type.
  **
  private Expr resolveCall(CallExpr call)
  {
    //if there is no target, attempt to bind to local variable
    if (call.target == null)
    {
      // attempt to a name in the current scope
      binding := resolveSymbol(call.name, call.loc)
      if (binding != null) {
        if (binding is MethodDef) {
            f := binding as MethodDef
            call.method = f
            call.ctype = f.ret
            
            if (!f.isStatic && curType != null) {
                thisExpr := ThisExpr(call.loc, curType.asRef)
                call.target = thisExpr
            }
            return call
        }
        else {
            
        }
      }
      return call
    }
    
    //maybe ctor
//    ctor := resolveStaticTypeTarget(call)
//    if (ctor != null) {
//      return this.resolveConstruction(ctor)
//    }

    binding := call.target.ctype.typeDef.slot(call.name)
    if (binding is MethodDef) {
      f := binding as MethodDef
      Expr res = CallExpr(call.loc, call.target, f)
      res.len = call.len
      return res
    }
    return call
  }

  **
  ** Resolve ShortcutExpr.
  **
  private Expr resolveShortcut(ShortcutExpr expr)
  {
    // if this is an indexed assigment such as x[y] += z
//    if (expr.isAssign && expr.target.id === ExprId.shortcut)
//      return resolveIndexedAssign(expr)

    // string concat is always optimized, and performs a bit
    // different since a non-string can be used as the lhs
//    if (expr.isStrConcat)
//    {
//      expr.ctype  = ns.strType
//      expr.method = ns.strPlus
//      return ConstantFolder(compiler).fold(expr)
//    }

    // if a binary operation
    if (expr.args.size == 1 && expr.op.isOperator)
    {
      method := resolveBinaryOperator(expr)
      if (method == null) { expr.ctype = ns.error; return expr }
      expr.method = method
      expr.name   = method.name
    }

    // resolve the call, if optimized, then return it immediately
    result := resolveCall(expr)
    if (result !== expr) return result

    // check that method has Operator facet
    if (expr.method != null && expr.op.isOperator && !expr.method.isOperator)
      err("Missing Operator facet: $expr.method.qname", expr.loc)

    // the comparision operations are special case that call a method
    // that return an Int, but leave a Bool on the stack (we also handle
    // specially in assembler)
    switch (expr.opToken)
    {
      case Token.lt:
      case Token.ltEq:
      case Token.gt:
      case Token.gtEq:
        expr.ctype = ns.boolType
    }
    
    //assignment is void except '=' 
    if (expr.isAssign && expr.opToken != Token.assign) {
        expr.ctype = ns.voidType
    }

    return expr
  }

  **
  ** Given a shortcut method such as 'lhs op rhs' figure
  ** out which method to use for the operator symbol.
  **
  private MethodDef? resolveBinaryOperator(ShortcutExpr expr)
  {
    op := expr.op
    lhs := expr.target.ctype
    rhs := expr.args.first

    if (lhs === ns.error || rhs.ctype === ns.error) return null

    // get matching operators for the method name
    matches := lhs.typeDef.operators.find(op.methodName)

    // if multiple matches, attempt to narrow by argument type
    if (matches.size > 1)
    {
      matches = matches.findAll |m|
      {
        if (m.params.size != 1) return false
        paramType := m.params.first.paramType
        return Coerce.canCoerce(rhs, paramType)
      }
    }

    // if no matches bail
    if (matches.isEmpty)
    {
      err("No operator method found: ${op.formatErr(lhs, rhs.ctype)}", expr.loc)
      return null
    }

    // if we have one match, we are golden
    if (matches.size == 1) return matches.first

    // still have an ambiguous operator method call
    names := (matches.map |MethodDef m->Str| { m.name }).join(", ")
    err("Ambiguous operator method: ${op.formatErr(lhs, rhs.ctype)} [$names]", expr.loc)
    return null
  }

  **
  ** ClosureExpr will just output its substitute expression.  But we take
  ** this opportunity to capture the local variables in the closure's scope
  ** and cache them on the ClosureExpr.  We also do variable name checking.
  **
  private Expr resolveClosure(ClosureExpr expr)
  {
    this.inClosure = true
    // save away current locals in scope
    //expr.enclosingVars = localsInScope

    // make sure none of the closure's parameters
    // conflict with the locals in scope
//    expr.doCall.paramDefs.each |ParamDef p|
//    {
//      if (expr.enclosingVars.containsKey(p.name) && p.name != "it")
//        err("Closure parameter '$p.name' is already defined in current block", p.loc)
//    }
    
    expr.signature.params.each |p| { ResolveType.doResolveType(this, p) }
    
    expr.ctype = expr.signature.typeRef
    
    this.inClosure = false
    return expr
  }

//////////////////////////////////////////////////////////////////////////
// Scope
//////////////////////////////////////////////////////////////////////////

  **
  ** Bind the specified local variable definition to a
  ** MethodVar (and register number).
  **
  private Void bindToMethodVar(LocalDefStmt def)
  {
    // make sure it doesn't exist in the current scope
    if (curScope.doFindSymbol(def.name) != null)
      err("Variable '$def.name' is already defined in current block", def.loc)

    // create and add it
    def.var_v = curMethod.addLocalVarForDef(def, curScope as Block)
    
    if (def.var_v.ctype != null) {
      ResolveType.doResolveType(this, def.var_v.ctype)
    }
  }

  **
  ** Resolve a local variable using current scope based on
  ** the block stack and possibly the scope of a closure.
  **
  private Symbol? resolveSymbol(Str name, Loc loc)
  {
    sym := curScope.findSymbol(name)
    if (sym == null) {
        err("Unknow symbol: $name", loc)
    }
    return sym
  }

//////////////////////////////////////////////////////////////////////////
// StmtStack
//////////////////////////////////////////////////////////////////////////

  private Stmt? findLoop()
  {
    for (i:=stmtStack.size-1; i>=0; --i)
    {
      stmt := stmtStack[i]
      if (stmt.id === StmtId.whileStmt) return stmt
      if (stmt.id === StmtId.forStmt)   return stmt
    }
    return null
  }
  
  override Void enterStmt(Stmt stmt) {
    super.enterStmt(stmt);
    stmtStack.push(stmt)
    if (stmt.id === StmtId.forStmt) {
        s := stmt as ForStmt
        s.block.parent = curScope; curScope = s.block
        ++scopeLevel
    }
  }
  
  override Void exitStmt(Stmt stmt) {
    super.exitStmt(stmt);
    stmtStack.pop
    if (stmt.id === StmtId.forStmt) {
        s := stmt as ForStmt
        curScope = curScope.parentScope
        --scopeLevel
    }
  }

//////////////////////////////////////////////////////////////////////////
// Scope
//////////////////////////////////////////////////////////////////////////

  override Void enterBlock(Block block) {
    super.enterBlock(block); 
    if (curScope === block) return
    block.parent = curScope; curScope = block
    ++scopeLevel
  }
  override Void exitBlock(Block block)  {
    super.exitBlock(block); 
    if (curScope !== block) return
    curScope = curScope.parentScope
    --scopeLevel
  }
  
  virtual Void enterUnit(CompilationUnit unit) { super.enterUnit(unit); curScope = unit }
  virtual Void exitUnit(CompilationUnit unit) { super.exitUnit(unit); curScope = null }

  virtual Void enterTypeDef(TypeDef def) {
    super.enterTypeDef(def); 
    curScope = def
  }
  virtual Void exitTypeDef(TypeDef def) { super.exitTypeDef(def); curScope = curScope.parentScope }

  virtual Void enterMethodDef(MethodDef def) {
    super.enterMethodDef(def);
    def.parent = curScope
    curScope = def
    ++scopeLevel
  }
  virtual Void exitMethodDef(MethodDef def) { super.exitMethodDef(def); curScope = curScope.parentScope; --scopeLevel }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  Stmt[] stmtStack  := Stmt[,]    // statement stack
  //Block[] blockStack := Block[,]  // block stack used for scoping
  Scope? curScope
  Bool inClosure = false         // are we inside a closure's block
  Int scopeLevel = 0
}