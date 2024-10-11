fun foo(c: own* Int) {
}

var p : own*? Int;
var p2 : own* Int;

fun main()
{
    var x: own*? Int;
    foo(x!);

    foo(move p);
    foo(move p2);
}