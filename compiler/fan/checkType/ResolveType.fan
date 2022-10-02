
class ResolveType : CompilerStep {
  //////////////////////////////////////////////////////////////////////////
// Construction
//////////////////////////////////////////////////////////////////////////

  **
  ** Constructor takes the associated Compiler
  **
  new make(CompilerContext compiler)
    : super(compiler)
  {
  }
  
  override Void run()
  {
    //debug("ResolveType")
    walkUnits(VisitDepth.slotDef)
  }

  override Void visitTypeDef(TypeDef t)
  {
    t.inheritances.each {
      resolveType(it)
    }
    
    generiParamDefs := t.generiParamDefeters
    //if (generiParamDefs != null) {
      generiParamDefs.each |p| {
        resolveType(p.bound)
      }
    //}
  }
  
  override Void visitFieldDef(FieldDef f) {
    resolveType(f.fieldType)
    if (f.inheritedRet != null) resolveType(f.inheritedRet)
  }
  
  override Void visitMethodDef(MethodDef m) {
    resolveMethod(m)
  }
  
  private Void resolveMethod(MethodDef m) {
    resolveType(m.ret)
    m.paramDefs.each |p| {
      resolveType(p.paramType)
    }
    
    if (m.ret.isThis) m.inheritedRet = m.parent.asRef
    else if (m.inheritedRet != null) resolveType(m.inheritedRet)
  }
  
  private Void resolveType(TypeRef type) {
    doResolveType(this, type)
  }
  
  static Void doResolveType(CompilerStep step, TypeRef type) {
    if (type.isResolved) return
    
    if (type.genericArgs != null) {
      type.genericArgs.each |c| {
        doResolveType(step, c)
      }
    }
    
    //find in imported types
    if (type.podName.isEmpty) {
      if (step.curType != null && step.curType.isGeneric) {
          gt := step.curType.getGeneriParamDefeter(type.name)
          if (gt != null) {
            type.resolveTo(gt)
            type.podName = step.curType.podName
            type.name = step.curType.name + "^" + type.name
            return
          }
      }
      
      types := step.curUnit.imported[type.name]
      if (types == null || types.isEmpty)
      {
        step.err("Unknown type '$type.name'", type.loc)
        type.resolveTo(step.ns.objType.typeDef)
      }
      else {
        // if more then one, first try to exclude those internal to other pods
        if (types.size > 1)
        {
          publicTypes := types.exclude |t| { t.isInternal && t.podName != step.podName }
          if (!publicTypes.isEmpty) types = publicTypes
        }
    
        // if more then one its ambiguous (use errReport to avoid suppression)
        if (types.size > 1)
          step.errReport(CompilerErr("Ambiguous type: " + types.join(", "), type.loc))
        
        impoortedType := types.first as TypeDef
        
//        if (impoortedType.typeDef is ParameterizedType) {
//          type.resolveTo((impoortedType.typeDef as ParameterizedType).root)
//        }
//        else
        if (impoortedType != null)  type.resolveTo(impoortedType)
      }
    }
    else if (type.podName == step.podName) {
        typeDef := step.compiler.pod.resolveType(type.name, false)
        if (typeDef == null) {
           step.err("Unknow type '${type}'", type.loc)
           type.resolveTo(step.ns.objType.typeDef)
        }
        else {
            type.resolveTo(typeDef)
        }
    }
    else {
      //find by qname
      try {
        step.ns.resolveTypeRef(type, type.loc, false)
        if (!type.isResolved) {
          step.err("Unknow type '${type}'", type.loc)
          type.resolveTo(step.ns.objType.typeDef)
        }
        ResolveImports.checkUsingPod(step.compiler, type.typeDef.podName, type.loc)
      }
      catch (Err e) {
        step.err("Unknow type '${type}'", type.loc)
        type.resolveTo(step.ns.objType.typeDef)
      }
    }
    
    //sett typeRef's podname
    if (type.isResolved) {
      if (type.typeDef.qname == "sys::This") {
        type.resolveTo(step.curType)
//        type.podName = step.curType.podName
//        type.name = step.curType.name
        type.podName = "sys"
        type.name = "This"
        return
      }
      if (type.typeDef is GeneriParamDefDef) {
        type.podName = type.typeDef.qname
        type.name = type.typeDef.name
      }
      else if (type.podName.isEmpty && type.typeDef.name == type.name) {
        type.podName = type.typeDef.podName
      }
      
      if (type.typeDef.isGeneric) {
        type.typeDef.generiParamDefeters.each |g| {
          doResolveType(step, g.bound)
        }
      }
      
      if (type.genericArgs != null) {
        if (type.typeDef.isGeneric) {}
        else
          step.err("$type is not Generic", type.loc)
      }
    }
  }
}
