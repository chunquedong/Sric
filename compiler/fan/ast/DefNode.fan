//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   10 Nov 05  Brian Frank  Creation
//    3 Jun 06  Brian Frank  Ported from Java to Fantom - Megan's b-day!
//

**
** DefNode is the abstract base class for definition nodes such as TypeDef,
** MethodDef, and FieldDef.  All definitions may be documented using a
** Javadoc style FanDoc comment.
**
abstract class DefNode : Node, Symbol
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  new make(Loc loc)
    : super(loc)
  {
  }

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////
  
  override Void print(AstWriter out) {
    if (doc != null) {
      doc.print(out)
    }
    if (facets != null) {
      facets.print(out)
      out.nl
    }
    
  }
  
  **
  ** Return if this type or slot should be documented:
  **   - public or protected
  **   - not synthentic
  **   - not a subclass of sys::Test
  **
  ** If a public type/slot is annotated with @NoDoc we
  ** we still generate the docs to make it available for
  ** reflection
  **
  Bool isDocumented()
  {
    if (flags.and(FConst.Synthetic) != 0) return false
    if (flags.and(FConst.Public) == 0 && flags.and(FConst.Protected) == 0) return false
    return true
  }


  **
  ** name of parent pod
  ** 
  Str podName() { pod.name }
  abstract Str qname()
  abstract Void walk(Visitor v, VisitDepth depth)
  abstract Str name()
  
  Bool isAbstract()  { flags.and(FConst.Abstract)  != 0 }
  //Bool isAccessor()  { flags.and(FConst.Getter.or(FConst.Setter)) != 0 }
  Bool isConst()     { flags.and(FConst.Const)     != 0 }
  Bool isReadonly()  { flags.and(FConst.Readonly)  != 0 }
  //Bool isCtor()      { flags.and(FConst.Ctor)      != 0 }
  Bool isEnum()      { flags.and(FConst.Enum)      != 0 }
  //Bool isGetter()    { flags.and(FConst.Getter)    != 0 }
  Bool isInternal()  { flags.and(FConst.Internal)  != 0 }
  Bool isNative()    { flags.and(FConst.Native)    != 0 }
  Bool isOverride()  { flags.and(FConst.Override)  != 0 }
  Bool isPrivate()   { flags.and(FConst.Private)   != 0 }
  Bool isProtected() { flags.and(FConst.Protected) != 0 }
  Bool isPublic()    { flags.and(FConst.Public)    != 0 }
  //Bool isSetter()    { flags.and(FConst.Setter)    != 0 }
  Bool isStatic()    { flags.and(FConst.Static)    != 0 }
  //Bool isStorage()   { flags.and(FConst.Storage)   != 0 }
  Bool isSynthetic() { flags.and(FConst.Synthetic) != 0 }
  Bool isVirtual()   { flags.and(FConst.Virtual)   != 0 }
  Bool isOverload()  { flags.and(FConst.Overload)  != 0 }
  Bool isOnce()      { flags.and(FConst.Once)      != 0 }

  //Bool isInstanceCtor() { isCtor && !isStatic }
  //Bool isStaticCtor() { isCtor && isStatic }
  Bool isOperator() { false }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  DocDef? doc         // lines of fandoc comment or null
  Int flags := 0      // type/slot flags
  FacetDef? facets  // facet declarations or null
  
  CompilationUnit? unit      // parent unit
  PodDef? pod                // parent pod
}

**************************************************************************
** DocDef
**************************************************************************

**
** Type or slot documentation in plain text fandoc format
**
class DocDef : Node
{
  new make(Loc loc, Str[] lines)
    : super(loc)
  {
    this.lines = lines
  }

  override Void print(AstWriter out)
  {
    lines.each |line| { out.w("** ").w(line).nl }
  }

  Str[] lines

  override Str toStr() {
    return lines.join("\n")
  }
}

class FacetDef : Node
{
  new make(Loc loc, Str[] lines)
    : super(loc)
  {
    this.lines = lines
  }

  override Void print(AstWriter out)
  {
    lines.each |line| { out.w("** ").w(line).nl }
  }

  Str[] lines

  override Str toStr() {
    return lines.join("\n")
  }
}
