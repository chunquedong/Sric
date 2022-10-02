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
  ** code to compile
  **
  CompilationUnit[] cunits
  [Str:CompilationUnit] cunitsMap
  
  **
  ** Log used for reporting compile errors
  **
  CompilerLog log
  
  
  ** temp vars see: ResolveExpr.resolveLocaleLiteral
  LocaleLiteralExpr[] localeDefs
  
  ** ctor
  new make(PodDef pod, CompilerInput input, CNamespace ns) {
    this.pod = pod
    log = CompilerLog()
    localeDefs = LocaleLiteralExpr[,]
    cunitsMap = [:]
    cunits = [,]
    this.input = input
    this.ns = ns
    ns.init(this)
  }
  
  PodDef loadDepends(Str name) {
    file := `./res/${name}.sc`.toFile
    code := file.readAllStr
    
    pod := PodDef(Loc.make(file.osPath), name)
    unit := CompilationUnit(Loc.make(file.osPath), pod, file.toStr)

    parser := DeepParser(log, code, unit)
    //parser.isImport = true
    //echo(parser.tokens.join("\n")|t|{ t.loc.toStr + "\t\t" + t.kind + "\t\t" + t.val })
    parser.parse
    
    pod.updateCompilationUnit(unit, null, log)
    
    ns.addCurPod(name, pod)
    return pod
  }
}
