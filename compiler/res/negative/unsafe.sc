

unsafe fun foo() {
}

fun main() {
    var p : raw*? Int = null;
    var i = *p;
    foo();
}