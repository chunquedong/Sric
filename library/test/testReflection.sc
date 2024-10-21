import sric::*;

reflect struct ReflectionTest {
    var a: Int;
    var b: Int = 0;
    fun foo(arg: Int): Int {
        return arg;
    }
}

reflect enum Color {
    Red, Green = 3, Blue
}

fun foo(c: Color) {
    var i = c as Int;
    printf("%d\n", i);
}

reflect fun testReflection() {
    var m = findModule("test");
    printf("%s\n", m.name);
}

fun testEnum() {
    foo(Color::Blue);
}