//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//   29 Aug 06  Brian Frank  Ported from Java to Fan
//

**
** CNamespace is responsible for providing a unified view pods, types,
** and slots between the entities currently being compiled and the
** entities being imported from pre-compiled pods.
**
class CNamespace
{
   private CompilerContext? context
//////////////////////////////////////////////////////////////////////////
// Initialization
//////////////////////////////////////////////////////////////////////////

  **
  ** Once the sub class is initialized, it must call this
  ** method to initialize our all predefined values.
  **
  protected Void init(CompilerContext context)
  {
    this.context = context
  }

  private TypeDef sysType(Str name)
  {
    return sysPod.findSymbol(name)
  }

  private TypeRef makeTypeRef(Str podName, Str name) {
    t := TypeRef(podName, name)
    resolveTypeRef(t, Loc.makeUnknow)
    return t
  }

  private MethodDef sysMethod(TypeRef t, Str name)
  {
    m := t.typeDef.findSymbol(name) as MethodDef
    if (m == null) throw Err("Cannot resolve '${t.signature}.$name' method in namespace")
    return m
  }

//////////////////////////////////////////////////////////////////////////
// Resolution
//////////////////////////////////////////////////////////////////////////

  **
  ** Attempt to import the specified pod name against our
  ** dependency library.  If not found then throw CompilerErr.
  **
  PodDef resolvePod(Str podName, Loc? loc)
  {
    // check cache
    pod := podCache[podName]
    if (pod != null) return pod

    // let namespace resolve it
    pod = findPod(podName)
    if (pod == null)
      throw CompilerErr("Pod not found '$podName'", loc)
    
    // stash in the cache and return
    podCache[podName] = pod
    return pod
  }
  private Str:PodDef podCache := [Str:PodDef][:]  // keyed by pod name
  Void addPod(Str name, PodDef pod) { podCache[name] = pod }

  **
  ** Subclass hook to resolve a pod name to a PodDef implementation.
  ** Return null if not found.
  **
  protected PodDef? findPod(Str podName) {
    return IncCompiler.resolveDependPod(podName, this, context.log)
  }

  **
  ** Attempt resolve a signature against our dependency
  ** library.  If not a valid signature or it can't be
  ** resolved, then throw Err.
  **
  TypeRef resolveType(Str sig)
  {
    // check our cache first
    t := typeCache[sig]
    if (t != null) return t

    // parse it into a TypeRef
    t = TypeParser.parse(sig)
    resolveTypeRef(t, null)
    
    if (!t.hasGeneriParamDefeter)
      typeCache[sig] = t
    return t
  }
  internal Str:TypeRef typeCache := [Str:TypeRef][:]   // keyed by signature
  
  Void resolveTypeRef(TypeRef typeRef, Loc? loc, Bool recursive := true) {
    if (typeRef.isResolved) return
    if (typeRef.podName.isEmpty)
      throw Err("Invalid typeRef: $typeRef, ${loc?.toLocStr}")

    //unspoort java FFI
    if (typeRef.podName[0] == '[') {
      typeRef.resolveTo(error.typeDef)
      return
    }
    
    pod := this.resolvePod(typeRef.podName, loc)
    
    //GeneriParamDefeterType
    typeName := typeRef.name
    pos := typeName.index("^")
    if (pos != null) {
      parentName := typeName[0..<pos]
      name := typeName[pos+1..-1]
      parent := pod.findSymbol(parentName) as TypeDef
      gptype := parent.getGeneriParamDefeter(name)
      return typeRef.resolveTo(gptype)
    }
    
    if (recursive && typeRef.genericArgs != null) {
      typeRef.genericArgs.each {
        resolveTypeRef(it, loc)
      }
    }
    typeDef := pod.findSymbol(typeName) as TypeDef
    if (typeDef == null) {
      throw CompilerErr("Type not found '$typeName'", loc)
    }
    typeRef.resolveTo(typeDef)
  }

  **
  ** Attempt resolve a slot against our dependency
  ** library.  If can't be resolved, then throw Err.
  **
  SlotDef resolveSlot(Str qname)
  {
    dot := qname.indexr(".")
    slot := resolveType(qname[0..<dot]).typeDef.findSymbol(qname[dot+1..-1])
    if (slot == null) throw Err("Cannot resolve slot: $qname")
    return slot
  }

//////////////////////////////////////////////////////////////////////////
// Predefined
//////////////////////////////////////////////////////////////////////////

  once PodDef? sysPod() { resolvePod("sys", null) }

  // place holder type used for resolve errors
  once TypeRef? error() { TypeDef.makePlaceHolder("Error").asRef }

  // place holder type used to indicate nothing (like throw expr)
  once TypeRef? nothingType() { TypeDef.makePlaceHolder("Nothing").asRef }

  once TypeRef? objType              () { makeTypeRef("sys", "any") }
  once TypeRef? boolType             () { makeTypeRef("sys", "bool") }
  once TypeRef? intType              () { makeTypeRef("sys", "int") }
  once TypeRef? floatType            () { makeTypeRef("sys", "float") }
  once TypeRef? strType              () { makeTypeRef("sys", "string") }
  once TypeRef? listType             () { makeTypeRef("sys", "array") }
  once TypeRef? funcType             () { makeTypeRef("sys", "function") }
  once TypeRef? errType              () { makeTypeRef("sys", "exception") }
  once TypeRef? pointerType          () { makeTypeRef("sys", "pointer") }
  once TypeRef? voidType             () { makeTypeRef("sys", "void") }

}