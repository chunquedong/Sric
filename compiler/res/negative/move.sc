fun foo(c: own* Int) {
}

fun main()
{
    var x: own*? Int;
    foo(x!);
}