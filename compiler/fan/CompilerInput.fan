//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//    2 Jun 06  Brian Frank  Ported from Java to Fan
//
**
** CompilerInput encapsulates all the input needed run the compiler.
** The compiler can be run in one of two modes - file or str.  In
** file mode the source code and resource files are read from the
** file system.  In str mode we compile a single source file from
** an in-memory string.
**
class CompilerInput
{
  **
  ** Output directory to write pod to, defaults to the
  ** current environment's working lib directory
  **
  File outDir := Env.cur.workDir + `lib/fan/`
 
  **
  ** Base directory of source tree - this directory is used to create
  ** the relative paths of the source and resource files in the pod zip.
  **
  File? baseDir
  
  **
  ** List of Fantom source files or directories containing Fantom
  ** source files to compile.  Uris are relative to `baseDir`.  This
  ** field is used only in file mode.
  **
  File[]? srcFiles
  
  **
  ** Fantom source code to compile (str mode only)
  **
  Str? srcStr
  
  **
  ** Location to use for SourceFile facet (str mode only)
  **
  Str? srcStrLoc
  
  **
  ** Include fandoc in output pod, default is false
  **
  Bool includeDoc := true
  
  **
  ** Namespace used to resolve dependency pods/types.
  **
  CNamespace? ns := null
  
  **
  ** empty pod for hold pod config info
  ** 
  PodDef? podDef

}
