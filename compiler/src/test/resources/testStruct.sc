


struct A {
    i : Int;

    fun foo() : Int { return i; }
}

fun main()
{
    a : A;
    a.i = 2;
    i:Int = a.foo();
}
