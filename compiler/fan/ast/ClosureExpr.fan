//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 Jul 06  Brian Frank  Creation
//


class FuncTypeDef : Node {
  
  TypeRef typeRef
  
  new make(Loc loc, TypeRef[] params, Str[] names, TypeRef ret)
   : super(loc)
  {
    typeRef = TypeRef.funcType(loc, params, ret)
    this.params = params
    this.names  = names
    this.ret    = ret
  }
  
  new makeItBlock(Loc loc, TypeRef itType)
    : this.make(loc, [itType], ["it"], TypeRef.voidType(loc))
  {
    // sanity check
    inferredSignature = true
  }
  
  override Void print(AstWriter out)
  {
    out.w(toStr)
  }
  
  override Void getChildren(Node[] list, [Str:Obj]? options) {
    params.eachWhile |e| {
      list.add(e)
    }
    list.add(ret)
  }
  
  override Str toStr() {
    s := StrBuf()
    s.add("|")
    params.size.times |i| {
      if (i > 0) s.add(", ")
      s.add(names.getSafe(i)).add(" : ").add(params[i])
    }
    s.add(" -> ").add(ret).add("|")
    return s.toStr
  }
  
  ParamDef[] toParamDefs()
  {
    p := ParamDef[,]
    p.capacity = params.size
    for (i:=0; i<params.size; ++i)
    {
      p.add(ParamDef(loc, params[i], names.getSafe(i, "\$$i")))
    }
    return p
  }
  
  Int arity() { params.size }
  
  TypeRef[] params // a, b, c ...
  Str[] names    { private set } // parameter names
  TypeRef ret // return type
  Bool unnamed                   // were any names auto-generated
  Bool inferredSignature   // were one or more parameters inferred
}


**************************************************************************
** ClosureExpr
**************************************************************************

**
** ClosureExpr is an "inlined anonymous method" which closes over it's
** lexical scope.  ClosureExpr is placed into the AST by the parser
** with the code field containing the method implementation.  In
** InitClosures we remap a ClosureExpr to an anonymous class TypeDef
** which extends Func.  The function implementation is moved to the
** anonymous class's doCall() method.  However we leave ClosureExpr
** in the AST in it's original location with a substitute expression.
** The substitute expr just creates an instance of the anonymous class.
** But by leaving the ClosureExpr in the tree, we can keep track of
** the original lexical scope of the closure.
**
class ClosureExpr : Expr
{
  new make(Loc loc, TypeDef enclosingType,
           SlotDef enclosingSlot, ClosureExpr? enclosingClosure,
           FuncTypeDef signature, Str name)
    : super(loc, ExprId.closure)
  {
//    this.ctype            = signature
    this.enclosingType    = enclosingType
    this.enclosingSlot    = enclosingSlot
    this.enclosingClosure = enclosingClosure
    this.signature        = signature
    this.name             = name
  }

  //'this' ref field
  once Expr outerThisField()
  {
    if (enclosingSlot.isStatic) throw Err("Internal error: $loc.toLocStr")
    return ThisExpr.makeType(loc, this.enclosingType.asRef)
  }

  override Str toStr()
  {
    return "$signature { ... }"
  }
  
  override Void getChildren(Node[] list, [Str:Obj]? options) {
    list.add(signature)
    if (code != null) {
      list.add(code)
    }
  }

  override Void print(AstWriter out)
  {
    out.w(signature.toStr)
    out.nl
    code.print(out)
  }

  
  // Parse
  TypeDef enclosingType         // enclosing class
  SlotDef enclosingSlot         // enclosing method or field initializer
  ClosureExpr? enclosingClosure // if nested closure
  FuncTypeDef signature         // function signature
  Block? code                   // moved into a MethodDef in InitClosures
  Str name                      // anonymous class name
  Bool isItBlock                // does closure have implicit it scope

  
  // InitClosures
//  Expr? substitute          // expression to substitute during assembly
//  TypeDef? cls                  // anonymous class which implements the closure
//  MethodDef? call               // anonymous class's call() with code
//  MethodDef? doCall             // anonymous class's doCall() with code

  // ResolveExpr
  [Str:MethodVar]? enclosingVars := [:] // my parent methods vars in scope
  //Bool setsConst                 // sets one or more const fields (CheckErrors)
//  TypeRef? itType                  // type of implicit it

  
//  TypeRef? followCtorType          // follow make a new Type
  
}
