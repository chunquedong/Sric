//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//    3 Jun 06  Brian Frank  Ported from Java to Fantom - Megan's b-day!
//


**
** TypeDef models a type definition for a class, mixin or enum
**
class TypeDef : DefNode, TypeMixin
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  new make(Loc loc, CompilationUnit unit, Str name, Int flags := 0) : super.make(loc)
  {
    this.loc = loc
//    this.ns          = ns
    this.pod         = unit.pod
    this.unit        = unit
    this.name        = name
    this.qname       = pod.name + "::" + name
    this.flags       = flags
    //this.isVal       = TypeRef.isValType(qname)
    this.inheritances  = TypeRef[,]
    this.enumDefs    = EnumDef[,]
//    this.slotMap     = Str:SlotDef[:]
//    this.slotDefMap  = Str:SlotDef[:]
    this.slotDefList = SlotDef[,]
    this.closures    = ClosureExpr[,]
  }
  
  new makePlaceHolder(Str name)  : super.make(loc) {
    this.loc = Loc.invalidLoc
//    this.ns          = ns
    this.pod         = PodDef(this.loc, "sys")
    this.unit        = null
    this.name        = name
    this.qname       = pod.name + "::" + name
    this.flags       = 0
    //this.isVal       = TypeRef.isValType(qname)
    this.inheritances  = TypeRef[,]
    this.enumDefs    = EnumDef[,]
//    this.slotMap     = Str:SlotDef[:]
//    this.slotDefMap  = Str:SlotDef[:]
    this.slotDefList = SlotDef[,]
    this.closures    = ClosureExpr[,]
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  **
  ** Return if this type is the anonymous class of a closure
  **
  Bool isClosure()
  {
    return closure != null
  }

  virtual Str extName() { "" }

  **
  ** This is the full signature of the type.
  **
  Str signature() { "$qname$extName" }

  **
  ** Return signature
  **
  override Str toStr() { signature }
  
  once TypeRef asRef() {
    tr := TypeRef.makeRef(loc, pod.name, name)
//    if (this.isGeneric) {
//      if (this.qname != "sys::Func") {
//        tr.genericArgs = [,]
//        this.generiParamDefeters.each |p| {
//          tr.genericArgs.add(p.bound)
//        }
//      }
//    }

    tr.resolveTo(this, false)
    return tr
  }
  
  virtual Bool isVal() {
    return flags.and(FConst.Struct) != 0
  }
  
//////////////////////////////////////////////////////////////////////////
// Generics
//////////////////////////////////////////////////////////////////////////

  **
  ** A generic type means that one or more of my slots contain signatures
  ** using a generic parameter (such as V or K).  Fantom supports three built-in
  ** generic types: List, Map, and Func.  A generic instance (such as Str[])
  ** is NOT a generic type (all of its generic parameters have been filled in).
  ** User defined generic types are not supported in Fan.
  **
  virtual Bool isGeneric() { generiParamDefeters.size > 0 }
  
  **
  ** find GeneriParamDefeter by name
  **
  virtual GeneriParamDefDef? getGeneriParamDefeter(Str name) {
    ps := generiParamDefeters
    return ps.find { it.paramName == name }
  }
  
  internal once Str:TypeDef parameterizedTypeCache() { [Str:TypeDef][:] }
  
//////////////////////////////////////////////////////////////////////////
// Data
//////////////////////////////////////////////////////////////////////////
  
  **
  ** The direct super class of this type (null for Obj).
  **
  virtual TypeRef? base() {
    ihs := inheritances
    if (ihs.size > 0 && ihs.first.isClass) return ihs.first
    return null
  }

  **
  ** Return the mixins directly implemented by this type.
  **
  virtual TypeRef[] mixins() {
    ihs := inheritances
    if (ihs.size > 0 && ihs.first.isClass) {
      return ihs[1..-1]
    }
    return ihs
  }
  
  **
  ** Get operators lookup structure
  **
  once COperators operators() { COperators(this) }
  
//////////////////////////////////////////////////////////////////////////
// Slots
//////////////////////////////////////////////////////////////////////////
  
  protected [Str:SlotDef]? slotDefMapCache
  protected [Str:SlotDef] slotDefMap() {
    if (slotDefMapCache != null) return slotDefMapCache
    map := [Str:SlotDef][:]
    slotDefs.each |s|{
      if (s.isOverload) return
      map[s.name] = s
    }
    slotDefMapCache = map
    return slotDefMapCache
  }
  
  **
  ** Return if this class has a slot definition for specified name.
  **
  Bool hasSlotDef(Str name)
  {
    return slotDefMap.containsKey(name)
  }

  **
  ** Return SlotDef for specified name or null.
  **
  SlotDef? slotDef(Str name)
  {
    return slotDefMap[name]
  }

  **
  ** Return FieldDef for specified name or null.
  **
  FieldDef? fieldDef (Str name)
  {
    return slotDefMap[name] as FieldDef
  }

  **
  ** Return MethodDef for specified name or null.
  **
  MethodDef? methodDef(Str name)
  {
    return slotDefMap[name] as MethodDef
  }

  **
  ** Get the FieldDefs declared within this TypeDef.
  **
  FieldDef[] fieldDefs()
  {
    return (FieldDef[])slotDefs.findType(FieldDef#)
  }

  **
  ** Get the static FieldDefs declared within this TypeDef.
  **
  FieldDef[] statiFieldDefDefs()
  {
    return fieldDefs.findAll |FieldDef f->Bool| { f.isStatic }
  }

  **
  ** Get the instance FieldDefs declared within this TypeDef.
  **
  FieldDef[] instanceFieldDefs()
  {
    return fieldDefs.findAll |FieldDef f->Bool| { !f.isStatic }
  }

  **
  ** Get the MethodDefs declared within this TypeDef.
  **
  MethodDef[] methodDefs()
  {
    return (MethodDef[])slotDefs.findType(MethodDef#)
  }

  **
  ** Get the constructor MethodDefs declared within this TypeDef.
  **
//  MethodDef[] ctorDefs()
//  {
//    return methodDefs.findAll |MethodDef m->Bool| { m.isCtor }
//  }
  
  **
  ** Map of the all defined slots, both fields and
  ** methods (including inherited slots).
  **
  private [Str:SlotDef]? slotsMapCache
  
  virtual Str:SlotDef slots() {
    if (slotsMapCache != null) return slotsMapCache
    
    [Str:SlotDef] slotsMap := OrderedMap(64)
    slotsMapCache = slotsMap
    
    slotDefs.each |s| {
      if (s.isOverload) return
      slotsMap[s.name] = s
    }
    
    this.inheritances.each |t| {
      inherit(slotsMap, this, t)
    }
    
    return slotsMapCache
  }
  
  **
  ** Lookup a slot by name.  If the slot doesn't exist then return null.
  **
  virtual SlotDef? slot(Str name) { slots[name] }
  
  private static Void inherit([Str:SlotDef] slotsCached, TypeDef def, TypeRef inheritance)
  {
    closure := |SlotDef newSlot|
    {
      //already inherit by base
      if (inheritance.isMixin && newSlot.parent.isObj) return
      
      // we never inherit constructors, private slots,
      // or internal slots outside of the pod
      if (newSlot.isPrivate || newSlot.isStatic ||
          (newSlot.isInternal && newSlot.parent.podName != inheritance.typeDef.podName))
        return
      
      oldSlot := slotsCached[newSlot.name]
      if (oldSlot != null) {
        // if we've inherited the exact same slot from two different
        // class hiearchies, then no need to continue
        if (newSlot === oldSlot) return
        
        // if this is one of the type's slot definitions
        if (oldSlot.parent === def) return
        
        kp := keep(oldSlot, newSlot)
        if (kp != newSlot) return
      }

      // inherit it
      slotsCached[newSlot.name] = newSlot
    }
    //inheritance.slots.vals.sort(|SlotDef a, SlotDef b->Int| {return a.name <=> b.name}).each(closure)
    inheritance.slots.each(closure)
  }
  
  **
  ** Return if there is a clear keeper between a and b - if so
  ** return the one to keep otherwise return null.
  **
  static SlotDef? keep(SlotDef a, SlotDef b)
  {
    // if one is abstract and one concrete we keep the concrete one
    if (a.isAbstract && !b.isAbstract) return b
    if (!a.isAbstract && b.isAbstract) return a

    // keep one if it is a clear override from the other
    if (a.parent.asRef.fits(b.parent.asRef)) return a
    if (b.parent.asRef.fits(a.parent.asRef)) return b

    return null
  }


//////////////////////////////////////////////////////////////////////////
// Slots
//////////////////////////////////////////////////////////////////////////

  **
  ** Add a slot to the type definition.  The method is used to add
  ** SlotDefs declared by this type as well as slots inherited by
  ** this type.
  **
  Void addSlot(SlotDef s, Int? slotDefIndex := null)
  {
    // if MethodDef
    m := s as MethodDef
    if (m != null)
    {
      // static initializes are just temporarily added to the
      // slotDefList but never into the name map - we just need
      // to keep them in declared order until they get collapsed
      // and removed in the Normalize step
//      if (m.isStaticInit)
//      {
//        slotDefList.add(m)
//        return
//      }

      // field accessors are added only to slotDefList,
      // name lookup is always the field itself
//      if (m.isFieldAccessor)
//      {
//        slotDefList.add(m)
//        return
//      }

      if (m.isOverload) {
        slotDefList.add(m)
        return
      }
    }

    // sanity check
    name := s.name
//    if (slotDefMap.containsKey(name))
//      throw Err("Internal error: duplicate slot $name [$loc.toLocStr]")

    // if my own SlotDef
    def := s as SlotDef
    if (def != null && def.parent === this)
    {
      // add to my slot definitions
      if (slotDefMapCache != null)
        slotDefMapCache[name] = def
      
      if (slotDefIndex == null)
        slotDefList.add(def)
      else
        slotDefList.insert(slotDefIndex, def)

      // if non-const FieldDef, then add getter/setter methods
//      if (s is FieldDef)
//      {
//        f := (FieldDef)s
//        if (f.get != null) addSlot(f.get)
//        if (f.set != null) addSlot(f.set)
//      }
    }
  }

//  **
//  ** Replace oldSlot with newSlot in my slot tables.
//  **
//  Void replaceSlot(SlotDef oldSlot, SlotDef newSlot)
//  {
//    // sanity checks
//    if (oldSlot.name != newSlot.name)
//      throw Err("Internal error: not same names: $oldSlot != $newSlot [$loc.toLocStr]")
//    if (slotMap[oldSlot.name] !== oldSlot)
//      throw Err("Internal error: old slot not mapped: $oldSlot [$loc.toLocStr]")
//
//    // remap in slotMap table
//    name := oldSlot.name
//    slotMap[name] = newSlot
//
//    // if old is SlotDef
//    oldDef := oldSlot as SlotDef
//    if (oldDef != null && oldDef.parent === this)
//    {
//      slotDefMap[name] = oldDef
//      slotDefList.remove(oldDef)
//    }
//
//    // if new is SlotDef
//    newDef := newSlot as SlotDef
//    if (newDef != null && newDef.parent === this)
//    {
//      slotDefMap[name] = newDef
//      slotDefList.add(newDef)
//    }
//  }

//////////////////////////////////////////////////////////////////////////
// SlotDefs
//////////////////////////////////////////////////////////////////////////

  **
  ** Get the SlotDefs declared within this TypeDef.
  **
  SlotDef[] slotDefs()
  {
    return slotDefList
  }

//////////////////////////////////////////////////////////////////////////
// Enum
//////////////////////////////////////////////////////////////////////////

  **
  ** Return EnumDef for specified name or null.
  **
  public EnumDef? enumDef(Str name)
  {
    return enumDefs.find |EnumDef def->Bool| { def.name == name }
  }

//////////////////////////////////////////////////////////////////////////
// Tree
//////////////////////////////////////////////////////////////////////////

  override Void walk(Visitor v, VisitDepth depth)
  {
    v.enterTypeDef(this)
    //walkFacets(v, depth)
    if (depth >= VisitDepth.slotDef)
    {
      slotDefs.each |SlotDef slot| { slot.walk(v, depth) }
    }
    v.visitTypeDef(this)
    v.exitTypeDef(this)
  }
  
  override Void getChildren(Node[] list, [Str:Obj]? options) {
    if (facets != null) {
      list.add(facets)
    }
    
    this.inheritances.each |t| {
      list.add(t)
    }
    
    slotDefs.each |slot| {
      if (slot.isSynthetic) return
      list.add(slot)
    }
  }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  override Void print(AstWriter out)
  {
    super.print(out)
    //out.flags(flags, false)
    
    if (isMixin)
      out.w("class $name")
    else if (isEnum)
      out.w("enum $name")
    else
      out.w("struct $name")

//    if (base != null || !mixins.isEmpty)
//    {
//      out.w(" : ")
//      if (base != null) out.w(" $base")
//      if (!mixins.isEmpty) out.w(", ").w(mixins.join(", ")).nl
//    }
    if (!inheritances.isEmpty) out.w(" : ").w(inheritances.join(", ")).nl
    else out.nl

    out.w("{").nl
    out.indent
    enumDefs.each |EnumDef e| { e.print(out) }
    slotDefs.each |SlotDef s| { s.print(out) }
    out.unindent
    out.w("};").nl
  }
  
  GeneriParamDefDef[] generiParamDefeters := [,]
  
  Void setBase(TypeRef base) {
    if (inheritances.size > 0) inheritances[0] = base
    else inheritances.add(base)
  }
  
  Bool isNoDoc() { false }
  
//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

//  override CNamespace ns         // compiler's namespace
  override const Str name          // simple class name
  override const Str qname         // podName::name
  //override const Bool isVal        // is this a value type (Bool, Int, etc)
  Bool baseSpecified := true       // was base assigned from source code
//  TypeRef? base             // extends class
//  TypeRef[] mixins          // mixin types
  
  virtual TypeRef[] inheritances
  
  EnumDef[] enumDefs               // declared enumerated pairs (only if enum)
  ClosureExpr[] closures           // closures where I am enclosing type (Parse)
  ClosureExpr? closure             // if I am a closure anonymous class
  private SlotDef[] slotDefList    // declared slot definitions
  FacetDef[]? indexedFacets        // used by WritePod
}