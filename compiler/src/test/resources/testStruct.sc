


struct A {
    int i;

    int foo() { return i; }
}

void main()
{
    A a;
    a.i = 2;
    int i = a.foo();
}
