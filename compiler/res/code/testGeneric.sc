
struct A$<T> {
    var i: T ;
    fun foo(): ref* T {
       return &i;
    }
}

fun main()
{
    var a = A$<Int>{};
    a.i = 2;
    var b : ref* Int = a.foo();
}
