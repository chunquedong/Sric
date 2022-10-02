

**
** Share Code between TypeRef and TypeDef
** 
mixin TypeMixin {
  
  abstract Str qname()
  
//////////////////////////////////////////////////////////////////////////
// Flags
//////////////////////////////////////////////////////////////////////////

  **
  ** Get the flags bitmask.
  **
  abstract Int flags()

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

//////////////////////////////////////////////////////////////////////////
// Conveniences
//////////////////////////////////////////////////////////////////////////
  
  Bool isObj()     { qname == "sys::Obj" }
  Bool isBool()    { qname == "sys::Bool" }
  Bool isInt()     { qname == "sys::Int" }
  Bool isFloat()   { qname == "sys::Float" }
  Bool isDecimal() { qname == "std::Decimal" }
  Bool isRange()   { qname == "sys::Range" }
  Bool isStr()     { qname == "sys::Str" }
  Bool isThis()    { qname == "sys::This" }
  Bool isType()    { qname == "std::Type" }
  Bool isVoid()    { qname == "sys::Void" }
  Bool isBuf()     { qname == "std::Buf" }
  Bool isList()    { qname == "sys::List" }
  Bool isMap()     { qname == "std::Map" }
//  virtual Bool isFunc()    { this.base.qname == "sys::Func" }
  Bool isNothing() { qname == "sys::Nothing" }
  Bool isError() { qname == "sys::Error" }

  ** Is this a valid type usable anywhere (such as local var)
  virtual Bool isValid() { !isVoid && !isThis }

  **
  ** Is this type ok to use as a const field?  Any const
  ** type fine, plus we allow Obj, List, Map, Buf, and Func since
  ** they will implicitly have toImmutable called on them.
  **
  Bool isConstFieldType()
  {
    if (flags.and(FConst.Const) != 0) return true

    // these are checked at runtime
    //if (t.isObj || t.isList || t.isMap|| t.isBuf || t.isFunc)
    //  return true
    if (flags.and(FConst.RuntimeConst) != 0) return true

    // definitely no way it can be immutable
    return false
  }
  
  Bool isTypeErasure() {
    return qname != "sys::Array" && qname != "sys::Ptr"
    // && qname != "sys::Func"
  }
}
