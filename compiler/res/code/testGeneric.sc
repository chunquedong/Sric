
trait NT {
    abstract operator fun plus(that: ref* NT): ref* NT;
}

struct A$<T = NT> {
    var i: T ;
    fun foo(): ref* T {
       return &i;
    }

    fun foo2(): T {
        return *(i + &i);
    }
}

fun main()
{
    var a = A$<Int>{};
    a.i = 2;
    var b : ref* Int = a.foo();
}
