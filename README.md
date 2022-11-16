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
raw int* p;      //unsafe raw pointer
int& p;          //temporary local pointer
int* p;          //unique ownership pointer
dchecked int* p; //checked in debug mode
checked int* p;  //checked in runtime
shared int* p;   //reference count pointer
weak int* p;     //weak pointer
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
dereference raw pointer in unsafe block

```
raw int* p;
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

Only interface can have virtual methods.
```
interface I {
  virtual void foo();
}

struct B {
  int a;
  void foo() { ... }
}

struct A : B, I {
  B* b;
  override void foo(B* b) {
    ...
  }
}
```

Protection
```
public
private
protected
internal
```
defaults to public

### Struct Init

```
struct A {
  int i;
}

A a = { .i = 0; }
A *a = alloc<A>() { .i = 0; }
unsafe A *a = alloc<A>() { .i = 0; }
shared A *a = alloc_shared<A>() { .i = 0; }
```

type inference
```
a := alloc<A>() { .i = 0; }
```

Constructor as same as normal method except for extra checks.
```
struct A {
  const int i;
  
  cotr void init(int s) {
    this { .i=s; }
  }
}

a := A.init(2);
p := alloc<A>().init(2);
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
no pointer arithmetic in safe mode.

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

array ref is temp fat pointer.
heap dynamic array is not supported.

pointer to array ref
```
raw int* p = ...;
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

readonly for outside class.
```
struct A {
   readonly int i;
   void change() {
      i = 1;
   }
}

A a;
a.i = 1; //compile error;
```

const class and const value
```
const struct Str {
    ...
}

const Str* str = "";
```
static and global value mast define as const for thread safe.



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


### Enum

```
enum Color {
    red = 1, green, blue
}

Color c = Color::red;
```

### Module

Module is namespace as well as the unit of compilation and deployment.

A module contains several source files and folders.

The module is defined in build scripts:
```
name = std
summary = standard library
outType = lib
version = 1.0
depends = sys 1.0
srcDirs = src/*
```

import external module in source file:
```
using std;
using std::vec;
```

type alias in import statement:
```
using std::vec<int> as ints;
```


## Planned Features

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

## Removed features from C++

- no reference, only pointer
- no class, only struct
- no header file
- no function overload
- define one var per statement
- no RAII
- no explicit constructor and destructor
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

