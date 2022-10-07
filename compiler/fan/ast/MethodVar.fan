//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Feb 06  Brian Frank  Creation
//   11 Sep 06  Brian Frank  Ported from Java to Fan
//

**
** MethodVar is a variable used in a method - either param or local.
**
class MethodVar : Node, Symbol
{

  new make(Loc loc, TypeRef? ctype, Str name) : super(loc)
  {
//    this.method   = method
    this.register = -1
    this.ctype    = ctype
    this.name     = name
//    this.flags    = flags
//    this.scope    = scope
  }
  
  override Void print(AstWriter out) {
    if (ctype != null) {
        ctype.print(out)
    }
    else out.w("auto")
    out.w(" ").w(name)
  }

//  new makeForParam(MethodDef method, Int register, ParamDef p, TypeRef paramType)
//    : this.make(p.loc, paramType, p.name)
//  {
//    this.method = method
//    this.register = register
//    this.flags = FConst.Param
//    this.paramDef = p
//    if (p.def != null) this.flags = this.flags.or(FConst.ParamDefault)
//  }

  Bool isParam() { flags.and(FConst.Param) != 0 }

  //Bool isWrapped() { wrapField != null }

  override Str toStr() { "$register  $name: $ctype" }

  Void reassigned()
  {
    isReassigned = true
    if (shadows != null) shadows.reassigned
  }
  
  override Void getChildren(Node[] list, [Str:Obj]? options) {
    if (ctype != null) list.add(ctype)
  }

  MethodDef? method    // declared method (doCall if declared in closure)
  Int register        // register number
  TypeRef? ctype         // variable type
  Str name            // variable name
  Int flags           // Param
  Bool isCatchVar     // is this auto-generated var for "catch (Err x)"
  Block? scope        // block which scopes this variable
  
  // if param
  virtual ParamDef? paramDef() { null }
  
  //used by closure
  Bool usedInClosure  // local used by closure within containing method
  MethodVar? shadows  // if closure var, this is the variable in parent scope we shadow
  //FieldDef? wrapField   // if wrapped onto heap this is 'Wrapper.val' field
  Bool isReassigned   // keeps track of reassigment assignment (we don't count initial local assign)
  //MethodVar? paramWrapper  // wrapper local var if param has to be wrapped
  Int scopeLevel = 0
}