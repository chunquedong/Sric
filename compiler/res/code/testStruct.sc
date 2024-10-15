

struct A {
    var i : Int;
    var j : Int;
    fun foo() : Int { return i; }

    fun init() {
        this { .i = 2; .j = 3; };
    }
}

fun main()
{
    var a = A { .i=1; .j=2; };
    a.i = 10;
    var i:Int = a.foo();

    var b = A { .init(); };
}
