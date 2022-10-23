// To change this License template, choose Tools / Templates
// and edit Licenses / FanDefaultLicense.txt
//
// History:
//   2022-10-23 yangjiandong Creation
//

**
** GenerateCpp
**
class GenerateCpp : CompilerStep
{
  AstWriter out
  Bool isImpl = false
  private Bool isForwardDecl = false
  
  new make(CompilerContext compiler) : super.make(compiler) {
    out = AstWriter()
  }
  
  override Void run()
  {
    //debug("CheckErrors")
    //checkPodDef(pod)
    if (isImpl) {
        out.w("#include \"${podName}.h\"").nl.nl
        out.w("//implement").nl
    }
    else {
        out.w("#ifndef ${podName}_H").nl
        out.w("#define ${podName}_H").nl
        
        out.w("#include \"safec.h\"").nl.nl
        
        out.w("namespace $podName {").nl
    }
    
    if (!isImpl) {
        out.w("//forward decl").nl
        isForwardDecl = true
        walkUnits(VisitDepth.slotDef)
        isForwardDecl = false
        
        out.nl
        out.w("//define").nl
    }
    
    walkUnits(VisitDepth.slotDef)
    
    if (!isImpl) {
        out.nl
        out.w("} //namespace").nl
        out.w("#endif //${podName}_H").nl
    }
  }

  override Void enterTypeDef(TypeDef def) {
    if (isImpl) return
    
    if (def.generiParamDefeters.size > 0) {
        out.w("template<")
        def.generiParamDefeters.each |GeneriParamDefDef p, i| {
            if (i > 0) out.w(", ")
            out.w("typename ")
            out.w(p.name)
        }
        out.w(">").nl
    }
    
    if (def.isEnum)
      out.w("enum $def.name")
    else
      out.w("struct $def.name")
      
    if (isForwardDecl) {
      out.w(";").nl
      return
    }
    
    if (!def.inheritances.isEmpty) out.w(" : public ").w(def.inheritances.join(", public "))
    
    out.w(" {").nl
    out.indent
    
    if (def.isEnum) {
        def.enumDefs.each |e, i| {
            if (i > 0) out.w(",").nl
            out.w(e.name)
            if (e.val != null) out.w(" = ").w(e.val)
        }
        out.nl
    }
  }
  override Void exitTypeDef(TypeDef def) { if (!isImpl && !isForwardDecl) out.unindent.w("};").nl }
  
  private Void printFlags(SlotDef slot) {
    if (slot.parentDef == null) {
        if (slot.isPrivate) out.w("static ")
        else out.w("extern ")
        return
    }
    
    flags := slot.flags
    if (flags.and(FConst.Public)    != 0) out.w("/*public*/ ")
    if (flags.and(FConst.Protected) != 0) out.w("/*protected*/ ")
    if (flags.and(FConst.Private)   != 0) out.w("/*private*/ ")
    if (flags.and(FConst.Internal)  != 0) out.w("/*internal*/ ")
  }

  override Void enterFieldDef(FieldDef def) {
    if (isImpl) {
        if (def.isStatic) {
            out.w(def.fieldType.toCppStr())
            out.w(" ")
            if (def.parentDef != null) out.w(def.parentDef.qname).w("::")
            else out.w(def.podName).w("::")
            out.w(def.name)
            if (def.init != null) out.w(" = ").w(def.init)
            out.w(";").nl
        }
        return
    }
    else if (isForwardDecl) {
        if (def.isStatic) {
            out.w("extern ")
            if (def.isConst) out.w("const ")
            out.w(def.fieldType.toCppStr(podName))
            out.w(" ")
            out.w(def.name).w(";").nl
        }
        return
    }
    
    if (!def.isStatic) {
        printFlags(def)
        if (def.isConst) out.w("const ")
        out.w(def.fieldType.toCppStr(podName))
        out.w(" ")
        out.w(def.name).w(";").nl
    }
  }
  
  override Void exitFieldDef(FieldDef def) { }
  
  private Void printMethodPrototype(MethodDef def) {
    out.w(def.ret.toCppStr(isImpl ? null : podName))
    out.w(" ")
    if (isImpl) {
        if (def.parentDef != null) out.w(def.parentDef.qname).w("::")
        else out.w(def.podName).w("::")
    }
    out.w(def.name).w("(")
    def.paramDefs.each |ParamDef p, Int i|
    {
      if (i > 0) out.w(", ")
      out.w(p.paramType.toCppStr(isImpl ? null : podName))
      out.w(" ")
      out.w(p.name)
      if (!isImpl && p.def != null) { out.w(" = "); p.def.print(out) }
    }
    out.w(")")
  }

  override Void enterMethodDef(MethodDef def) {
    if (!isImpl) {
        if (isForwardDecl) {
            if (def.isStatic) {
                out.w("extern ")
                printMethodPrototype(def)
                out.w(";").nl
            }
            return
        }
        
        printFlags(def)
        if (def.isVirtual) out.w("virtual ")
        printMethodPrototype(def)
        if (def.isOverride) out.w(" override ")
        if (def.code == null) {
            if (def.isVirtual) out.w(" = 0")
            out.w(";")
        }
        else if (def.isInline || (def.parentDef != null && def.parentDef.isGeneric)) {
            out.w(" ")
            def.code.print(out)
        }
        else {
            out.w(";")
        }
        out.nl
    }
    else {
        if (def.code == null) return
        if (def.isInline || (def.parentDef != null && def.parentDef.isGeneric)) {
            return
        }
        
        printMethodPrototype(def)
        out.w(" ")
        def.code.print(out)
        out.nl
    }
  }
  
  override Void exitMethodDef(MethodDef def) {}
}
