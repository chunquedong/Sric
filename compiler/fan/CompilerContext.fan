//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//    2 Jun 06  Brian Frank  Ported from Java to Fan
//
**
** shared data in compiler step
**
class CompilerContext {
  
  CompilerInput input
  
  **
  ** Namespace used to resolve dependency pods/types.
  **
  CNamespace? ns
  
  **
  ** Current pod
  ** 
  PodDef pod

  **
  ** file to CompilationUnit. for trace recompile result
  **
  [Str:CompilationUnit] cunitsMap
  
  **
  ** Log used for reporting compile errors
  **
  CompilerLog log
  
  ** ctor
  new make(PodDef pod, CompilerInput input, CNamespace ns) {
    this.pod = pod
    log = CompilerLog()
    //localeDefs = LocaleLiteralExpr[,]
    cunitsMap = [:]
    this.input = input
    this.ns = ns
    ns.init(this)
  }
}
