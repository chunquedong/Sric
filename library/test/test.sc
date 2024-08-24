
import std::*;

fun testDArray() {
    var a : DArray<Int>;
    a.add(1);
    a.add(2);

    for (var i = 0; i<a.size; ++i) {
        var v = a[i];
        printf("%d\n", v);
    }
}


fun main() {
    testDArray();
}
