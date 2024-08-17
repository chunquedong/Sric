import std::*;

fun main() {
    var a = []Int{ 1,2,3 };
    for (var i = 0; i<3; ++i) {
        var v = a[i];
        printf("%d\n", v);
    }
}
