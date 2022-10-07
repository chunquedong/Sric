//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   19 Jul 06  Brian Frank  Creation
//

**
** Expr
**
abstract class Expr : Node
{

//////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  new make(Loc loc, ExprId id)
    : super(loc)
  {
    this.id = id
  }
  
  **
  ** Get this Token as a ExprId or throw Err.
  **
  static ExprId tokenToExprId(Token token)
  {
    switch (token)
    {
      // unary
      case Token.bang:         return ExprId.boolNot

      // binary
      case Token.assign:       return ExprId.assign
      case Token.doubleAmp:    return ExprId.boolAnd
      case Token.doublePipe:   return ExprId.boolOr
      case Token.same:         return ExprId.same
      case Token.notSame:      return ExprId.notSame
      case Token.elvis:        return ExprId.elvis

      // default
      default: throw Err(token.toStr)
    }
  }
  
  
  **
  ** Map an operator token to it's shortcut operator enum.
  ** Degree is 1 for unary and 2 for binary.
  **
  static ShortcutOp tokenToShortcutOp(Token token, Int degree)
  {
    switch (token)
    {
      case Token.plus:           return ShortcutOp.plus      // a + b
      case Token.minus:          return degree == 1 ? ShortcutOp.negate : ShortcutOp.minus  // -a; a - b
      case Token.star:           return ShortcutOp.mult      // a * b
      case Token.slash:          return ShortcutOp.div       // a / b
      case Token.percent:        return ShortcutOp.mod       // a % b
      case Token.increment:      return ShortcutOp.increment // ++a, a++
      case Token.decrement:      return ShortcutOp.decrement // --a, a--
      case Token.eq:             return ShortcutOp.eq        // a == b
      case Token.notEq:          return ShortcutOp.eq        // a != b
      case Token.cmp:            return ShortcutOp.cmp       // a <=> b
      case Token.gt:             return ShortcutOp.cmp       // a > b
      case Token.gtEq:           return ShortcutOp.cmp       // a >= b
      case Token.lt:             return ShortcutOp.cmp       // a < b
      case Token.ltEq:           return ShortcutOp.cmp       // a <= b
      case Token.assignPlus:     return ShortcutOp.plus      // a += b
      case Token.assignMinus:    return ShortcutOp.minus     // a -= b
      case Token.assignStar:     return ShortcutOp.mult      // a *= b
      case Token.assignSlash:    return ShortcutOp.div       // a /= b
      case Token.assignPercent:  return ShortcutOp.mod       // a %= b
      default: throw Err(token.toStr)
    }
  }

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////

  **
  ** Return this expression as an Int literal usable in a tableswitch,
  ** or null if this Expr doesn't represent a constant Int.  Expressions
  ** which work as table switch cases: int literals and enum constants
  **
  virtual Int? asTableSwitchCase() { null }

  **
  ** Get this expression's type as a string for error reporting.
  **
  Str toTypeStr()
  {
    if (id == ExprId.nullLiteral) return "null"
    return ctype.toStr
  }

  **
  ** If this expression performs assignment, then return
  ** the target of that assignment.  Otherwise return null.
  **
  virtual Obj? assignTarget() { null }

  **
  ** Return if this expression can be used as the
  ** left hand side of an assignment expression.
  **
  virtual Bool isAssignable() { false }

  **
  ** Is this a boolean conditional (boolOr/boolAnd)
  **
  virtual Bool isCond() { false }

  **
  ** Does this expression make up a complete statement.
  ** If you override this to true, then you must make sure
  ** the expr is popped in CodeAsm.
  **
  virtual Bool isStmt() { false }

  **
  ** Was this expression generated by the compiler (not necessarily
  ** everything auto-generated has this flag true, but we set in
  ** cases where error checking needs to be handled special)
  **
  virtual Bool synthetic() { false }

  **
  ** If this an assignment expression, then return the
  ** result of calling the given function with the LHS.
  ** Otherwise return false.
  **
  virtual Bool isDefiniteAssign(|Expr lhs->Bool| f) { false }

  **
  ** Return if this expression is guaranteed to sometimes
  ** return a null result (safe invoke, as, etc)
  **
  virtual Bool isAlwaysNullable() { false }

  **
  ** Assignments to instance fields require a temporary local variable.
  **
  virtual Bool assignRequiresTempVar() { false }

  **
  ** Return if this expression represents the same variable or
  ** field as that.  This is used for self assignment checks.
  **
  virtual Bool sameVarAs(Expr that) { false }

//  **
//  ** Map the list of expressions into their list of types
//  **
//  static TypeRef[] ctypes(Expr[] exprs)
//  {
//    return exprs.map |Expr e->TypeRef| { e.ctype }
//  }

//  **
//  ** Given a list of Expr instances, find the common base type
//  ** they all share.  This method does not take into account
//  ** the null literal.  It is used for type inference for lists
//  ** and maps.
//  **
//  static TypeRef commonType(CNamespace ns, Expr[] exprs)
//  {
//    hasNull := false
//    exprs = exprs.exclude |Expr e->Bool|
//    {
//      if (e.id !== ExprId.nullLiteral) return false
//      hasNull = true
//      return true
//    }
//    t := TypeRef.common(ns, ctypes(exprs))
//    if (hasNull) t = t.toNullable
//    return t
//  }

  **
  ** Return this expression as an ExprStmt
  **
  ExprStmt toStmt()
  {
    return ExprStmt(this)
  }

  **
  ** Make an Expr which will serialize the given literal.
  **
  static Expr makeForLiteral(Loc loc, Obj val)
  {
    switch (val.typeof)
    {
      case Bool#:
        return val == true ?
          LiteralExpr(loc, ExprId.trueLiteral, true) :
          LiteralExpr(loc, ExprId.falseLiteral, false)
      case Str#:
        return LiteralExpr(loc, ExprId.strLiteral, val)
      case Int#:
        return LiteralExpr(loc, ExprId.intLiteral, val)
      case DateTime#:

        return CallExpr(loc, null, "fromStr", ExprId.construction)
        {
          //method = ns.resolveSlot("sys::DateTime.fromStr")
          //ctype  = method.parent
          args   = [makeForLiteral(loc, val.toStr)]
        }
      default:
        throw Err("Unsupported literal type $val.typeof")
    }
  }

  **
  ** Set this expression to not be left on the stack.
  **
  Expr noLeave()
  {
    // if the expression is prefixed with a synthetic cast by
    // CallResolver, it is unnecessary at the top level and must
    // be stripped
    result := this
    if (result.id === ExprId.coerce)
    {
      coerce := (TypeCheckExpr)result
      if (coerce.synthetic) result = coerce.target
    }
    result.leave = false
    return result
  }

//////////////////////////////////////////////////////////////////////////
// Doc
//////////////////////////////////////////////////////////////////////////

  **
  ** Get this expression as a string suitable for documentation.
  ** This string must not contain a newline or it will break the
  ** DocApiParser.
  **
  Str? toDocStr()
  {
    // not perfect, but better than what we had previously which
    // was nothing; we might want to grab the actual text from the
    // actual source file - but with the current design we've freed
    // the buffer by the time the tokens are passed to the parser
    try
    {
      // literals
      if (this is LiteralExpr)
      {
        s := toStr
        if (s.size > 40) s = "..."
        return s
      }

      // if this is cast, return base
      if (this is TypeCheckExpr)
        return ((TypeCheckExpr)this).target.toDocStr

      // if we access an internal slot then don't expose in public docs
      SlotDef? slot := null
      if (this is CallExpr) slot = ((CallExpr)this).method
      else if (this is FieldExpr) slot = ((FieldExpr)this).field
      if (slot != null && (slot.isPrivate || slot.isInternal)) return null

      // remove extra parens with binary ops
      s := toStr
      if (s[0] == '(' && s[s.size-1] == ')') s = s[1..-2]

      // hide implicit assignments
      if (s.contains("=")) s = s[s.index("=")+1..-1].trim

      // remove extra parens with binary ops
      if (s[0] == '(' && s[s.size-1] == ')' && !s.endsWith("()")) s = s[1..-2]

      // hide storage operator
      s = s.replace(".@", ".")

      // hide safe nav construction
      s = s.replace(".?(", "(")

      // use unqualified names
      while (true)
      {
        qcolon := s.index("::")
        if (qcolon == null) break
        i := qcolon-1
        for (; i>=0; --i) if (!s[i].isAlphaNum && s[i] != '_') break
        s = (i < 0) ? s[qcolon+2..-1] : s[0..i] + s[qcolon+2..-1]
      }

      if (s.size > 40) s = "..."
      return s
    }
    catch (Err e)
    {
      e.trace
      return toStr
    }
  }

//////////////////////////////////////////////////////////////////////////
// Tree
//////////////////////////////////////////////////////////////////////////

  Expr walk(Visitor v)
  {
    walkChildren(v)
    return v.visitExpr(this)
  }

  virtual Void walkChildren(Visitor v)
  {
  }

  static Expr? walkExpr(Visitor v, Expr? expr)
  {
    if (expr == null) return null
    return expr.walk(v)
  }

  static Expr[] walkExprs(Visitor v, Expr?[] exprs)
  {
    for (i:=0; i<exprs.size; ++i)
    {
      expr := exprs[i]
      if (expr != null)
      {
        replace := expr.walk(v)
        if (expr !== replace)
          exprs[i] = replace
      }
    }
    return exprs
  }
  
  override Void getChildren(Node[] list, [Str:Obj]? options) {}

//////////////////////////////////////////////////////////////////////////
// Debug
//////////////////////////////////////////////////////////////////////////

  override abstract Str toStr()

  override Void print(AstWriter out)
  {
    out.w(toStr)
  }

//////////////////////////////////////////////////////////////////////////
// Fields
//////////////////////////////////////////////////////////////////////////

  const ExprId id         // expression type identifier
  TypeRef? ctype            // type expression resolves to
  Bool leave := true { protected set } // leave this expression on the stack
}



**************************************************************************
** ExprId
**************************************************************************

**
** ExprId uniquely identifies the type of expr
**
enum class ExprId
{
  nullLiteral,      // LiteralExpr
  trueLiteral,
  falseLiteral,
  intLiteral,
  floatLiteral,
  //decimalLiteral,
  strLiteral,
  //durationLiteral,
  //uriLiteral,
  typeLiteral,
  //localeLiteral,    // LocaleLiteralExpr
  slotLiteral,      // SlotLiteralExpr
  //rangeLiteral,     // RangeLiteralExpr
  listLiteral,      // ListLiteralExpr
  //mapLiteral,       // MapLiteralExpr
  boolNot,          // UnaryExpr
  cmpNull,
  cmpNotNull,
  elvis,
  assign,           // BinaryExpr
  same,
  notSame,
  boolOr,           // CondExpr
  boolAnd,
  isExpr,           // TypeCheckExpr
  //isnotExpr,
  asExpr,
  coerce,
  call,             // CallExpr
  construction,
  shortcut,         // ShortcutExpr (has ShortcutOp)
  field,            // FieldExpr
  localVar,         // LocalVarExpr
  thisExpr,         // ThisExpr
  superExpr,        // SuperExpr
  itExpr,           // ItExpr
  staticTarget,     // StaticTargetExpr
  unknownVar,       // UnknownVarExpr
  //storage,
  ternary,          // TernaryExpr
  complexLiteral,   // ComplexLiteral
  closure,          // ClosureExpr
  //dsl,            // DslExpr
  throwExpr,        // ThrowExpr
  awaitExpr,
  sizeOfExpr,
  addressOfExpr
}

