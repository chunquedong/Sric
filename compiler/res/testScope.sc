
void main1()
{
    int i = 1;
    int* p = &i
}

void main2()
{
    int&? p;
    if (true) {
        int a = 1;
        p = &a;
    }
}
