// To change this License template, choose Tools / Templates
// and edit Licenses / FanDefaultLicense.txt
//
// History:
//   2022Äê10ÔÂ15ÈÕ yangjiandong Creation
//

**
** GenericInstancing
**
class GenericInstancing : Visitor
{
  private TypeDef? instanceType;
  private MethodDef? curMethod
  private [Str:TypeRef]? genericArgs;
  
  TypeDef instantiate(TypeDef type, TypeRef typeRef) {
    if (!type.isGeneric) return type
    
    this.genericArgs = [:]
    typeRef.genericArgs.each |t, i| {
        name := type.generiParamDefeters[i].name
        this.genericArgs[name] = t
    }
    
    instanceType = TypeDef(type.loc, type.unit, type.name, type.flags)
    instanceType.inheritances = type.inheritances.map { instancingTypeRef(it) }
    type.slotDefs.each |s|{
        if (s is FieldDef) {
            def := s as FieldDef
            n := FieldDef(def.loc, instanceType, def.name, def.flags)
            n.fieldType = instancingTypeRef(def.fieldType)
            instanceType.addSlot(n)
        }
        else {
            def := s as MethodDef
            n := MethodDef(def.loc, instanceType, def.name, def.flags)
            n.ret = instancingTypeRef(def.ret)
            def.paramDefs.each |p| {
                np := ParamDef(p.loc, instancingTypeRef(p.ctype), p.name, p.def)
                n.paramDefs.add(np)
            }
            n.code = def.code.dup
            instanceType.addSlot(n)
        }
    }
    
    instanceType.walk(this, VisitDepth.expr)
    
    return instanceType
  }
  
  private TypeRef instancingTypeRef(TypeRef type) {
    if (type.typeDef is GeneriParamDefDef) {
        typeRef := this.genericArgs[type.name]
        return typeRef
    }
    else if (!type.typeDef.isGeneric) return type
    
    if (type.genericArgs != null) {
        np := type.genericArgs.map |t| {
            instancingTypeRef(t)
        }
        type.genericArgs = np;
    }
    return GenericInstancing().instantiate(type.typeDef, type).asRef
  }

  override Expr visitExpr(Expr expr) {
  
    switch (expr.id)
    {
      case ExprId.nullLiteral:      // LiteralExpr
      case ExprId.trueLiteral:
      case ExprId.falseLiteral:
      case ExprId.intLiteral:
      case ExprId.floatLiteral:
      case ExprId.strLiteral:
        return expr
      case ExprId.typeLiteral:
      //case ExprId.localeLiteral:
      case ExprId.slotLiteral:
        return expr
      case ExprId.listLiteral:      // ListLiteralExpr
        return expr
      case ExprId.boolNot:          // UnaryExpr
      case ExprId.cmpNull:
      case ExprId.cmpNotNull:
        return expr

      case ExprId.elvis:
        return expr
      case ExprId.assign:           // BinaryExpr
        return expr

      case ExprId.same:
      case ExprId.notSame:
      case ExprId.boolOr:           // CondExpr
      case ExprId.boolAnd:
        return expr
      
      case ExprId.isExpr:           // TypeCheckExpr
        return expr
      case ExprId.asExpr:
      case ExprId.coerce:
        return expr

      case ExprId.call:             // CallExpr
        return expr

      //case ExprId.construction:
      //  expr = resolveConstruction(expr)
      case ExprId.shortcut:         // ShortcutExpr (has ShortcutOp)
        return expr

      case ExprId.field:            // FieldExpr
        return expr
      case ExprId.localVar:         // LocalVarExpr
        return expr

      case ExprId.thisExpr:         // ThisExpr
        return expr
      case ExprId.superExpr:        // SuperExpr
        return expr
      case ExprId.itExpr:           // ItExpr
        return expr
      case ExprId.staticTarget:     // StaticTargetExpr
        return expr
      case ExprId.unknownVar:       // UnknownVarExpr
        return expr
//      case ExprId.storage:
//        expr = resolveStorage(expr)
      case ExprId.ternary:          // TernaryExpr
        return expr
      case ExprId.complexLiteral:   // ComplexLiteral
        return expr
      case ExprId.closure:          // ClosureExpr
        return expr
      //case ExprId.dsl:              // DslExpr
        //expr = resolveDsl(expr)
      case ExprId.throwExpr:        // ThrowExpr
        return expr
//      case ExprId.awaitExpr:
//        expr = resolveAwait(expr)
      case ExprId.sizeOfExpr:
        return expr
      case ExprId.addressOfExpr:
        return expr
      case ExprId.InitBlockExpr:
        return expr
    }
    
    return expr
  }  
}
