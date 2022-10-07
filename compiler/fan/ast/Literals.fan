
**************************************************************************
** LiteralExpr
**************************************************************************

**
** LiteralExpr puts an Bool, Int, Float, Str, Duration, Uri,
** or null constant onto the stack.
**
class LiteralExpr : Expr
{
  new make(Loc loc, ExprId id, Obj? val)
    : super(loc, id)
  {
    this.val   = val
//    if (val == null && !ctype.isNullable)
//      throw Err("null literal must typed as nullable!")
  }
  
  new makeType(Loc loc, ExprId id, TypeRef ctype, Obj? val)
    : super.make(loc, id)
  {
    this.ctype = ctype
    this.val   = val
    if (val == null && !ctype.isNullable)
      throw Err("null literal must typed as nullable!")
  }

  new makeNull(Loc loc)
    : this.make(loc, ExprId.nullLiteral, null) {}

  new makeTrue(Loc loc)
    : this.make(loc, ExprId.trueLiteral, true) {}

  new makeFalse(Loc loc)
    : this.make(loc, ExprId.falseLiteral, false) {}

  new makeStr(Loc loc, Str val)
    : this.make(loc, ExprId.strLiteral, val) {}

  static LiteralExpr makeDefaultLiteral(Loc loc, TypeRef ctype)
  {
    LiteralExpr? literal
    if (!ctype.isNullable)
    {
      if (ctype.isBool())  literal = make(loc, ExprId.falseLiteral, false)
      else if (ctype.isInt())   literal = make(loc, ExprId.intLiteral, 0)
      else if (ctype.isFloat()) literal = make(loc, ExprId.floatLiteral, 0f)
      
      if (literal != null) {
        literal.ctype = ctype
        return literal
      }
    }
    literal = makeNull(loc)
    literal.ctype = ctype
    return literal
  }

  override Bool isAlwaysNullable() { id === ExprId.nullLiteral }

  override Int? asTableSwitchCase()
  {
    return val as Int
  }

  override Str toStr()
  {
    switch (id)
    {
      case ExprId.nullLiteral: return "null"
      case ExprId.strLiteral:  return "\"" + val.toStr.replace("\n", "\\n") + "\""
      case ExprId.typeLiteral: return "${val}#"
      //case ExprId.uriLiteral:  return "`$val`"
      default: return val.toStr
    }
  }

  Obj? val // Bool, Int, Float, Str (for Str/Uri), Duration, TypeRef, or null
}

**************************************************************************
** SlotLiteralExpr
**************************************************************************

**
** SlotLiteralExpr
**
class SlotLiteralExpr : Expr
{
  new make(Loc loc, TypeRef parent, Str name)
    : super(loc, ExprId.slotLiteral)
  {
    this.parent = parent
    this.name = name
  }

  override Str toStr() { "$parent#${name}" }

  TypeRef parent
  Str name
  SlotDef? slot
}

**************************************************************************
** ListLiteralExpr
**************************************************************************

**
** ListLiteralExpr creates a List instance
**
class ListLiteralExpr : Expr
{
  new make(Loc loc, TypeRef? explicitType := null)
    : super(loc, ExprId.listLiteral)
  {
    this.explicitType = explicitType
  }

  new makeFor(Loc loc, TypeRef explicitType, Expr[] vals)
    : super.make(loc, ExprId.listLiteral)
  {
    this.explicitType = explicitType
    this.vals  = vals
  }

  override Void walkChildren(Visitor v)
  {
    vals = walkExprs(v, vals)
  }

  override Str toStr()
  {
    return format |Expr e->Str| { e.toStr }
  }

  Str format(|Expr e->Str| f)
  {
    s := StrBuf.make
    if (explicitType != null) s.add(explicitType)
    s.add("[")
    if (vals.isEmpty) s.add(",")
    else vals.each |Expr v, Int i|
    {
      if (i > 0) s.add(",")
      s.add(f(v))
    }
    s.add("]")
    return s.toStr
  }

  TypeRef? explicitType
  Expr[] vals := Expr[,]
}
