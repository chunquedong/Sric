
struct A$<T> {
    var i: T ;
    fun foo(): *T {
       return &i;
    }
}

fun main()
{
    var a = A$<Int>{};
    a.i = 2;
    var b : *Int = a.foo();
}
