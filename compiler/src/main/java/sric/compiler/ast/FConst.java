//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

/**
 *
 * @author yangjiandong
 */
public class FConst {
    public static final int Abstract   = 0x00000001;
    public static final int Const      = 0x00000002;
    public static final int Ctor       = 0x00000004;
    public static final int Enum       = 0x00000008;
    public static final int Facet      = 0x00000010;
    public static final int Unsafe     = 0x00000020;
    public static final int Getter     = 0x00000040;
    public static final int Internal   = 0x00000080;
    public static final int Mixin      = 0x00000100;
    public static final int Extern     = 0x00000200;
    public static final int Override   = 0x00000400;
    public static final int Private    = 0x00000800;
    public static final int Protected  = 0x00001000;
    public static final int Public     = 0x00002000;
    public static final int Setter     = 0x00004000;
    //public static final int Static     = 0x00008000;
    public static final int ExternC    = 0x00010000;
    public static final int Noncopyable= 0x00020000;
    public static final int Virtual    = 0x00040000;
    public static final int Struct     = 0x00080000;
    public static final int Extension  = 0x00100000;
    public static final int Mutable    = 0x00200000;
    public static final int Readonly   = 0x00400000;
    public static final int Async      = 0x00800000;
    public static final int Overload   = 0x01000000;
    public static final int Closure    = 0x02000000;
    public static final int Throws     = 0x04000000;
    public static final int Reflect    = 0x08000000;
    public static final int Inline     = 0x10000000;
    public static final int Packed     = 0x20000000;
    public static final int ConstExpr  = 0x40000000;
    public static final int Operator   = 0x80000000;
}
