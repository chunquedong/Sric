# safeC

memory safe and compiled systems programming language

## Features
- fast as C. low-level memeory access
- safe as Rust. compile time lifetime check
- object-oriented. inheritance and polymorphisn
- simple as C. less features than C++
- interoperate with existing code. compile to C++ code
- familiar C-like syntax
- non-nullable pointer

## Design

### Pointer Type

```
unsafe int* p;   //unsafe raw pointer
int& p;          //temporary local pointer
int* p;          //unique ownership pointer
shared int* p;   //reference count pointer
weak int* p;     //weak pointer for beak cycle reference
```

### Pointer safe

unique pointer and temporary work like Rust, but no lifetime annotations.

- other pointers can implicit convert to temporary pointer.
- temporary pointer can't convert to others.
- short liftime object can't be referenced by long lifetime.

```
void foo() {
  int& p = ...;
  if (true) {
     int a;
     p = &a; //compile error
  }
}
```
- borrow check
```
void main() {
  int* p = ...;
  int& p2 = p;
  foo(move p); //compile error
}
```

### Copy and Move

it cannot be copied if the struct has unique pointer:
```
struct A {
  int* i;
}
A a;
A b = a; //compile error
```

move the ownership
```
struct A {
  int* i;
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

unsafe function must call in unsafe block
```
unsafe void foo() { ... }

void main() {
  unsafe {
    foo();
  }
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
  B* b;
  override void foo(B* b) {
    ...
  }
  override void bar() {...}
}
```


### Struct Init

```
struct A {
  int i;
}

A a = { .i = 0; }
A *a = alloc<A>() { .i = 0; }
```

type inference
```
a := alloc<A>() { .i = 0; }
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

array define
```
int[14] a;
```

array pass by array ref
```
foo(int[] a) {
  print(a.length);
  print(a[0]);
}
foo(a);
```

pointer to array ref
```
unsafe int* p = ...;
int[] a = as_array(p, 14);

int& q = a.pointer;
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
non-nullable is default.
```
B* a;
B*? a = null;
```


### Immutable

const means pointer and content immutable
```
const struct Str {
    ...
}

const Str* str = "";
final StrBuf* str;
```
static and global var mast define as const for thread safe.



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

### Getter/Setter
```
struct Bar {
  private int _size;
  setter void size(int s) {
    this._size = s;
  }
  getter int size() { return _size; }
}

Bar b;
b.size = 2; // call b.size(2);
int n = b.size;
```


### Enum

```
enum Color {
    red = 1, green, blue
}

Color c = Color::red;
```



## Plan Features

### Closure
```

void foo(|int->string| f) { f(1); }
foo |i| { i.to_string };

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


## Removed features from C++

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

