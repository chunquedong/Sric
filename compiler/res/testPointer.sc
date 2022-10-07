struct A {
    shared int* a;
    int* b;
    unsafe int* c;

    int& foo(int& c) {
        return c
    }
}

void main()
{
    int i = 1;
    int& p = &i
}
