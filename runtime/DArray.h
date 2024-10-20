#ifndef _SRIC_DARRAY_H_
#define _SRIC_DARRAY_H_

#include "common.h"
#include <vector>
#include "Ptr.h"

namespace sric
{
template <typename T>
class DArray {
    std::vector<T> data;
public:
    void resize(int size) {
        data.resize(size);
    }

    T& operator[](int i) {
        sric::sc_assert(i >= 0 && i < data.size(), "index out of array");
        return data[i];
    }

    const T& operator[](int i) const {
        sric::sc_assert(i >= 0 && i < data.size(), "index out of array");
        return data[i];
    }

    sric::RefPtr<T> get(int i) {
        sric::sc_assert(i >= 0 && i < data.size(), "index out of array");
        return RefPtr<T>(&data[i]);
    }

    const sric::RefPtr<T> get(int i) const {
        sric::sc_assert(i >= 0 && i < data.size(), "index out of array");
        return RefPtr<T>(&data[i]);
    }

    void set(int i, const T& d) {
        sric::sc_assert(i >= 0 && i < data.size(), "index out of array");
        data[i] = d;
    }

    void add(T d) {
        data.push_back(d);
    }

    //void add(const T& d) {
    //    data.push_back(d);
    //}

    int size() {
        return data.size();
    }

    void removeAt(int i) {
        data.erase(data.begin() + i);
    }

    void reserve(int capacity) {
        data.reserve(capacity);
    }

};
}
#endif