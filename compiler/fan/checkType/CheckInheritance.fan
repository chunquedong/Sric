//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//    2 Dec 05  Brian Frank  Creation (originally InitShimSlots)
//   23 Sep 06  Brian Frank  Ported from Java to Fan
//

**
** CheckInheritance is used to check invalid extends or mixins.
**
class CheckInheritance : CompilerStep
{

//////////////////////////////////////////////////////////////////////////
// Constructor
//////////////////////////////////////////////////////////////////////////

  new make(CompilerContext compiler)
    : super(compiler)
  {
  }

//////////////////////////////////////////////////////////////////////////
// Run
//////////////////////////////////////////////////////////////////////////

  override Void run()
  {
    //debug("CheckInheritance")
    walkUnits(VisitDepth.typeDef)
  }

  override Void visitTypeDef(TypeDef t)
  {
    checkInheritSlot(t)
    
    // check out of order base vs mixins first
    if (!checkOutOfOrder(t)) return

    // check extends
    checkExtends(t, t.base)

    // check each mixin
    t.mixins.each |TypeRef m| { checkMixin(t, m) }
    
    
    checkCyclicInheritance(t)
  }

//////////////////////////////////////////////////////////////////////////
// Checks
//////////////////////////////////////////////////////////////////////////

  private Bool checkOutOfOrder(TypeDef t)
  {

      cls := t.mixins.find |TypeRef x->Bool| { !x.isMixin }
      if (cls != null)
      {
        err("Invalid inheritance order, ensure class '$cls' comes first before mixins", t.loc)
        return false
      }
    
    return true
  }

  private Void checkExtends(TypeDef t, TypeRef? base)
  {
    // base is null only for sys::Obj
    if (base == null)
      return

    // ensure facet doesn't extend class
    //if (t.isFacet && t.baseSpecified)
    //  err("Facet '$t.name' cannot extend class '$base'", t.loc)

    // check extends a mixin
    if (base.isMixin)
      err("Class '$t.name' cannot extend mixin '$base'", t.loc)

    // check extends parameterized type
    if (!base.isParameterized && base.typeDef.isGeneric)
      err("Class '$t.name' cannot extend generic type '$base'", t.loc)

    // check extends final
    if (base.isFinal)
      err("Class '$t.name' cannot extend final class '$base'", t.loc)

    // check extends internal scoped outside my pod
    if (base.isInternal && t.podName != base.podName)
      err("Class '$t.name' cannot access internal scoped class '$base'", t.loc)
  }

  private Void checkMixin(TypeDef t, TypeRef m)
  {
    // check mixins a class
    if (!m.isMixin)
    {
      if (t.isMixin)
        err("Mixin '$t.name' cannot extend class '$m'", t.loc)
      else
        err("Class '$t.name' cannot mixin class '$m'", t.loc)
    }

    // check extends internal scoped outside my pod
    if (m.isInternal && t.podName != m.podName)
      err("Type '$t.name' cannot access internal scoped mixin '$m'", t.loc)
  }
  
  private Void checkCyclicInheritance(TypeDef t) {
    allInheritances := [Str:TypeRef][:]
    getInheritances(allInheritances, t.asRef())
    if (allInheritances.containsKey(t.qname)) {
      err("Cyclic inheritance for '$t.name'", t.loc)
      t.inheritances.clear
      t.inheritances.add(ns.objType)
    }
  }
  
  private Void getInheritances([Str:TypeRef] acc, TypeRef t) {
    t.inheritances.each |p| {
      pt := acc[p.qname]
      if (pt == null) {
        acc[p.qname] = p
        getInheritances(acc, p)
      }
    }
  }




  private Void checkInheritSlot(TypeDef t)
  {
    inheriSlots := [Str:SlotDef[]][:]
    t.inheritances.each |bt| {
      bt.typeDef.slotDefs.each |v| {
        k := v.name
        if (v.isOverload || v.isStatic) return
        list := inheriSlots[k]
        if (list == null) {
          inheriSlots[k] = [v]
          return
        }
        if (list.contains(v)) return
        
        list.add(v)
      }
    }
    
    // check overrides all overrode something
    t.slotDefs.each |SlotDef slot|
    {
      if (slot.isOverload) return
      
      dupDef := t.slot(slot.name)
      if (dupDef !== slot) {
        f := slot as MethodDef
        err("Duplicate slot name '$slot.name'", slot.loc)
      }
      parentSlots := inheriSlots[slot.name]
      if (parentSlots == null) {
        if (slot.isOverride)
          err("Invalid override", slot.loc)
        return
      }
      
      parentSlot := parentSlots.first
      if (!slot.isOverride) {
        checkNameConfilic(t, parentSlot)
        return
      }
      
      parentSlots.each |oldSlot| {
        checkOverride(t, oldSlot, slot)
      }
    }
    
    inheriSlots.each |v,k| {
      
      if (t.slot(k) != null) return
      
      impCounst := 0
      v.each { if (!it.isAbstract) ++impCounst }
    
      if (impCounst > 1) {
        if (t.slot(k) == null) err("Must override ambiguous inheritance '${v[0].qname}' and '${v[1].qname}'", t.loc)
      }
      else if (impCounst == 0 && !t.isAbstract) {
        err("Must override abstract slot '${v[0].qname}'", t.loc)
      }
    
      for (i:=0; i<v.size-1; ++i) {
        if (!matchingSignatures(v[i], v[i+1])) {
          err("Inherited slots have conflicting signatures '${v[i].qname}' and '${v[i+1].qname}'", t.loc)
          break
        }
      }
    }
  }

//////////////////////////////////////////////////////////////////////////
// Inherit
//////////////////////////////////////////////////////////////////////////
  
  
  //do not allow same name slot for invokevirtual on private methods
  private Void checkNameConfilic(TypeDef t, SlotDef parentSlot) {
    //if (parentSlot.isCtor) return
    if ((parentSlot.isPrivate && parentSlot.parentDef.podName == t.podName) ||
         (parentSlot.isInternal && parentSlot.parentDef.podName != t.podName)) {
      name := parentSlot.name
      selfSlot := t.slot(name)
      if (selfSlot != null) {
        loc := t.loc
        if (selfSlot is SlotDef) {
          loc = ((SlotDef)selfSlot).loc
        }
        err("Can not override private $parentSlot with $selfSlot", loc)
      }
    }
  }

  **
  ** Return if two slots have matching signatures
  **
  private Bool matchingSignatures(SlotDef a, SlotDef b)
  {
    fa := a as FieldDef
    fb := b as FieldDef
    ma := a as MethodDef
    mb := b as MethodDef

    if (fa != null && fb != null)
      return fa.fieldType == fb.fieldType

    if (ma != null && mb != null)
      return ma.returnType == mb.returnType &&
             ma.inheritedReturnType == mb.inheritedReturnType &&
             ma.hasSameParams(mb)

    if (fa != null && mb != null)
      return fa.fieldType == mb.returnType &&
             fa.fieldType == mb.inheritedReturnType &&
             mb.params.size == 0

    if (ma != null && fb != null)
      return ma.returnType == fb.fieldType &&
             ma.inheritedReturnType == fb.fieldType &&
             ma.params.size == 0

    return false
  }

  **
  ** Check that def is a valid override of the base slot.
  **
  private Void checkOverride(TypeDef t, SlotDef base, SlotDef def)
  {
    loc := def.loc

    // check base is virtual
    if (!base.isVirtual)
      err("Cannot override non-virtual slot '$base.qname'", loc)

    // check override keyword was specified
    if (!def.isOverride)
      err("Must specify override keyword to override '$base.qname'", loc)

    // check protection scope
    if (isOverrideProtectionErr(base, def))
      err("Override narrows protection scope of '$base.qname'", loc)

    // check if this is a method/method override
    if (base is MethodDef && def is MethodDef)
    {
      checkMethodMethodOverride(t, (MethodDef)base, (MethodDef)def)
      return
    }

    // check if this is a method/field override
    if (base is MethodDef && def is FieldDef)
    {
      checkMethodFieldOverride(t, (MethodDef)base, (FieldDef)def)
      return
    }

    // check if this is a field/field override
    if (base is FieldDef && def is FieldDef)
    {
      checkFieldFieldOverride(t, (FieldDef)base, (FieldDef)def)
      return
    }

    // TODO otherwise this is a potential inheritance conflict
    err("Invalid slot override of '$base.qname'", def.loc)
  }

  private Bool isOverrideProtectionErr(SlotDef base, SlotDef def)
  {
    if (def.isPublic)
      return false

    if (def.isProtected)
      return base.isPublic || base.isInternal

    if (def.isInternal)
      return base.isPublic || base.isProtected

    return true
  }

  private Void checkMethodMethodOverride(TypeDef t, MethodDef base, MethodDef def)
  {
    //skip check for Closure class call.
    // the isParameterized is Void call(Int a); call is Obj? call(Obj?,Obj?,Obj?,Obj?,Obj?,...)
//    if (t.isClosure && def.name == "call" && base.isParameterized) {
//        return
//    }
    
    loc := def.loc

    defRet := def.returnType
    baseRet := base.returnType

    // if the base is defined as This, then all overrides must be This
    
      // check return types
      if (defRet != baseRet)
      {
        // check if new return type is a subtype of original
        // return type (we allow covariant return types)
        if (!defRet.fits(baseRet) || (defRet.isVoid && !baseRet.isVoid) || defRet.isNullable != baseRet.isNullable)
          err("Return type mismatch in override of '$base.qname' - '$baseRet' != '$defRet', type:$t", loc)

        // can't use covariance with value types
        //TODO check
        //if (defRet.isVal || baseRet.isVal)
        //  err("Cannot use covariance with value types '$base.qname' - '$baseRet' != '$defRet'", loc)
      }


    // check that we have same parameter count
    if (!base.hasSameParams(def)) {
      err("Parameter mismatch in override of '$base.qname' - '$base.nameAndParamTypesToStr' != '$def.nameAndParamTypesToStr'", loc)
      return
    }
    
    // check override has matching defaults
    base.params.each |b, i|
    {
      d := def.params[i]
      if (b.hasDefault == d.hasDefault) return
      if (d.hasDefault)
        err("Parameter '$d.name' must not have default to match overridden method", loc)
      else
        err("Parameter '$d.name' must have default to match overridden method", loc)
    }

    // correct override
    return
  }

  private Void checkMethodFieldOverride(TypeDef t, MethodDef base, FieldDef def)
  {
    loc := def.loc

    // check that types match
    ft := def.fieldType
    rt := base.returnType
    if (ft != rt)
    {
      // we allow field to be covariant typed
      if (!ft.fits(rt) || ft.isNullable != rt.isNullable)
        err("Type mismatch in override of '$base.qname' - '$rt' != '$ft'", loc)
    }

    // check that field isn't static
    if (def.isStatic)
      err("Cannot override virtual method with static field '$def.name'", loc)

    // check that method has no parameters
    if (!base.params.isEmpty)
      err("Field '$def.name' cannot override method with params '$base.qname'", loc)

    // save original return type
    //def.inheritedRet = base.inheritedReturnType

    // correct override
    return
  }

  private Void checkFieldFieldOverride(TypeDef t, FieldDef base, FieldDef def)
  {
    loc := def.loc

    // check that types match
    if (!MethodDef.sameType(base.fieldType, def.fieldType))
      err("Type mismatch in override of '$base.qname' - '$base.fieldType' != '$def.fieldType'", loc)

    // const field cannot override a field (const fields cannot be set,
    // therefore they can override only methods)
    if (def.isConst || def.isReadonly)
      err("Const field '$def.name' cannot override field '$base.qname'", loc)

    // correct override
    return
  }

}