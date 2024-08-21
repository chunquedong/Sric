
var g : const Int = 1;

struct A {
    var i : Int = 0;

    fun foo() mut {
        i = 1;
    }

    fun foo2() {
        i = 1;
    }

}

fun foo3(a: A) {
    a.i = 1;
}

fun main() {
    var a : const A = A{};
    a.foo();
    a.foo2();
    foo3(a);
    g = 2;
}
