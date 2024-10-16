

struct A : B, I {
  override fun foo() {
    bar();
  }
}

trait I {
  abstract fun foo();
}

virtual struct B {
  var a: Int;
  fun bar() {  }
}
