import sric::*;

enum Color {
    Red = 1, Green, Blue
}

fun foo(c: Color) {
    var i = c as Int;
    printf("%d\n", i);
}

fun main() {
    var c = Color::Red;
    foo(c);
}
