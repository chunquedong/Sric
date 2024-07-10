
native public struct int {}
native public struct void {}
native public struct float {}
native public struct bool {}
native public struct string {}
native public struct exception {}
native public struct any {}

native public struct array<T> {}
native public struct owner_ptr<T> {}
native public struct instant_ptr<T> {}
native public struct raw_ptr<T> {}
native public struct weak_ptr<T> {}
native public struct function<T> {}

native public int printf(string format, ...);
