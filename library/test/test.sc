
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
}
