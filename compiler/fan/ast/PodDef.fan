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
  
  Void updateCompilationUnit(CompilationUnit? unit, CompilationUnit? old, CompilerLog log) {
    if (old != null) {
      cunits.remove(old)
    }
    
    if (unit == null) return
    cunits.add(unit)
  }
  
  **
  ** Return name
  **
  override final Str toStr()
  {
    return name
  }
  
  Symbol? findSymbol(Str name) {
    cunits.eachWhile |unit| {
        unit.doFindSymbol(name)
    }
  }
  
  once DefNode[] defines() {
    list := DefNode[,]
    cunits.each {
        list.addAll(it.defines)
    }
    return list
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
    cunits.each |CompilationUnit unit| { unit.print(out) }
    out.nl
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

//  override CNamespace? ns            // compiler's namespace
  const Str name           // simple pod name
  Str:Str meta := [Str:Str][:]        // pod meta-data props
  //Str:Obj index := [Str:Obj][:]       // pod index props (vals are Str or Str[])
  CompilationUnit[] cunits := [,]           // Tokenize

  Str? summary
}