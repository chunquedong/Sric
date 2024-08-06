
extern public struct Int {}
extern public struct Void {}
extern public struct Float {}
extern public struct Bool {}
extern public struct Str {}
extern public struct Any {}

extern public struct Array$<T> {}
extern public struct ArrayRef$<T> {}
extern public struct OwnPtr$<T> {}
extern public struct WeakPtr$<T> {}
extern public struct RawPtr$<T> {}
extern public struct Func$<T> {}

extern public fun printf(format: *const Int8, args: ...);
