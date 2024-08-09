

enum Color {
    Red, Blue, Green
}

struct A {
    var i : Int;

    fun foo() : Int { return i; }
}

fun main()
{
    var a : A;
    a.i = 2;
    var i:Int = a.foo();
}
