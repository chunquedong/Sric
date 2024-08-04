struct A {
    var a: own Int*;
    var b: ref Int*;
    var c: Int*;
    var d: weak Int*;

    fun foo(c: mut Int*) {
    }
}

fun main()
{
    var i = 1;
    var p: Int* = &i;
}
