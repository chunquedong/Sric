

class DebugTest : Test {
  Void test() {
    code := 
    Str<| 
            struct A<T> {
                T i
            }

            void main()
            {
                a := A<int>{}
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