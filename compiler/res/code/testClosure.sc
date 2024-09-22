
import sric::*;

fun foo(a: fun(a:Int, b:Int):Int) {
    var r = a(1, 2);
    printf("%d\n", r);
}

fun main() {
    foo(fun(a:Int, b:Int):Int{ return a-b; });
}