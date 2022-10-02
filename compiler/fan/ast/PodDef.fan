//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   5 Jun 06  Brian Frank  Creation
//

**
** PodDef models the pod being compiled.
**
class PodDef : Node
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  new make(Loc loc, Str name)
    : super(loc)
  {
    this.name = name
    this.defineDefs = [:]
  }

//////////////////////////////////////////////////////////////////////////
// PodDef
//////////////////////////////////////////////////////////////////////////

  Version version = Version("1.0")

  Depend[] depends := [,]
  
  **
  ** Map of dependencies keyed by pod name set in ResolveDepends.
  **
  [Str:PodDef]? resolvedDepends

  TypeDef? resolveType(Str name, Bool checked)
  {
    t := defineDefs[name]
    if (t != null && t is TypeDef) return t
    if (checked) throw UnknownTypeErr("${this.name}::${name}")
    return null
  }
  
  **
  ** Add a synthetic type
  **
  Void addDef(DefNode t)
  {
    t.unit.addDef(t)
    this.defineDefs.add(t.name, t)
    this.defines.add(t)
  }
  
  Void updateCompilationUnit(CompilationUnit? unit, CompilationUnit? old, CompilerLog log) {
    if (old != null) {
      old.defines.each |t| {
        this.defineDefs.remove(t.name)
        this.defines.remove(t)
        //this.closures.removeAll(t.closures)
      }
    }
    
    if (unit == null) return
    unit.defines.each |t| {
      if (this.defineDefs.containsKey(t.name)) {
        log.err("Duplicate type name '$t.name'", unit.loc)
      }
      this.defineDefs[t.name] = t
      this.defines.add(t)
      //this.closures.addAll(t.closures)
    }
  }
  
  **
  ** Return name
  **
  override final Str toStr()
  {
    return name
  }

//////////////////////////////////////////////////////////////////////////
// Tree
//////////////////////////////////////////////////////////////////////////

  override Void print(AstWriter out)
  {
    out.nl
    out.w("//======================================").nl
    out.w("// pod $name").nl
    out.w("//======================================").nl
    //units.each |CompilationUnit unit| { unit.print(out) }
    defines.each |t| { t.print(out) }
    out.nl
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

//  override CNamespace? ns            // compiler's namespace
  const Str name           // simple pod name
  Str:Str meta := [Str:Str][:]        // pod meta-data props
  //Str:Obj index := [Str:Obj][:]       // pod index props (vals are Str or Str[])
  //[Str:CompilationUnit] units := [:]           // Tokenize
  [Str:DefNode] defineDefs           // ScanForUsingsAndTypes
  //ClosureExpr[]? closures := [,]           // Parse
//  TypeDef[]? orderedTypeDefs
  DefNode[] defines := [,] { private set }

  Str? summary
}