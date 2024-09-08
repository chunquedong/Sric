import sstd::*;

struct A {
    var a: own* Int;
    var b: ref* Int;
    var c: raw* Int;
    var d: weak* Int;
}

fun foo(c: ref* Int) {
}

fun foo2(c: own* Int) {
}

fun main()
{
    var i = 1;
    var p: ref* Int = &i;
    foo(p);

    var p2: own* Int = alloc$<Int>();
    foo(p2);

    p = p2;

    var x: own*? Int;
    foo(x!);

    foo2(move p2);
}
