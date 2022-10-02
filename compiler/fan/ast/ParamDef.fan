//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 Jul 06  Brian Frank  Creation
//

**
** ParamDef models the definition of a method parameter.
**
class ParamDef : MethodVar
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  new make(Loc loc, TypeRef paramType, Str name, Expr? def := null)
    : super(loc, paramType, name)
  {
//    this.paramType = paramType
//    this.name = name
    this.def  = def
    flags = FConst.Param
  }

//////////////////////////////////////////////////////////////////////////
// ParamDef
//////////////////////////////////////////////////////////////////////////

  Bool hasDefault() { def != null }
  **
  ** generic type erasure
  **
  virtual Bool isTypeErasure() {
    return false
  }
//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  override Str toStr()
  {
    return "$paramType $name"
  }

  override Void print(AstWriter out)
  {
    out.w(name).w(" : ").w(paramType)
    if (def != null) { out.w(" = "); def.print(out) }
  }
  
  override ParamDef? paramDef() { this }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  TypeRef paramType() { super.ctype }   // type of parameter
//  override Str name          // local variable name
  Expr? def                  // default expression
  Bool isVarParam = false
}