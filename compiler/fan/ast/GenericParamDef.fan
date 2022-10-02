
**************************************************************************
** GeneriParamDefeterType
**************************************************************************

**
** GeneriParamDefeterType models the generic parameter types
** sys::V, sys::K, etc.
**

class GeneriParamDefDef : TypeDef {
  TypeRef? bound
  TypeDef parent
  Str paramName
  Int index

  new make(Loc loc, Str name, TypeDef parent, Int index, TypeRef? bound := null) : super.make(loc, parent.unit, name)
  {
    this.loc = loc
    this.parent = parent
    this.paramName = name
    this.index = index
    this.name = parent.name+"^"+name
    if (bound == null) bound = TypeRef.objType(loc).toNullable
    this.bound = bound
    this.pod = parent.pod
  }
  
  override Str toStr() {
    s := paramName
    if (bound != null) s += " : " + bound
    return s
  }
  
  override Void print(AstWriter out)
  {
    out.w(paramName)
    if (bound != null) out.w(" : ").w(bound)
  }

}