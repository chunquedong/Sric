//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   26 Dec 05  Brian Frank  Creation
//    5 Jun 06  Brian Frank  Ported from Java to Fan
//

**
** ResolveImports maps every Using node in each CompilationUnit to a pod
** and ensures that it exists and that no imports are duplicated.  Then we
** create a map for all the types which are imported into the CompilationUnit
** so that the Parser can quickly distinguish between a type identifier and
** other identifiers.  The results of this step populate Using.resolvedXXX and
** CompilationUnit.importedTypes.
**
class ResolveImports : CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  **
  ** Constructor takes the associated Compiler
  **
  new make(CompilerContext compiler)
    : super(compiler)
  {
    //resolved[pod.name] = pod
  }

//////////////////////////////////////////////////////////////////////////
// Methods
//////////////////////////////////////////////////////////////////////////

  **
  ** Run the step
  **
  override Void run()
  {
    //debug("ResolveImports")
    
    // process each unit for Import.pod
    curPod.cunits.each |CompilationUnit unit|
    {
      try
        resolveImports(unit)
      catch (CompilerErr e)
        errReport(e)
    }

    // process each unit for CompilationUnit.importedTypes
    curPod.cunits.each |CompilationUnit unit|
    {
      try
        resolveImportedTypes(unit)
      catch (CompilerErr e)
        errReport(e)
    }

  }

  **
  ** Resolve all the imports in the specified unit
  ** and ensure there are no duplicates.
  **
  private Void resolveImports(CompilationUnit unit)
  {
    // map to keep track of duplicate imports
    // within this compilation unit
    dups := [Str:Using][:]

    // process each import statement (remember the
    // first one is an implicit import of sys)
    unit.usings.each |Using u|
    {
      podName := u.podName

      // check that this podName was in the compiler's
      // input dependencies
      checkUsingPod(this.log, this.curPod, podName, u.loc)

      // don't allow a using my own pod
      if (u.typeName == null && u.podName == this.podName) {
        warn("Using '$u.podName' is on pod being compiled", u.loc)
        return
      }

      // check for duplicate imports
      key := podName
      if (u.typeName != null) key += "::$u.typeName"
      if (u.asName != null) key += " as $u.asName"
      if (dups.containsKey(key))
      {
        err("Duplicate using '$key'", u.loc)
        return
      }
      dups[key] = u

      // if already resolved, then just use it
      u.resolvedPod = resolved[podName]

      // resolve the import and cache in resolved map
      if (u.resolvedPod == null)
      {
        try
        {
          pod := ns.resolvePod(podName, u.loc)
          resolved[podName] = u.resolvedPod = pod
        }
        catch (CompilerErr e)
        {
          errReport(e)
          return
        }
        catch {
          return
        }
      }

      // if type specified, then resolve type
      if (u.typeName != null)
      {
        
        typeRef := TypeRef(u.podName, u.typeName)
        try
        {
          ns.resolveTypeRef(typeRef, u.loc)
          if (!typeRef.isResolved)
          {
            err("Type not found in pod '$podName::$u.typeName'", u.loc)
            return
          }
          u.resolvedDefine = typeRef.typeDef
        }
        catch (CompilerErr e) {
          errReport(e)
        }
      }
    }
  }
  
  private DefNode[] defaultImportedTypes(CompilationUnit unit) {
    res := DefNode[,]
    if (podName != "sys") {
      pod := ns.resolvePod("sys", unit.loc)
      //echo("********$pod.defines")
      res.addAll(pod.defines)
    }
    return res
  }

  **
  ** Create a unified map of type names to TypeRef[] for all the
  ** imports in the specified unit (this includes types within
  ** the pod being compilied itself).  For example if foo::Thing
  ** and bar::Thing are imported, then importedTypes would contain
  **   "Thing" : [foo::Thing, bar::Thing]
  **
  private Void resolveImportedTypes(CompilationUnit unit)
  {
    // name -> TypeRef[]
    types := [Str:DefNode[]][:]

    //import system default pod types
    addAll(types, defaultImportedTypes(unit))
    
    // add types for my own pod
    addAll(types, curPod.defines)
    
    // add pod level imports first
    unit.usings.each |Using u|
    {
      if (u.typeName == null && u.resolvedPod != null)
        addAll(types, u.resolvedPod.defines)
    }

    // add type specific imports last (these
    // override any pod level imports)
    unit.usings.each |Using u|
    {
      if (u.typeName != null && u.resolvedDefine != null)
      {
        if (u.asName == null)
        {
          types[u.typeName] = [u.resolvedDefine]
        }
        else
        {
          remove(types, u.resolvedDefine)
          types[u.asName] = [u.resolvedDefine]
        }
      }
    }

//    if (pod.name == "sys") addAll(types, sizedPrimitive.vals)

    /*
    // dump
    echo("--- types for $unit")
    ids := types.keys.sort
    ids.each |Str id| { echo("$id = ${types[id]}") }
    */

    // save away on unit
    unit.imported = types
  }

//  private once Str:TypeRef sizedPrimitive() {
//    intType := ns.intType
//    floatType := ns.floatType
//
//    return [
//     "Int8"  : SizedPrimitiveType(intType, "8"),
//     "Int16" : SizedPrimitiveType(intType, "16"),
//     "Int32" : SizedPrimitiveType(intType, "32"),
//     "Int64" : SizedPrimitiveType(intType, "64"),
//     "Float32" : SizedPrimitiveType(floatType, "32"),
//     "Float64" : SizedPrimitiveType(floatType, "64")
//    ]
//  }

  private Void addAll([Str:DefNode[]] types, DefNode[] toAdd)
  {
    toAdd.each |DefNode t|
    {
      list := types.get(t.name)
      if (list == null) {
        list = DefNode[,]
        types.set(t.name, list)
      }
      //tr := TypeRef.makeRef(t.loc, t.pod.name, t.name)
      //tr.resolveTo(t)
      list.add(t)
    }
  }

  private Void remove([Str:DefNode[]] types, DefNode t)
  {
    list := types[t.name]
    if (list != null)
    {
      for (i:=0; i<list.size; ++i)
        if (list[i].qname == t.qname) { list.removeAt(i); break }
    }
  }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////

  **
  ** Resolve a fully qualified type name into its TypeRef representation.
  ** This may be a TypeDef within the compilation units or could be
  ** an imported type.  If the type name cannot be resolved then we
  ** log an error and return null.
  **
//  static TypeRef? resolveQualified(CompilerContext cs, Str podName, Str typeName, Loc loc)
//  {
//    // first check pod being compiled
//    if (podName == cs.pod.name)
//    {
//      t := cs.pod.resolveType(typeName, false)
//      if (t == null)
//      {
//        cs.errors.err("Type '$typeName' not found within pod being compiled", loc)
//        return null
//      }
//      return t
//    }
//
//    // resolve pod
//    pod := resolvePod(cs, podName, loc)
//    if (pod == null) return null
//
//    // now try to lookup type
//    t := pod.resolveType(typeName, false)
//    if (t == null)
//    {
//      cs.errors.err("Type '$typeName' not found in pod '$podName'", loc);
//      return null
//    }
//
//    return t
//  }

//  **
//  ** Resolve a pod name into its PodDef representation.  If pod
//  ** cannot be resolved then log an error and return null.
//  **
//  static PodDef? resolvePod(CompilerContext cs, Str podName, Loc loc)
//  {
//    // if this is the pod being compiled no further checks needed
//    if (cs.pod.name == podName) return cs.pod
//
//    // otherwise we need to try to resolve pod
//    PodDef? pod := null
//    try
//    {
//      pod = cs.ns.resolvePod(podName, loc)
//    }
//    catch (CompilerErr e)
//    {
//      cs.errors.errReport(e)
//      return null
//    }
//
//    // check that we have a dependency on the pod
//    checkUsingPod(cs, podName, loc)
//
//    return pod
//  }

  **
  ** Check that a pod name is in the dependency list.
  **
  static Void checkUsingPod(CompilerLog log, PodDef curPod, Str podName, Loc loc)
  {
    // scripts don't need dependencies
    //if (cs.input.isScript) return
    if (curPod.resolvedDepends == null) return

    // if we have a declared dependency that is ok
    if (curPod.resolvedDepends.containsKey(podName)) return

    // if this is the pod being compiled that is obviously ok
    if (curPod.name == podName) return

    // we don't require explicit dependencies on FFI
    if (podName.startsWith("[")) return

    // we got a problem
    log.err("Using '$podName' which is not a declared dependency for '$curPod.name'", loc)
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  Str:PodDef resolved := [Str:PodDef][:]  // reuse CPods across units

}