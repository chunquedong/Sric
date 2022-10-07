

class DebugTest : Test {
  Void test() {
    code := 
    Str<| 
            void main()
            {
                int i = 1;
                printf("hello:%d\n", i);
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