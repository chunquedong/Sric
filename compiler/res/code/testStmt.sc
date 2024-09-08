import sstd::*;

fun foo() : Int {
    if (true) {
        printf("if\n");
    }

    for (var i=0; i<10; ++i) {
        printf("for %d", i);
        if (i > 5) break;
    }

    var i = 0;
    while (i < 2) {
        ++i;
        printf("while %d", i);
    }

    switch (i) {
        case 1:
            fallthrough;
        case 2:
            printf("case 1,2\n");
        case 3:
            printf("case 3\n");
        default:
            printf("case default\n");
    }

    unsafe {
        printf("unsafe\n");
    }

    return i;
}