fun foo(c: own* Int) {
}

var p : own*? Int;

fun main()
{
    var x: own*? Int;
    foo(x!);

    foo(move p);
}