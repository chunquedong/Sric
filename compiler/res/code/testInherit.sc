

trait I {
  virtual fun foo();
}

struct B {
  var a: Int;
  fun bar() {  }
}

struct A : B, I {
  override fun foo() {
    bar();
  }
}
