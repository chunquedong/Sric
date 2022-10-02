//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//    3 Jun 06  Brian Frank  Ported from Java to Fantom - Megan's b-day!
//

**
** CompilationUnit models the top level compilation unit of a source file.
**
class CompilationUnit : Node
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  new make(Loc loc, PodDef pod, Str file)
    : super(loc)
  {
    this.pod    = pod
    this.usings = Using[,]
    this.defines  = DefNode[,]
    this.file = file
  }

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  override Void print(AstWriter out)
  {
    out.nl
    usings.each |Using u| { u.print(out) }
    defines.each |DefNode t| { t.print(out) }
  }

  override Str toStr()
  {
    return file + loc.toStr
  }

  // get all imported extendsion methods
  once Str:MethodDef[] extensionMethods() {
    meths := [Str:MethodDef[]][:]
    imported.each |defines|{
      defines.each |define| {
        if (define is MethodDef) {
          try {
            m := define as MethodDef
            if (m.isStatic && (m.flags.and(FConst.Extension) != 0)) {
                 ms := meths[m.name]
                 if (ms == null) { ms = MethodDef[,]; meths[m.name] = ms }
                 ms.add(m)
            }
          } catch (Err e) {
            e.trace
          }
        }
      }
    }
    return meths
  }
  
  Void addDef(DefNode t) {
    defines.add(t)
//    pod.typeDefs[t.name] = t
  }
  
  override Void getChildren(Node[] list, [Str:Obj]? options) {
    usings.each |u| {
      list.add(u)
    }
    
    defines.each |t| {
      list.add(t)
    }
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  PodDef pod                    // ctor
  TokenVal[]? tokens            // Tokenize
  Using[] usings                // ScanForUsingsAndTypes
  DefNode[] defines             // TypeDef and SlotDef
  [Str:DefNode[]]? imported  // ResolveImports (includes my pod)
  Str file
  //Bool isFanx := true           //is fanx syntax
}