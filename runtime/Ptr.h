/*
 * Copyright (c) 2012-2016, chunquedong
 *
 * This file is part of cppfan project
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE version 3.0
 *
 * History:
 *   2012-12-23  Jed Young  Creation
 */
#ifndef PTR_H_
#define PTR_H_

#include <cstdio>
#include <cstdlib>
#include <type_traits>

#include "Refable.h"

namespace sric
{

class Refable;

template<typename T>
class OwnPtr {
    T* pointer;

public:
    OwnPtr() : pointer(nullptr) {
    }

    explicit OwnPtr(T* p) : pointer(p) {
    }

    ~OwnPtr() {
        clear();
    }

    OwnPtr(const OwnPtr& other) = delete;

    OwnPtr(OwnPtr&& other) {
        if (other.pointer) {
            pointer = other.pointer;
            other.pointer = nullptr;
        }
        else {
            pointer = nullptr;
        }
    }

    template <class U>
    OwnPtr(OwnPtr<U>&& other) {
        if (other.pointer) {
            pointer = other.pointer;
            other.pointer = nullptr;
        }
        else {
            pointer = nullptr;
        }
    }

    OwnPtr& operator=(const OwnPtr& other) = delete;

    OwnPtr& operator=(OwnPtr&& other) {
        T* toDelete = pointer;

        if (other.pointer) {
            pointer = other.pointer;
            other.pointer = nullptr;
        }
        else {
            pointer = nullptr;
        }

        if (toDelete) {
            doFree(toDelete);
        }
        return *this;
    }

    T* operator->() const { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    T* operator->() { sc_assert(pointer != nullptr, "try deref null pointer"); return pointer; }

    T& operator*() { sc_assert(pointer != nullptr, "try deref null pointer"); return *pointer; }

    T* get() const { return pointer; }

    bool isNull() { return pointer == nullptr; }

    void clear() {
        //TODO
        //doFree(pointer);
        pointer = nullptr;
    }

    T* take() {
        T* p = pointer;
        pointer = nullptr;
        return p;
    }

    void swap(OwnPtr& other) {
        T* p = pointer;
        pointer = other.pointer;
        other.pointer = p;
    }

    template <class U> OwnPtr<U> staticCastTo()
    {
        OwnPtr<U> copy(static_cast<U*>(take()));
        return copy;
    }

    template <class U> OwnPtr<U> dynamicCastTo()
    {
        OwnPtr<U> copy(dynamic_cast<U*>(take()));
        return copy;
    }

    OwnPtr<T> share() {
        if (pointer)
            pointer->addRef();
        return OwnPtr<T>(pointer);
    }
};

template <class T>
OwnPtr<T> share(OwnPtr<T> p) {
    T* pointer = p.get();
    if (pointer)
        pointer->addRef();
    return OwnPtr<T>(pointer);
}

template <class T>
OwnPtr<T> rawToOwn(T* ptr) {
    ptr->addRef();
    return OwnPtr<T>(ptr);
}

}
#endif
