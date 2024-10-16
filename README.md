# Sric Language

A memory safe and compiled systems programming language.


## Features
- Ownership based memory management without GC.
- Memory safe and no reference limitations.
- Low-level memeory access. 
- Object-oriented.
- Interoperate with existing C++ code.
- Non-nullable pointer.
- Modularization.
- With Block.
- Generic Template, Closure, Operator Overloading.


## Design

### Pointer Type

```
var p: own* Int;       //ownership pointer, both unique and shared
var p: ref* Int;       //non-ownership pointer, safe check at runtime
var p: raw* Int;       //unsafe raw pointer
var p: WeakPtr$<Int>;  //weak pointer
```

### Explicit Copy or Move

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

Single inheritance
```
trait I {
    virtual fun foo();
}

virtual struct B {
    var a: Int;
    fun bar() { ... }
}

struct A : B, I {
    override fun foo(B* b) {
        ...
    }
}

```

### With Block

The with Block is not C++ designated initialization. It can contain any statement.
```
struct A {
    var i: Int;
    fun init() { ... }
}

var a  = A { .init(); .i = 0; };
var a: own* A = alloc$<A>() { .i = 0; };
```


### Pointer Usage

Always access by '.'
```
var a: A;
var b: own* A;
a.foo();
b.foo();
```

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


### Generic Type
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

Non-nullable by default 
```
var a: own*? B;
var b: own* B = a!;
```


### Immutable

Just like C++
```
var p : raw* const Int;
var p : const raw* Int;
var p : const raw* const Int;
```

Function params are const by default
```
struct Bar {
    var i: Int = 0;
    fun set(a: mut Int) mut : Int {
        i = a;
    }
}
```

### Protection
```
public
private
protected
readonly
```
Readonly means public read access, private write access.

### Operator Overloading

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
compare    == != < > <= >=
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
srcDirs = src/
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

### Default Param and Named Arg

```
fun foo(a: Int, b: Int = 0) {
}

fun main() {
    foo(a : 10);
}
```

## Removed features from C++

- No reference, just pointer
- No function overload by params;
- No header file
- No implicit copying of large objects
- No static member
- No new, delete
- No define multi var per statement
- No constructor
- No nested class, nested function
- No class, just struct
- No namespace, just module
- No macro
- No forward declarations
- No three static
- No friend class
- No multiple inheritance
- No virtual,private inheritance
- No i++ just ++i
- No switch auto fallthrough
- No template overload
- No pointer arithmetic in safe mode
