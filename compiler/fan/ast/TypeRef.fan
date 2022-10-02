//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   4 Jun 06  Brian Frank  Creation
//

**
** TypeRef is a "compiler type" which is class used for representing
** the Fantom type system in the compiler.  CTypes map to types within
** the compilation units themsevles as TypeDef and TypeRef or to
** precompiled types in imported pods via ReflectType or FType.
**
class TypeRef : Node, TypeMixin
{
  Str name
  Str podName
  
  TypeRef[]? genericArgs {
    set { &genericArgs = it; if (it != null && it.any|a|{ a ===  this}) throw Err("self ref") }
  }
  
  ** for sized primitive type. the Int32's extName is 32
  Str? sized
  
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
  
//////////////////////////////////////////////////////////////////////////
// Builder
//////////////////////////////////////////////////////////////////////////
  
  static TypeRef? objType(Loc loc) { makeRef(loc, "sys", "Obj") }
  static TypeRef? voidType(Loc loc) { makeRef(loc, "sys", "Void") }
  static TypeRef? errType(Loc loc) { makeRef(loc, "sys", "Err") }
  static TypeRef? error(Loc loc) { makeRef(loc, "sys", "Error") }
  static TypeRef? nothingType(Loc loc) { makeRef(loc, "sys", "Nothing") }
  static TypeRef? boolType(Loc loc) { makeRef(loc, "sys", "Bool") }
  static TypeRef? enumType(Loc loc) { makeRef(loc, "sys", "Enum") }
  static TypeRef? facetType(Loc loc) { makeRef(loc, "sys", "Facet") }
  static TypeRef? intType(Loc loc) { makeRef(loc, "sys", "Int") }
  static TypeRef? strType(Loc loc) { makeRef(loc, "sys", "Str") }
  static TypeRef? thisType(Loc loc) { makeRef(loc, "sys", "This") }
  static TypeRef? listType(Loc loc, TypeRef elemType) {
    t := makeRef(loc, "sys", "List")
    t.genericArgs = [elemType]
    return t
  }
  static TypeRef? funcType(Loc loc, TypeRef[] params, TypeRef ret) {
    t := makeRef(loc, "sys", "Func")
    t.genericArgs = [ret].addAll(params)
    return t
  }
  static TypeRef? asyncType(Loc loc) { makeRef(loc, "concurrent", "Async") }
//  static TypeRef? promiseType(Loc loc) { TypeRef(loc, "concurrent", "Promise") }
  static TypeRef? mapType(Loc loc, TypeRef k, TypeRef v) {
    t := makeRef(loc, "std", "Map")
    t.genericArgs = [k, v]
    return t
  }
  
//////////////////////////////////////////////////////////////////////////
// methods
//////////////////////////////////////////////////////////////////////////
  
  override Void print(AstWriter out)
  {
    out.w(toStr)
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
        c := typeDef.parameterizedTypeCache[extName]
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
    
    if (podName.isEmpty && !typeDef.isError) podName = this.resolvedType.podName
    if (nullabelePeer != null) {
        nullabelePeer.resolvedType = this.resolvedType
        if (nullabelePeer.podName.isEmpty && !typeDef.isError) nullabelePeer.podName = this.resolvedType.podName
    }
  }

  **
  ** Qualified name such as "sys:Str".
  **
  override Str qname() { "${podName}::$name" }

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
  
  
  override Int flags() { typeDef.flags }
  
  
  virtual Bool isFunc() { qname == "sys::Func" || (base != null && base.qname == "sys::Func") }
  
//  private TypeRef dup() {
//    d := TypeRef(qname, name)
//    d.resolvedType = typeDef
//    return d
//  }

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
  virtual Bool isVal() {
    if (isNullable) return false
    return flags.and(FConst.Struct) != 0
  }

  Bool isJavaVal() {
    if (isNullable) return false
    n := qname
    return n == "sys::Bool" || n == "sys::Float" || n == "sys::Int"
  }
  
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

  **
  ** If this is a parameterized type which uses 'This',
  ** then replace 'This' with the specified type.
  **
  virtual TypeRef parameterizeThis(TypeRef thisType) {
    //if (!usesThis) return this
    //f := |TypeRef t->TypeRef| { t.isThis ? thisType : t }
    //return FuncType(params.map(f), names, f(ret), defaultParameterized)
    
    if (this.isThis) return thisType
    
    if (this.genericArgs != null) {
      hasThis := this.genericArgs.any { it.isThis }
      if (!hasThis) return this
      
      nt := TypeRef.makeResolvedType(this.resolvedType)
      if (this.isNullable)
        nt = nt.toNullable
      nt.genericArgs = this.genericArgs.map |a|{ a.parameterizeThis(thisType) }
      return nt
    }
    return this
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
    else if (t.isThis || t.podName.isEmpty || t.name != typeDef.name) {
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

    // let anything fit unparameterized generic parameters like
    // V, K (in case we are using List, Map, or Method directly)
    //if (t.name.size == 1 && t.pod.name == "sys")
    //  return true

    //echo("$this not fits $ty")

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

  **
  ** Map of the all defined slots, both fields and
  ** methods (including inherited slots).
  **
  virtual Str:SlotDef slots() { typeDef.slots }

  **
  ** Return if this type contains a slot by the specified name.
  **
  Bool hasSlot(Str name) { slots.containsKey(name) }

  **
  ** Lookup a slot by name.  If the slot doesn't exist then return null.
  **
  virtual SlotDef? slot(Str name) { slots[name] }

  **
  ** Lookup a field by name (null if method).
  **
  virtual FieldDef? field(Str name) { slot(name) as FieldDef }

  **
  ** Lookup a method by name (null if field).
  **
  virtual MethodDef? method(Str name) { slot(name) as MethodDef }

  **
  ** List of the all defined fields (including inherited fields).
  **
  FieldDef[] fields() { slots.vals.findType(FieldDef#) }

  **
  ** List of the all defined methods (including inherited methods).
  **
  MethodDef[] methods() { slots.vals.findType(MethodDef#) }

  **
  ** List of the all constructors.
  **
  //MethodDef[] ctors() { slots.vals.findAll |s| { s.isCtor } }

  ** List of the all instance constructors.
  **
  //MethodDef[] instanceCtors() { slots.vals.findAll |s| { s.isInstanceCtor } }

  **
  ** Get operators lookup structure
  **
//  abstract COperators operators()

}


