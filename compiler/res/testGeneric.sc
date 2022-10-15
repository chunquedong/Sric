
struct A<T> {
    T*? i
    T*? foo() {
       return null
    }
}

void main()
{
    a := A<int>{}
    a.i = 2
    b := a.foo();
}
