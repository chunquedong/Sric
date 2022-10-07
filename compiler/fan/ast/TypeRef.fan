//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Jun 06  Brian Frank  Creation
//

enum class PtrType {
    shared_ptr, weak_ptr, unique_ptr, temp_ptr, unsafe_ptr
}

**
** TypeRef is a "compiler type" which is class used for representing
** the Fantom type system in the compiler.  CTypes map to types within
** the compilation units themsevles as TypeDef and TypeRef or to
** precompiled types in imported pods via ReflectType or FType.
**
class TypeRef : Node
{
  Str name
  Str podName
  
  TypeRef[]? genericArgs {
    set { &genericArgs = it; if (it != null && it.any|a|{ a ===  this}) throw Err("self ref") }
  }
  
  ** for sized primitive type. the Int32's extName is 32
  Str? sized
  
  PtrType? ptrType
  
  **
  ** Is this is a nullable type (marked with trailing ?)
  **
  private const Bool _isNullable := false
  private TypeRef? nullabelePeer = null
  
  private TypeDef? resolvedType
  
//////////////////////////////////////////////////////////////////////////
// Ctors
//////////////////////////////////////////////////////////////////////////
  
  static TypeRef makeRef(Loc loc, Str? pod, Str name) {
    t := TypeRef(pod ?: "", name, loc)
    return t
  }
  
  new make(Str pod, Str name, Loc loc := Loc.makeUnknow) : super.make(loc) {
    this.podName = pod
    this.loc = loc
    
    if (pod == "sys" || pod.isEmpty) {
      if (name.size > 3 && 
        (name == "Int8" || name == "Int16" || name == "Int32" || name == "Int64") ) {
        sized = name[3..-1]
        this.name = name[0..<3]
      }
      else if (name.size > 5 &&
        (name == "Float32" || name == "Float64") ) {
        sized = name[5..-1]
        this.name = name[0..<5]
      }
      else {
        this.name = name
      }
    }
    else {
      this.name = name
    }
  }
  
  static TypeRef makeQname(Str sig) {
    colon    := sig.index("::")
    podName := sig[0..<colon]
    name := sig[colon+2..-1]
    
    return TypeRef(podName, name)
  }
  
  new makeResolvedType(TypeDef resolvedType, Loc? loc := null) : super.make(loc) {
    this.resolvedType = resolvedType
    this.name = resolvedType.name
    this.podName = resolvedType.podName
    this.loc = loc ?: this.resolvedType.loc
  }
  
  
  **
  ** Is this a public scoped class
  **
  Bool isPublic() { flags.and(FConst.Public) != 0 }

  **
  ** Is this an internally scoped class
  **
  Bool isInternal() { flags.and(FConst.Internal) != 0 }

  **
  ** Is this a compiler generated synthetic class
  **
  Bool isSynthetic() { flags.and(FConst.Synthetic) != 0 }

  **
  ** Is the entire class implemented in native code?
  **
  Bool isNative() { flags.and(FConst.Native) != 0 }
  
  
  **
  ** Return if this Type is an sys::Enum
  **
  Bool isEnum() { flags.and(FConst.Enum) != 0 }

  **
  ** Return if this Type is abstract and cannot be instantiated.  This
  ** method will always return true if the type is a mixin.
  **
  Bool isAbstract() { flags.and(FConst.Abstract) != 0 }

  **
  ** Return if this Type is const and immutable.
  **
  Bool isConst() { flags.and(FConst.Const) != 0 }
  
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
  
  Bool isObj()     { signature == "sys::any" }
  Bool isBool()    { signature == "sys::bool" }
  Bool isInt()     { signature == "sys::int" }
  Bool isFloat()   { signature == "sys::float" }
  Bool isStr()     { signature == "sys::string" }
  Bool isVoid()    { signature == "sys::void" }
  Bool isNothing() { signature == "sys::nothing" }
  Bool isError() { signature == "sys::error" }
  
//////////////////////////////////////////////////////////////////////////
// Builder
//////////////////////////////////////////////////////////////////////////
  
  static TypeRef? objType(Loc loc) { makeRef(loc, "sys", "any") }
  static TypeRef? voidType(Loc loc) { makeRef(loc, "sys", "void") }
  static TypeRef? errType(Loc loc) { makeRef(loc, "sys", "exception") }
  static TypeRef? error(Loc loc) { makeRef(loc, "sys", "error") }
  static TypeRef? nothingType(Loc loc) { makeRef(loc, "sys", "nothing") }
  static TypeRef? boolType(Loc loc) { makeRef(loc, "sys", "bool") }
  static TypeRef? intType(Loc loc) { makeRef(loc, "sys", "int") }
  static TypeRef? strType(Loc loc) { makeRef(loc, "sys", "string") }
  static TypeRef? pointerType(Loc loc, TypeRef elemType, PtrType ptrType = PtrType.unique_ptr) {
    t := makeRef(loc, "sys", "pointer")
    t.genericArgs = [elemType]
    t.ptrType = ptrType
    return t
  }

  static TypeRef? listType(Loc loc, TypeRef elemType, PtrType ptrType = PtrType.unique_ptr) {
    t := makeRef(loc, "sys", "array")
    t.genericArgs = [elemType]
    t.ptrType = ptrType
    return t
  }
  static TypeRef? funcType(Loc loc, TypeRef[] params, TypeRef ret) {
    t := makeRef(loc, "sys", "function")
    t.genericArgs = [ret].addAll(params)
    return t
  }
  //static TypeRef? asyncType(Loc loc) { makeRef(loc, "concurrent", "Async") }
  
//////////////////////////////////////////////////////////////////////////
// methods
//////////////////////////////////////////////////////////////////////////
  
  override Void print(AstWriter out)
  {
    if (ptrType != null) {
        if (ptrType == PtrType.shared_ptr) {
            out.w("shared_ptr")
        }
        else if (ptrType == PtrType.weak_ptr) {
            out.w("weak_ptr")
        }
        else if (ptrType == PtrType.unique_ptr) {
            out.w("unique_ptr")
        }
        else if (ptrType == PtrType.unsafe_ptr) {
            out.w("unsafe_ptr")
        }
        else if (ptrType == PtrType.temp_ptr) {
            //out.w("temp_ptr")
        }
    }
    else {
        if (!podName.isEmpty && podName != "sys") {
          out.w(podName).w("::")
        }
        out.w(name)
    }
    if (genericArgs != null) {
      if (ptrType == PtrType.temp_ptr) {
        out.w(genericArgs.first).w("*")
      }
      else {
        out.w("<").w(genericArgs.join(",")).w(">")
      }
    }
  }
  

  TypeDef typeDef() {
    if (resolvedType == null) {
      throw Err("try access unresolved type: $this")
      //resolvedType = PlaceHolderTypeDef("Error")
    }
    return resolvedType
  }
  
  
  virtual Bool isResolved() {
    if (resolvedType == null) return false
//    if (typeDef.isError()) return false
    return true
  }
  
    
  ** generic genericArgs is absent
  Bool defaultParameterized() {
//    if (resolvedType is ParameterizedType) {
//       return ((ParameterizedType)resolvedType).defaultParameterized
//    }
    return false
  }
  
  Void resolveTo(TypeDef typeDef, Bool defaultParameterized := true) {
    if (typeDef.isGeneric) {
      if (genericArgs == null && !defaultParameterized) {
        resolvedType = typeDef
      }
      else {
        resolvedType = typeDef
        //c := typeDef.parameterizedTypeCache[extName]
        //TODO
//        if (c == null) {
//          c = ParameterizedType.create(typeDef, genericArgs)
//          typeDef.parameterizedTypeCache[extName] = c
//        }
//        resolvedType = c
//
//        if ((resolvedType as ParameterizedType).defaultParameterized) {
//          genericArgs = (resolvedType as ParameterizedType).genericArgs
//        }
      }
    }
    else {
      resolvedType = typeDef
    }
    
    if (podName.isEmpty) podName = this.resolvedType.podName
    if (nullabelePeer != null) {
        nullabelePeer.resolvedType = this.resolvedType
        if (nullabelePeer.podName.isEmpty) nullabelePeer.podName = this.resolvedType.podName
    }
  }

  **
  ** This is the full signature of the type.
  **
  Str signature() {
    s := StrBuf()
    
    if (!podName.isEmpty) {
      s.add(podName).add("::")
    }
    s.add(name)
    s.add(extName)
    return s.toStr
  }
  
  Str extName() {
    s := StrBuf()
    if (sized != null) s.add(sized)
    if (ptrType != null) s.add("_").add(ptrType.toStr)
    if (genericArgs != null) {
      s.add("<").add(genericArgs.join(",")).add(">")
    }
    if (_isNullable) {
      s.add("?")
    }
    return s.toStr
  }

  **
  ** Return signature
  **
  override Str toStr() {
    if (podName != "sys") return signature
    return name
  }
  
  
  Int flags() { typeDef.flags }
  
  Str qname() { "$podName::$name" }
  
  virtual Bool isFunc() { qname == "sys::func" || (base != null && base.qname == "sys::func") }
  

   override Void getChildren(Node[] list, [Str:Obj]? options) {
     if (genericArgs != null) {
        genericArgs.each { list.add(it) }
     }
   }

//////////////////////////////////////////////////////////////////////////
// Nullable
//////////////////////////////////////////////////////////////////////////

  **
  ** Is this is a value type (Bool, Int, or Float and their nullables)
  **
//  virtual Bool isVal() {
//    if (isNullable) return false
//    return flags.and(FConst.Struct) != 0
//  }
  
  private new makeNullable(TypeRef type) : super.make(type.loc) {
    this.name = type.name
    this.podName = type.podName
    this.resolvedType = type.resolvedType
    this._isNullable = true
    this.genericArgs = type.genericArgs
    this.loc = type.loc
    this.len = type.len
    //d.attachedGeneriParamDef = attachedGeneriParamDef
    this.sized = type.sized
  }

  **
  ** Get this type as a nullable type (marked with trailing ?)
  **
  virtual TypeRef toNullable() {
    if (_isNullable) return this
    if (nullabelePeer != null) return nullabelePeer
    nullabelePeer := makeNullable(this)
    nullabelePeer.nullabelePeer = this
    return nullabelePeer
  }

  **
  ** Get this type as a non-nullable (if nullable)
  **
  virtual TypeRef toNonNullable() {
    if (!_isNullable) return this
    return nullabelePeer
  }
  
  **
  ** Is this is a nullable type (marked with trailing ?)
  **
  Bool isNullable() { _isNullable || (resolvedType != null && resolvedType is GeneriParamDefDef) }
  
  Bool isExplicitNullable() { _isNullable }
  

//////////////////////////////////////////////////////////////////////////
// Generics
//////////////////////////////////////////////////////////////////////////

  **
  ** A parameterized type is a type which has parameterized a generic type
  ** and replaced all the generic parameter types with generic argument
  ** types.  The type Str[] is a parameterized type of the generic type
  ** List (V is replaced with Str).  A parameterized type always has a
  ** signature which is different from the qname.
  **
  Bool isParameterized() {
    //if (this.typeDef is ParameterizedType) return true
    return false
  }
  
  ** after generic type erasure
  virtual TypeRef raw() {
    if (typeDef is GeneriParamDefDef) {
        t := ((GeneriParamDefDef)typeDef).bound
        if (this.isNullable && !t.isNullable) t = t.toNullable
        return t
    }
    return this
  }
  
  Bool isGeneriParamDefeter() {
    return typeDef is GeneriParamDefDef
  }
  
  TypeDef? generic() {
    if (typeDef.isGeneric) return typeDef
    //if (typeDef is ParameterizedType) return ((ParameterizedType)typeDef).root
    return null
  }

  **
  ** Return if this type is a generic parameter (such as V or K) in a
  ** generic type (List, Map, or Method).  Generic parameters serve
  ** as place holders for the parameterization of the generic type.
  ** Fantom has a predefined set of generic parameters which are always
  ** defined in the sys pod with a one character name.
  **
  Bool hasGeneriParamDefeter() {
    if (this.typeDef.isGeneric) return true
    if (this.typeDef is GeneriParamDefDef) return true
    if (this.genericArgs != null) {
      if (this.genericArgs.any { it.hasGeneriParamDefeter }) return true
    }
    
    return false
  }
  
  TypeRef funcRet() {
    if (genericArgs == null || genericArgs.size == 0) return TypeRef.make("sys", "Obj").toNullable
    return this.genericArgs.first
  }
  
  TypeRef[] funParamDefs() {
    if (genericArgs == null || genericArgs.size == 0) {
      t := TypeRef.make("sys", "Obj").toNullable
      return [t, t, t, t, t, t, t, t]
    }
    return this.genericArgs[1..-1]
  }
  
  Int funcArity() {
    if (genericArgs == null || genericArgs.size == 0) return 8
    return this.genericArgs.size - 1
  }
  
  TypeRef arrayOf() {
    if (genericArgs == null|| genericArgs.size == 0) return TypeRef.make("sys", "Obj").toNullable
    return this.genericArgs[0]
  }
  
  
//////////////////////////////////////////////////////////////////////////
// Inheritance
//////////////////////////////////////////////////////////////////////////
  
  virtual TypeRef[] inheritances() { typeDef.inheritances }

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
  ** Hash on signature.
  **
  override Int hash()
  {
    return typeDef.signature.hash
  }

  **
  ** Equality based on signature.
  **
  override Bool equals(Obj? t)
  {
    if (this === t) return true
    that := t as TypeRef
    if (that == null) return false
    return signature == that.signature
  }
    
  private TypeRef typeForMatch() {
    t := this
    if (typeDef is GeneriParamDefDef) {
        t = ((GeneriParamDefDef)typeDef).bound
    }
    else if (t.podName.isEmpty || t.name != typeDef.name) {
        t = typeDef.asRef
    }
    t = t.toNonNullable
    return t
  }

  **
  ** Does this type implement the specified type.  If true, then
  ** this type is assignable to the specified type (although the
  ** converse is not necessarily true).  All types (including
  ** mixin types) fit sys::Obj.
  **
  virtual Bool fits(TypeRef ty)
  {
    if (ty.isObj) return true
    if (this == ty) return true
    
    if (this.isFunc && ty.isFunc) {
        return Coerce.isFuncAutoCoerce(this, ty)
    }
    
    //unparameterized generic parameters
    // don't take nullable in consideration
    t := ty.typeForMatch
    m := this.typeForMatch

    // short circuit if myself
    if (m.qname == t.qname) {
        if (t.genericArgs == null || m.genericArgs == null) return true
        if (t.defaultParameterized || m.defaultParameterized) return true
        if (t.genericArgs.size != m.genericArgs.size) {
            //echo("fits1: m != $t: size: ${m.genericArgs.size}!=${t.genericArgs.size}")
            return false
        }
        for (i:=0; i<genericArgs.size; ++i) {
          if (!t.genericArgs[i].fits(m.genericArgs[i]) && !m.genericArgs[i].fits(t.genericArgs[i])) {
            //echo("fits2: $m != $t; param:$i; $t.genericArgs[i] != m.genericArgs[i]")
            return false
          }
        }
        return true
    }

    // recurse extends
    if (base != null && base.fits(t)) return true

    // recuse mixins
    for (i:=0; i<mixins.size; ++i)
      if (mixins[i].fits(t)) return true

    // no fit
    return false
  }

  **
  ** Return if this type fits any of the types in the specified list.
  **
  Bool fitsAny(TypeRef[] types)
  {
    return types.any |TypeRef t->Bool| { this.fits(t) }
  }


//////////////////////////////////////////////////////////////////////////
// Slots
//////////////////////////////////////////////////////////////////////////

//  **
//  ** Return if this type contains a slot by the specified name.
//  **
//  Bool hasSlot(Str name) { slots.containsKey(name) }
//
//  **
//  ** Lookup a slot by name.  If the slot doesn't exist then return null.
//  **
//  virtual SlotDef? slot(Str name) { slots[name] }

}


