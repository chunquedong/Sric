

class DebugTest : Test {
  Void test() {
    code := 
    Str<| 
            void main2()
            {
                int&? p;
                if (true) {
                    int a = 1;
                    p = &a;
                }
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