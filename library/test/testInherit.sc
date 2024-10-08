
import sric::*;

struct C : B, I {
  override fun foo() {
    printf("C::foo\n");
  }
}

trait I {
  abstract fun foo();
}

virtual struct B {
  var a: Int;
  virtual fun foo() { printf("B::foo\n"); }
}

fun testInherit() {
    var x = alloc$<C>();
    x.foo();

    var p1: ref* B = x;
    var p3: ref* I = p1 as ref* I;
    p3.foo();
    var p4: ref* I = x as ref* I;
    
    p4.foo();

    var b1 = p1 is ref* I;

    printf("x is I: %d\n", b1);
}
