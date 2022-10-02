

class DebugTest : Test {
  Void test() {
    code := 
    Str<| 
            public struct A {
                int a;
                int foo() { return a; }
            }
        |>
    
    
    pod := PodDef(Loc.makeUnknow, "testPod")
    m := IncCompiler(pod)
    
    file := "testFile"
    m.updateSource(file, code)
    
    m.checkError
    
    m.context.pod.dump

  }
}