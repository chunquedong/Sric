
fun main1()
{
    var i = 1;
    var p: *Int = &i;
}

fun main2()
{
    var p: *? Int;
    if (true) {
        var a : Int = 1;
        p = &a;
    }
}
