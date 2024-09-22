import sric::*;

fun print(x: Int) {
    printf("%d\n", x);
}

fun foo() : Int {
    var a = 0;
    var b = 1;
    var c = a + b * 2;
    
    a *= 10;
    print(a);

    var d = a | b >> 2;
    print(d);

    var e = a > b ? a : b;
    print(e);

    var f = 123.0;
    var g = a < f;

    var h = false;
    var i = !h && g;
    print(i);

    return 1;
}

fun literal() {
    var a = 100;
    var b = 100.0;
    var c = true;
    var d = a + b;
    var e = "str";
}