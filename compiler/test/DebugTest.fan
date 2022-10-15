

class DebugTest : Test {
  Void test() {
    code := 
    Str<| 
            struct A {
                int i;
            }

            void main()
            {
                A a;
                a.i = 2;
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