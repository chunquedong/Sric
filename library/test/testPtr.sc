
import sric::*;

struct A {
    var a: Int = 1;
}

fun testA(a: ref* A) {
    printf("%d\n", a.a);
}

fun testPtr() {
    var a: A;
    testA(&a);

    var p: own* A = alloc$<A>();
    testA(p);

    if (!isNull(p)) {
        printf("%d\n", p.a);
    }

    var rp: ref* A = p;
    p = null;
    //printf("%d\n", rp.a);
}

fun testNullable() {
    var p: ref*? A;
    var i = p.a;
}