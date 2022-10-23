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
class TypeDef : DefNode, Scope
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
    //this.closures    = ClosureExpr[,]
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
    //this.closures    = ClosureExpr[,]
  }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////

  **
  ** Return if this Type is a class (as opposed to enum or mixin)
  **
  Bool isClass() { !isMixin && flags.and(FConst.Enum) == 0 }

  **
  ** Return if this Type is a mixin type and cannot be instantiated.
  **
  Bool isMixin() { flags.and(FConst.Mixin) != 0 }

  **
  ** Return if this Type is final and cannot be subclassed.
  **
  Bool isFinal() { flags.and(FConst.Final) != 0 ||
       (flags.and(FConst.Virtual) == 0  && flags.and(FConst.Abstract) == 0) }

  **
  ** This is the full signature of the type.
  **
  Str signature() { qname }

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
  
  TypeDef instantiateGeneric(TypeRef type) {
    key := type.signature
    val := parameterizedTypeCache[key]
    if (val != null) return val
    typeDef := GenericInstancing().instantiate(this, type)
    val = typeDef
    parameterizedTypeCache[key] = val
    return val
  }
  
  private once [Str:TypeDef] parameterizedTypeCache() { [Str:TypeDef][:] }
  
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
  
  
  override Scope? parentScope() { unit }
  override Symbol? doFindSymbol(Str name) {
    if (symbolTable == null) {
        symbolTable = [:]
        slotDefList.each { symbolTable[it.name] = it }
    }
    sym := symbolTable[name]
    if (sym == null) {
        return inheritances.eachWhile { typeDef.doFindSymbol(name) }
    }
    return sym
  }

//////////////////////////////////////////////////////////////////////////
// Slots
//////////////////////////////////////////////////////////////////////////

  **
  ** Add a slot to the type definition.  The method is used to add
  ** SlotDefs declared by this type as well as slots inherited by
  ** this type.
  **
  Void addSlot(SlotDef def, Int? slotDefIndex := null)
  {
      if (slotDefIndex == null)
        slotDefList.add(def)
      else
        slotDefList.insert(slotDefIndex, def)
  }
  
  SlotDef[] slotDefs() { slotDefList }
  
  FieldDef[] fieldDefs() {
    slotDefList.findAll { it is FieldDef }
  }
  
  SlotDef? slot(Str name) {
    slotDefList.find { it.name == name }
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
    
    if (isMixin)
      out.w("class $name")
    else if (isEnum)
      out.w("enum $name")
    else
      out.w("struct $name")

    if (!inheritances.isEmpty) out.w(" : ").w(inheritances.join(", "))

    out.w("{").nl
    out.indent
    enumDefs.each |EnumDef e| { e.print(out) }
    slotDefs.each |SlotDef s| { s.print(out) }
    out.unindent
    out.w("}").nl
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
  //Bool baseSpecified := true       // was base assigned from source code
//  TypeRef? base             // extends class
//  TypeRef[] mixins          // mixin types
  
  virtual TypeRef[] inheritances
  
  EnumDef[] enumDefs               // declared enumerated pairs (only if enum)
  //ClosureExpr[] closures           // closures where I am enclosing type (Parse)
  //ClosureExpr? closure             // if I am a closure anonymous class
  private SlotDef[] slotDefList    // declared slot definitions
  private [Str:Symbol]? symbolTable
  
}