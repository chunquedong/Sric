
class SemanticTest : GoldenTest {
  
   const static Str separator := "\n\n///!!!!!!!!!!!!!!!!!!!!!!!\n\n"
  
//  override Void setup() {
//    super.goldenDir.delete
//  }
  
  Void testParser() {
    count := 0
    pass := 0
    fails := [,]

    srcFiles := File[,]
    `res/`.toFile.walk |f|{
      if (f.isDir || f.ext != "sc") return
      if (f.basename == "sys") return
      //if (f.toStr.find("res/enum/testErrors2.fan") == -1) return
      echo("test:"+f.normalize)
      
      code := f.readAllStr
      ++count
      try {
        runParse(code, f.parent.basename +"/"+ f.basename)
        ++pass
      }
      catch (Err e) {
        fails.add(f)
        e.trace
      }
    }

    echo("GoldenTest:pass:$pass/$count, fails:")
    fails.each { echo("    $it") }
  }
  
  Void runParse(Str code, Str name) {
    
    pod := PodDef(Loc.makeUnknow, "testPod")
    m := IncCompiler(pod)
    //m.context.input.isScript = true
    
    file := name
    m.updateSource(file, code)
    
    m.checkError
    
    s := StrBuf()
    
    writer := AstWriter.make(s.out)
    pod.print(writer)
//    s.add(code)
    s.add(separator)
    m.context.log.errs.each { s.add(it).add("\n") }

    verifyGolden(s.toStr, name)
  }
}
