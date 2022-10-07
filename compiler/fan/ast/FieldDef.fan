//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//   21 Jul 06  Brian Frank  Ported from Java to Fan
//

**
** FieldDef models a field definition
**
public class FieldDef : SlotDef
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  new make(Loc loc, TypeDef? parent, Str name := "?", Int flags := 0)
     : super(loc, parent)
  {
    this.name = name
    this.flags = flags
    this.fieldType = TypeRef.error(loc)
  }

  FieldExpr makeAccessorExpr(Loc loc, Bool useAccessor)
  {
    Expr? target
    if (isStatic)
      target = StaticTargetExpr(loc, parentDef.asRef())
    else
      target = ThisExpr(loc)

    return FieldExpr(loc, target, this.name, useAccessor)
  }
  
  Int enumOrdinal() {
    enumDef := parentDef.enumDef(name)
    if (enumDef != null) return enumDef.ordinal
    return -1
  }

//////////////////////////////////////////////////////////////////////////
// FieldDef
//////////////////////////////////////////////////////////////////////////

  Str signature() { qname }

  TypeRef inheritedReturnType()
  {
    return fieldType
  }

//////////////////////////////////////////////////////////////////////////
// Tree
//////////////////////////////////////////////////////////////////////////

  override Void walk(Visitor v, VisitDepth depth)
  {
    v.enterFieldDef(this)
    //walkFacets(v, depth)
//    if (depth >= VisitDepth.expr && init != null && walkInit)
//      init = init.walk(v)
    v.visitFieldDef(this)
    v.exitFieldDef(this)
  }
  
  override Void getChildren(Node[] list, [Str:Obj]? options) {
    if (facets != null) {
      list.add(facets)
    }
    
//    if (init != null) {
//      list.add(init)
//    }
    
//    if (get != null && !get.isSynthetic) {
//      list.add(get)
//    }
//    if (set != null && !set.isSynthetic) {
//      list.add(set)
//    }
  }
  **
  ** Does this field covariantly override a method?
  **
  Bool isCovariant() { isOverride && fieldType != inheritedReturnType }

  **
  ** Is this field typed with a generic parameter.
  **
  Bool isGeneric() { fieldType.hasGeneriParamDefeter }

  virtual FieldDef? generic() { null }

  **
  ** Is this field the parameterization of a generic field,
  ** with the generic type replaced with a real type.
  **
  override Bool isParameterized() { false }

  **
  ** Return the bridge if this slot is foreign or uses any foreign
  ** types in its signature.
  **
//  override CBridge? usesBridge()
//  {
//    if (bridge != null) return bridge
//    return fieldType.bridge
//  }

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  override Void print(AstWriter out)
  {
    super.print(out)
    
    if (isConst) out.w("const ")
    //else if (isReadonly) out.w("let ")
    //else out.w("var ")
    fieldType.print(out)
    out.w(" ")
    out.w(name).w(";")
    
    //if (init != null) { out.w(" = "); init.print(out) }
    out.nl.nl
   }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  TypeRef fieldType  // field type
//  Field? field              // resolved finalized field
//  Expr? init                // init expression or null
  Bool walkInit := true     // tree walk init expression
  //FieldDef? concreteBase      // if I override a concrete virtual field
  //TypeRef? inheritedRet       // if covariant override of method
  Bool requiresNullCheck    // flags that ctor needs runtime check to ensure it-block set it
  EnumDef? enumDef          // if an enum name/ordinal pair
  Str? closureInfo          // if this is a closure wrapper field

}