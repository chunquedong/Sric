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
    return sysPod.resolveType(name, true)
  }

  private TypeRef makeTypeRef(Str podName, Str name) {
    t := TypeRef(podName, name)
    resolveTypeRef(t, Loc.makeUnknow)
    return t
  }

  private MethodDef sysMethod(TypeRef t, Str name)
  {
    m := t.slots[name] as MethodDef
    if (m == null) throw Err("Cannot resolve '${t.qname}.$name' method in namespace")
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
  Void addCurPod(Str name, PodDef pod) { podCache[name] = pod }

  **
  ** Subclass hook to resolve a pod name to a PodDef implementation.
  ** Return null if not found.
  **
  protected PodDef? findPod(Str podName) {
    return context.loadDepends(podName)
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
      parent := pod.resolveType(parentName, true)
      gptype := parent.getGeneriParamDefeter(name)
      return typeRef.resolveTo(gptype)
    }
    
    if (recursive && typeRef.genericArgs != null) {
      typeRef.genericArgs.each {
        resolveTypeRef(it, loc)
      }
    }
    typeDef := pod.resolveType(typeName, false)
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
    slot := resolveType(qname[0..<dot]).slot(qname[dot+1..-1])
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

  once TypeRef? objType              () { makeTypeRef("sys", "Obj") }
  once TypeRef? boolType             () { makeTypeRef("sys", "Bool") }
  once TypeRef? enumType             () { makeTypeRef("sys", "Enum") }
  once TypeRef? facetType            () { makeTypeRef("sys", "Facet") }
  once TypeRef? intType              () { makeTypeRef("sys", "int") }
  once TypeRef? floatType            () { makeTypeRef("sys", "Float") }
  once TypeRef? strType              () { makeTypeRef("sys", "Str") }
  once TypeRef? strBufType           () { makeTypeRef("sys", "StrBuf") }
  once TypeRef? listType             () { makeTypeRef("sys", "List") }
  once TypeRef? funcType             () { makeTypeRef("sys", "Func") }
  once TypeRef? errType              () { makeTypeRef("sys", "Err") }
  once TypeRef? typeType             () { makeTypeRef("std", "Type") }
  once TypeRef? ptrType              () { makeTypeRef("sys", "Ptr") }
  once TypeRef? rangeType            () { makeTypeRef("sys", "Range") }
  once TypeRef? voidType             () { makeTypeRef("sys", "void") }
  once TypeRef? fieldNotSetErrType   () { makeTypeRef("sys", "FieldNotSetErr") }
  once TypeRef? notImmutableErrType  () { makeTypeRef("sys", "NotImmutableErr") }
  once TypeRef? thisType             () { makeTypeRef("sys", "This") }

  once TypeRef? decimalType() { makeTypeRef("std", "Decimal") }
  once TypeRef? durationType() { makeTypeRef("std", "Duration") }
  once TypeRef? mapType() { makeTypeRef("std", "Map") }
  once TypeRef? podType() { makeTypeRef("std", "Pod") }
  once TypeRef? slotType() { makeTypeRef("std", "Slot") }
  once TypeRef? fieldType() { makeTypeRef("std", "Field") }
  once TypeRef? methodType() { makeTypeRef("std", "Method") }
  once TypeRef? testType() { makeTypeRef("std", "Test") }
  once TypeRef? uriType() { makeTypeRef("std", "Uri") }
  once TypeRef? asyncType() { makeTypeRef("concurrent", "Async") }
  once TypeRef? promiseType() { makeTypeRef("concurrent", "Promise") }

  once MethodDef? objTrap            () { sysMethod(objType,    "trap") }
  once MethodDef? objWith            () { sysMethod(objType,    "with") }
  once MethodDef? objToImmutable     () { sysMethod(objType,    "toImmutable") }
  once MethodDef? boolNot            () { sysMethod(boolType,    "not") }
  once MethodDef? intIncrement       () { sysMethod(intType,    "increment") }
  once MethodDef? intDecrement       () { sysMethod(intType,    "decrement") }
  once MethodDef? intPlus            () { sysMethod(intType,    "plus") }
  once MethodDef? floatPlus          () { sysMethod(floatType,    "plus") }
  once MethodDef? floatMinus         () { sysMethod(floatType,    "minus") }
  once MethodDef? strPlus            () { sysMethod(strType,    "plus") }
  once MethodDef? strBufMake         () { sysMethod(strBufType,    "make") }
  once MethodDef? strBufAdd          () { sysMethod(strBufType,    "add") }
  once MethodDef? strBufToStr        () { sysMethod(strBufType,    "toStr") }
  once MethodDef? listMake           () { sysMethod(listType,    "make") }
  once MethodDef? listMakeObj        () { sysMethod(listType,    "makeObj") }
  once MethodDef? listAdd            () { sysMethod(listType,    "add") }
  once MethodDef? listToNullable     () { sysMethod(listType,    "toNullable") }
  once MethodDef? mapMake            () { sysMethod(mapType,    "make") }
  once MethodDef? mapSet             () { sysMethod(mapType,    "set") }
  once MethodDef? enumOrdinal        () { sysMethod(enumType,    "ordinal") }
  once MethodDef? funcBind           () { sysMethod(funcType,    "bind") }
  once MethodDef? rangeMakeInclusive () { sysMethod(rangeType,    "makeInclusive") }
  once MethodDef? rangeMakeExclusive () { sysMethod(rangeType,    "makeExclusive") }
  once MethodDef? slotFindMethod     () { sysMethod(slotType,    "findMethod") }
  once MethodDef? slotFindFunc       () { sysMethod(slotType,    "findFunc") }
  once MethodDef? podFind            () { sysMethod(podType,    "find") }
  once MethodDef? podLocale          () { sysMethod(podType,    "locale") }
  once MethodDef? typePod            () { sysMethod(typeType,    "pod") }
  once MethodDef? typeField          () { sysMethod(typeType,    "field") }
  once MethodDef? typeMethod         () { sysMethod(typeType,    "method") }
  once MethodDef? funcCall           () { sysMethod(funcType,    "call") }
  once MethodDef? fieldNotSetErrMake () { sysMethod(fieldNotSetErrType,    "make") }
  once MethodDef? notImmutableErrMake() { sysMethod(notImmutableErrType,    "make") }
  once MethodDef decimalMakeMethod   () { sysMethod(decimalType,  "fromStr") }
  once MethodDef uriMakeMethod       () { sysMethod(uriType,  "fromStr") }
  once MethodDef durationMakeMethod  () { sysMethod(durationType,  "fromTicks") }

}