//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//   19 Jul 06  Brian Frank  Ported from Java to Fan
//

**
** MethodDef models a method definition - it's signature and body.
**
class MethodDef : SlotDef, Scope
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  new make(Loc loc, TypeDef? parent, Str name := "?", Int flags := 0)
     : super(loc, parent)
  {
    this.name = name
    this.flags = flags
    this.ret = TypeRef.error(loc)
    paramDefs = ParamDef[,]
//    vars = MethodVar[,]
  }
  
  override Scope? parentScope() { parent }
  override Symbol? doFindSymbol(Str name) {
    paramDefs.find { it.name == name }
  }

  **
  ** Does this method contains generic parameters in its signature.
  **
  virtual Bool isGeneric() { calcGeneric(this) }

  **
  ** Is this method the parameterization of a generic method,
  ** with all the generic parameters filled in with real types.
  **
  override Bool isParameterized() { false }

  **
  ** If isParameterized is true, then return the generic
  ** method which this method parameterizes, otherwise null
  **
  virtual MethodDef? generic() { null }

  internal static Bool calcGeneric(MethodDef m)
  {
    if (!m.parentDef.isGeneric) return false
    isGeneric := m.returnType.hasGeneriParamDefeter
    if (isGeneric) return true
    return m.params.any { it.paramType.hasGeneriParamDefeter }
  }

  **
  ** Return a string with the name and parameters.
  **
  Str nameAndParamTypesToStr()
  {
    return name + "(" +
      params.join(", ", |ParamDef p->Str| { p.paramType.signature }) +
      ")"
  }

  ** more loose for ParameterizedType type
  static Bool sameType(TypeRef ai, TypeRef bi) {
    if (ai == bi) return true
    if (ai.isNullable != bi.isNullable)
      return false

    if (ai.signature == bi.signature) return true
    
    /*
    if (ai is GeneriParamDefeter && bi is GeneriParamDefeter) {
      ag := (GeneriParamDefeter)ai
      bg := (GeneriParamDefeter)bi
      return ag.paramName == bg.paramName
    }
    */
    return false
  }

  **
  ** Return if this method has the exact same parameters as
  ** the specified method.
  **
  Bool hasSameParams(MethodDef that)
  {
    a := params
    b := that.params

    if (a.size != b.size)
      return false
    for (i:=0; i<a.size; ++i) {
      if (!sameType(a[i].paramType, b[i].paramType))
        return false
    }
    return true
  }
//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  **
  ** Make and add a MethodVar for a local variable.
  **
  MethodVar addLocalVarForDef(LocalDefStmt def, Block? scope)
  {
    var_v := def.var_v
//    var_v.isCatchVar = def.isCatchVar
    if (scope == null) scope = code
    scope.vars.add(var_v)
    var_v.method = this
    return var_v
  }

  **
  ** Make and add a MethodVar for a local variable.  If name is
  ** null then we auto-generate a temporary variable name
  **
  MethodVar addLocalVar(Loc loc, TypeRef ctype, Str? name, Block? scope)
  {
    // allocate next register index, implicit this always register 0
    reg := code.vars.size
//    if (!isStatic) reg++
    if (scope == null) scope = code

    // auto-generate name
    if (name == null) name = "\$temp" + reg

    // create variable and add it variable list
    var_v := MethodVar(loc, ctype, name)
    var_v.scope = scope
    var_v.method = this;
    scope.vars.add(var_v)
    return var_v
  }

  **
  ** Add a parameter to the end of the method signature and
  ** initialize the param MethodVar.
  ** Note: currently this only works if no locals are defined.
  **
  MethodVar addParamVar(TypeRef ctype, Str name)
  {
    //if (code.vars.size > 0 && !code.vars[code.vars.size-1].isParam) throw Err("Add param with locals $qname")
    param := ParamDef(loc, ctype, name)
    params.add(param)

//    reg := params.size-1
//    if (!isStatic) reg++
//    var_v := MethodVar.makeForParam(this, reg, param, ctype)
//    code.vars.add(var_v)
//    return var_v
    param.method = this
    return param
  }
  
//  MethodVar[] vars() { code.vars }

  **
  ** Why maintain register
  **
  MethodVar[] getAllVars() {
    vars := MethodVar[,]
    vars.addAll(paramDefs)
    
    visitor := BlockVisitor {
      vars.addAll(it.vars)
    }
    code?.walk(visitor, VisitDepth.block)
    
    reg := 0
    if (!isStatic) reg++
    vars.each |v| {
      v.register = reg
      ++reg
    }
    return vars
  }

//////////////////////////////////////////////////////////////////////////
// MethodDef
//////////////////////////////////////////////////////////////////////////

  Str signature() { qname + "(" + params.join(",") + ")" }

  TypeRef returnType() { ret }

  TypeRef inheritedReturnType()
  {
    return ret
  }

  ParamDef[] params() { paramDefs }

//////////////////////////////////////////////////////////////////////////
// Tree
//////////////////////////////////////////////////////////////////////////

  override Void walk(Visitor v, VisitDepth depth)
  {
    v.enterMethodDef(this)
    //walkFacets(v, depth)
    if (depth >= VisitDepth.stmt)
    {
      if (depth >= VisitDepth.expr)
      {
        //if (ctorChain != null) ctorChain = (CallExpr)ctorChain.walk(v)
        paramDefs.each |ParamDef p| { if (p.def != null) p.def = p.def.walk(v) }
      }
      if (code != null) code.walk(v, depth)
    }
    v.visitMethodDef(this)
    v.exitMethodDef(this)
  }
  
  override Void getChildren(Node[] list, [Str:Obj]? options) {
    if (facets != null) {
      list.add(facets)
    }

    if (ret != null) {
      list.add(ret)
    }
    
    paramDefs.each |p| {
      list.add(p)
    }
    
//    if (ctorChain != null) {
//      list.add(ctorChain)
//    }
    
    if (code != null) {
      list.add(code)
    }
  }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  override Void print(AstWriter out)
  {
    super.print(out)
    
    //if (isCtor) out.w("new ")
    //else out.w("fun ")
    
    ret.print(out)
    out.w(" ")
    out.w(name).w("(")
    paramDefs.each |ParamDef p, Int i|
    {
      if (i > 0) out.w(", ")
      p.print(out)
    }
    out.w(")")
    
    out.nl

    if (code != null) code.print(out)
    out.nl
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  TypeRef? ret              // return type
  //TypeRef? inheritedRet    // used for original return if covariant
  ParamDef[] paramDefs   // parameter definitions
  Block? code            // code block
  //CallExpr? ctorChain    // constructor chain for this/super ctor
//  MethodVar[] vars       // all param/local variables in method
  //FieldDef? accessorFor  // if accessor method for field
  Bool usesCvars         // does this method have locals enclosed by closure
  Scope? parent
}