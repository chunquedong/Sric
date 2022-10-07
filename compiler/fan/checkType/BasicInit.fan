
class BasicInit : CompilerStep {
  
  new make(CompilerContext compiler)
    : super(compiler)
  {
  }
  
  override Void run()
  {
    //debug("BasicInit")
    walkUnits(VisitDepth.slotDef)
  }

  override Void visitTypeDef(TypeDef def)
  {
    loc := def.loc
    def.flags = normalizeFlags(def.flags, loc)
    initVirtualFlags(def)
  }
  
  override Void visitFieldDef(FieldDef m) {
    m.flags = normalizeFlags(m.flags, m.loc)
    if (m.parentDef == null) {
        m.flags = m.flags.or(FConst.Static)
    }
  }

  override Void visitMethodDef(MethodDef m) {
    m.flags = normalizeFlags(m.flags, m.loc)
    if (m.ret == null) {
      m.ret = TypeRef.voidType(m.loc)
    }
    if (m.parentDef == null) {
        m.flags = m.flags.or(FConst.Static)
    }
  }
  
  private Int normalizeFlags(Int flags, Loc loc) {
    if (flags.and(FConst.Abstract) != 0 && flags.and(FConst.Virtual) != 0)
      err("Abstract implies virtual", loc)
    if (flags.and(FConst.Override) != 0 && flags.and(FConst.Virtual) != 0)
      err("Override implies virtual", loc)

    if (flags.and(FConst.Extension) != 0 && flags.and(FConst.Static) == 0) {
      err("Extension must static", loc)
    }

//    if (curUnit.isFanx && flags.and(FConst.Ctor) != 0 && flags.and(FConst.Static) != 0) {
//      err("Ctor can not be static", loc)
//    }
    
    protection := FConst.Internal.or(FConst.Private).or(FConst.Protected).or(FConst.Public)
    if (protection.and(flags) == 0) flags = flags.or(FConst.Public)
    
    
    if (flags.and(FConst.Abstract) != 0) flags = flags.or(FConst.Virtual)
    if (flags.and(FConst.Override) != 0)
    {
      if (flags.and(FConst.Final) != 0)
        flags = flags.and(FConst.Final.not)
      else
        flags = flags.or(FConst.Virtual)
    }
    return flags
  }
  
  private Void initVirtualFlags(TypeDef t) {
    if (t.flags.and(FConst.Virtual) != 0 ||
        t.flags.and(FConst.Abstract) != 0 ||
        t.flags.and(FConst.Final) != 0) return
        
    if (t.flags.and(FConst.Virtual) == 0 && t.flags.and(FConst.Abstract) == 0 &&
        t.flags.and(FConst.Mixin) == 0) {
        t.flags = t.flags.or(FConst.Final)
    }
  }
  
  
  private Void addImplicitReturn(MethodDef m)
  {
    code := m.code
    loc := code.loc

    // we allow return keyword to be omitted if there is exactly one statement
    if (code.size == 1 && !m.returnType.isVoid && code.stmts[0].id == StmtId.expr)
    {
      code.stmts[0] = ReturnStmt.makeSynthetic(code.stmts[0].loc, code.stmts[0]->expr)
      return
    }

    // return is implied as simple method exit
    code.add(ReturnStmt.makeSynthetic(loc))
  }
}
