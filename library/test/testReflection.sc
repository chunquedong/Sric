import sric::*;

reflect struct ReflectionTest {
    var a: Int;
    var b: Int = 0;
    fun foo(arg: Int): Int {
        return arg;
    }
}

reflect fun testReflection() {
    var m = findModule("test");
    printf("%s\n", m.name);
}