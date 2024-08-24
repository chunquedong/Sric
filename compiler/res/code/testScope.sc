
fun main1()
{
    var i = 1;
    var p: ref* Int = &i;
}

fun main2()
{
    var p: ref*? Int;
    if (true) {
        var a : Int = 1;
        p = &a;
    }
}
