

externc fun printf(format: raw*const Int8, args: ...): Int;

extern fun alloc$<T>(): own* T;

extern struct WeakPtr$<T> {
    fun init(p: own* T);
    fun lock(): own* T;
}
