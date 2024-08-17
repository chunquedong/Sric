

struct A {
    var i : Int;
    var j : Int;
    fun foo() : Int { return i; }
}

fun main()
{
    var a = A { i=1, j=2 };
    a.i = 10;
    var i:Int = a.foo();
}
