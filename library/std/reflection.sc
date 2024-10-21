

struct Comment {
    readonly var type: Int;
    readonly var content : raw*const Int8;
}

struct Field {
    readonly var flags : Int;
    readonly var comments : DArray$<Comment>;
    readonly var name : raw*const Int8;
    readonly var offset : Int;
    readonly var pointer : raw* Void;
    readonly var fieldType : raw*const Int8;
    readonly var hasDefaultValue : Bool;
}

struct Func {
    readonly var flags : Int;
    readonly var comments : DArray$<Comment>;
    readonly var name : raw*const Int8;
    readonly var pointer : raw* Void;
    readonly var returnType: raw*const Int8;
    readonly var params : DArray$<Field>;
    readonly var genericParams : DArray$<raw*const Int8>;
}

struct Struct {
    readonly var flags : Int;
    readonly var comments : DArray$<Comment>;
    readonly var name : raw*const Int8;
    readonly var fields : DArray$<Field>;
    readonly var funcs : DArray$<Func>;
    readonly var inheritances : DArray$<raw*const Int8>;
    readonly var genericParams : DArray$<raw*const Int8>;
}

struct Module {
    readonly var name : raw*const Int8;
    readonly var version : raw*const Int8;

    readonly var fields : DArray$<Field>;
    readonly var funcs : DArray$<Func>;
    readonly var structs : DArray$<Struct>;
}

extern fun findModule(name : raw*const Int8) : ref*? Module;
