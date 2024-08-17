


struct A {
    readonly var i : Int = 0;
    private fun foo1() {}
    protected fun foo() {}
}

protected struct B : A {
    fun test() {
        foo();
        foo1();
    }
}

fun main() {
    var b = B{};
    var i = b.i;
    b.i = 2;
}