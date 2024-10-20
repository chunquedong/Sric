
import sric::*;

fun testDArray() {
    var a : DArray$<Int>;
    a.add(1);
    a.add(2);

    for (var i = 0; i<a.size(); ++i) {
        var v = a[i];
        printf("%d\n", *v);
    }
}

var globalValue : const Int = 1;
fun testGlobal() {
    printf("%d\n", globalValue);
}

constexpr var arraySize : Int = 10;

fun testArray() {
    var a  = []Int {1,2,3,4};
    for (var i = 0; i<4; ++i) {
        var v = a[i];
        printf("%d\n", v);
    }

    var p: raw* Int = &a;
    for (var i = 0; i<4; ++i) {
        unsafe {
            var v = p[i];
            printf("%d\n", v);
        }
    }

    var b: [arraySize]Int;
    b[0] = 1;
}

fun testString() {
    var cstr = "Hello";
    var str = asStr(cstr);
    str.add("World");

    printf("%s\n", str.c_str());
}

fun main() {
    //testDArray();
    //testString();
    testPtr();
    testInherit();
    testCompare();
    testMove();
    testRaw();
}
