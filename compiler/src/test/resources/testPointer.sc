struct A {
    int* a;
    int* b;
    raw_ptr<int> c;

    int& foo(int& c) {
        return c;
    }
}

void main()
{
    int i = 1;
    int& p = &i;
}
