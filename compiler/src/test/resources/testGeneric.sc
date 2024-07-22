
struct A<T> {
    i: T ;
    fun foo(): T* {
       return &i;
    }
}

fun main()
{
    a := A<Int>{};
    a.i = 2;
    b : Int* = a.foo();
}
