# safeC

memory safe and compiled systems programming language

## Features
- fast as C. low-level memeory access
- safe as Rust. compile time lifetime check
- powerful as C++. support object-oriented inheritance and polymorphisn
- simple as Java. less features than C++
- interoperate with existing code. compile to C++ code
- familiar C-like syntax


## Design

### Pointer Type

```
shared int* p; //reference count pointer
uniuqe int* p; //unique reference pointer
weak int* p;   //weak pointer for beak cycle reference
unsafe int* p; //unsafe raw pointer
int& p;        //temporary local pointer
int* p;        //default is unique pointer
```

### Pointer safe

unique pointer and temporary work like Rust, but no lifetime annotations.

- other pointers can implicit convert to temporary pointer.
- temporary pointer can't convert to others.
- short liftime object can't be referenced by long lifetime.

### Copy and Move

custem copy function:
```
struct A {
  unique int* i;
  A copy() {...}
}
A a;
A b = a;
```

right-value move by default.
```
struct A {
  unique int* i;
  A copy() {...}
  void move_to(A &a) {...}
}
A getA() { ... }
A b = getA();  //call move_to
```

left-value copy by default.
```
struct A {
  unique int* i;
  void move_to(A &a) {...}
}
A a;
A b = move a;
print(b); //compile error: already moved
```

### Unsafe
dereference unsafe pointer in unsafe block

```
unsafe int* p;
...
unsafe {
  int i = *p;
}
```

### Inheritance

```
mixin I {
  virtual void foo() { ... }
}

virtual struct B {
  int a;
  virtual void bar() { ... }
}

struct A : B, I {
  unique B* b;
  override void foo(B* b) {
    ...
  }
  override void bar() {...}
}
```


### Pointer Usage

```
A a;
A* b;
a.foo();
b.foo();
```
type cast:
```
A *a = cast<A*>(p);
B *a = unsafe_cast<B*>(p);
```

### Array

array is a fat pointer with lenght.
array bounds checks at runtime.

```
int[14] a;
...
foo(int[] a) {
  print(a.length);
  print(a[0]);
}
foo(a);
```

pointer cast to array
```
int *p;
int[] a = as_array(p, 5);

int *q = a.pointer;
```

### Exception
A function that throw exception, must mark with throws keyword.
```
void foo() throws {
}

void dar() throws {
  foo();
}

void bar2() {
  try {
    foo();
  }
  catch (Exception *e) {
  }
}
```

### Template
```
struct Bar<T> {
  void foo() {
    ...
  }
}


T foo<T>(T a) {
   return a;
}
```

### Null
Default non-nullable.
```
B* a;
B*? a = null;
```


### Getter/Setter
```
struct Bar {
  private int _size;
  setter void size(int s) {
    this._size = s;
  }
  int size() { return _size; }
}

Bar b;
b.size = 2; // call b.size(2);
int n = b.size;
```

### Immutable

const means pointer and conent immutable
```
const struct Str {
    ...
}


const Str* str = "";
final StrBuf* str;
```
static and global var mast define as const for thread safe.

### Closure
```

void foo(|int->string| f) { f(1); }
foo |i| { i.to_string };

```


### Operator overload

```
struct A {
    operator A mult(A a) { ... }
}

var c = a * b;

```

operator method:
```
prefix     symbol    degree
------     ------    ------
negate     -a        unary
increment  a++       unary
decrement  a--       unary
plus       a + b     binary
minus      a - b     binary
mult       a * b     binary
div        a / b     binary
mod        a % b     binary
get        a[b]      binary
set        a[b] = c  ternary
add        a { b, }
```

### enum

```
enum Color {
    red = 1, green, blue
}

Color c = Color::red;
```


### Removed features from C++

- no reference, only pointer
- no class, only struct
- no header file
- no function overload
- define one var per statement
- no RAII
- no constructor and destructor
- no nested class, nested function
- no namespace, only module
- no macro
- no template constraint
- no union
- no forward declarations
- no three static
- type alias in import statement
- no friend class
- no ++i only i++ and exper type is void
- no switch fallthrough
- no do while statement

