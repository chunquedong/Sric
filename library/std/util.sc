

externc fun printf(format: raw*const Int8, args: ...): Int;

extern fun alloc$<T>(): own* T;

extern struct WeakPtr$<T> {
    fun init(p: own* T);
    fun lock(): own* T;
}


extern fun share$<T>(p: own* T): own* T;

extern unsafe fun rawToOwn$<T>(p: raw* T): own* T;

extern fun refToOwn$<T>(p: ref* T): own*? T;

extern unsafe fun rawToRef$<T>(p: raw* T): ref* T;

