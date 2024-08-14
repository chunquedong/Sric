# safeC

Memory safe and compiled systems programming language.

Work in process ...

## Features
- Fast as C. low-level memeory access without GC
- Safe as Rust. ownship and runtime check
- Object-oriented. inheritance and polymorphisn
- Simpler than C++
- Interoperate with existing code. compile to C++ code
- Non-nullable pointer

## Design

### Pointer Type

```
var p: own* Int;       //ownership pointer, both unique and shared
var p: ref* Int;       //non-ownership pointer, safe check at runtime
var p: *Int;           //unsafe raw pointer
var p: weak* Int;      //weak pointer
```

### Explicit Copy and Move

Explicit move or share ownership pointer
```
var p: own* Int = ...;
var p1 = p; //compiler error;
var p2 = move p;
var p3 = share(p2);
```

Explicit move or copy if the struct has ownership pointer:
```
struct A {
    var i: own* Int;
    fun copy(): A { ... }
}
var a: A;
var x = a; //compile error
var b = move a;
var c = a.copy();
```

### Unsafe
Dereference raw pointer in unsafe block

```
var p: *Int;
...
unsafe {
  var i = *p;
}
```

Unsafe function must call in unsafe block
```
unsafe fun foo() { ... }

fun main() {
  unsafe {
    foo();
  }
}
```

### Inheritance

```
trait I {
  virtual fun foo();
}

struct B {
  var a: Int;
  fun bar() { ... }
}

struct A : B, I {
  override fun foo(B* b) {
    ...
  }
}

```

### Init Block

```
struct A {
  var i: Int;
}

var a  = A { i : 0 };
var a: own* A = alloc$<A>() { i : 0 };
```


### Pointer Usage

```
var a: A;
var b: own* A;
a.foo();
b.foo();
```

no pointer arithmetic in safe mode.

### Type Cast:
```
var a = p as own* A;
var b = p is own* A;
```

### Array

static size Array
```
var a  = []Int { 1,2,3 };
var a: [15]Int;
```


### Template
```
struct Bar$<T> {
  fun foo() {
    ...
  }
}

T fun foo$<T>(a: T) {
   return a;
}

var b: Bar$<Int>;
```

### Nullable

Default non-nullable
```
var a: own* B = ..;
var b: own*? B = null;
```


### Immutable

Just like C++
```
var p : raw* const Int;
var p : const raw* Int;
var p : const raw* const Int;
```

Function params is const by default

```
fun foo(a: * mut Int) mut : Int {
    *a = 1;
    return *a;
}
```

### Protection
```
public
private
protected
readonly
```
Readonly means private write and public read

### Operator overload

```
struct A {
    operator fun mult(a: A): A { ... }
}

var c = a * b;
```

operator methods:
```
methods    symbol
------     ------
plus       a + b 
minus      a - b 
mult       a * b 
div        a / b 
get        a[b] 
set        a[b] = c
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

import external module in code:
```
import std::*;
import std::Vec;
```

### Closure

```
fun foo(f: fun(a:Int) ) {
    f(1);
}

foo(fun(a:Int){ ... });
```

### Typealias

typealias:
```
typealias VecInt = std::Vec$<Int>;
```

### Enum

```
enum Color {
    red = 1, green, blue
}

var c = Color::red;
```

## Removed features from C++

- no reference, just pointer
- no class, just struct
- no header file
- no function overload by params
- define one var per statement
- no constructor
- no nested class, nested function
- no namespace, just module
- no macro
- no forward declarations
- no three static
- no friend class
- no multiple inheritance
- no i++ only ++i
- no switch auto fallthrough
- no template overload

