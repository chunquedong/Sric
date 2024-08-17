

struct A : B, I {
  override fun foo() {
    bar();
  }
}

trait I {
  abstract fun foo();
}

struct B {
  var a: Int;
  fun bar() {  }
}
