
struct A<T> {
    T i;
    T*? foo() {
       return null;
    }
}

void main()
{
    A<int> a = A<int>{};
    a.i = 2;
    int* b = a.foo();
}
