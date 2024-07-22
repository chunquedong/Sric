struct A {
    a: own Int*;
    b: ref Int*;
    c: Int*;
    d: weak Int*;

    fun foo(c: mut Int*) {
    }
}

fun main()
{
    i := 1;
    p: Int* = &i;
}
