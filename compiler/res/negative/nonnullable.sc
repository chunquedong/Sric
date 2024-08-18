
fun foo(p: ref* Int) {

}

fun main() {
    var a : own*? Int;
    var b : own* Int;
    
    foo(a);
}