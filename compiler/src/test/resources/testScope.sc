
fun main1()
{
    i := 1;
    p: Int* = &i;
}

fun main2()
{
    p: Int?;
    if (true) {
        a : Int = 1;
        p = &a;
    }
}
