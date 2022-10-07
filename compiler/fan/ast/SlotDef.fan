//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 Jul 06  Brian Frank  Creation
//

**
** SlotDef models a slot definition - a FieldDef or MethodDef
**
abstract class SlotDef : DefNode
{ 
//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  new make(Loc loc, TypeDef? parentDef)
    : super(loc)
  {
    this.parentDef = parentDef
  }

//////////////////////////////////////////////////////////////////////////
// SlotDef
//////////////////////////////////////////////////////////////////////////

  override Str qname() {
    if (parentDef != null) return "${parentDef.qname}.${name}"
    else return "${podName}::${name}"
  }
  
  override Void print(AstWriter out) {
    super.print(out)
    out.flags(flags, parentDef != null)
  }
  
  virtual Bool isParameterized() { false }

//////////////////////////////////////////////////////////////////////////
// Tree
//////////////////////////////////////////////////////////////////////////

  override abstract Void walk(Visitor v, VisitDepth depth)
  override abstract Void getChildren(Node[] list, [Str:Obj]? options)

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  TypeDef? parentDef             // parent TypeDef
  override Str name := "?"      // slot name
//  Bool overridden := false      // set by Inherit when successfully overridden

}