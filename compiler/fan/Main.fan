

class Main
{

  static Void main() {
    if (true) {
        DebugTest().test
        return
    }
    File file := Env.cur.args[0].toUri.toFile
    compiler := IncCompiler.fromProps(file)
    compiler.run
  }

}