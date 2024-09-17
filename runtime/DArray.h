#ifndef DARRAY_H_
#define DARRAY_H_

#include "common.h"
#include <vector>

namespace sstd
{
template <typename T>
class DArray {
    std::vector<T> data;
public:
    void resize(int size) {
        data.resize(size);
    }

    T operator[](int i) { return get(i); }

    const T operator[](int i) const { return get(i); }

    T get(int i) {
        sric::sc_assert(i >= 0 && i < data.size(), "index out of array");
        return data[i];
    }

    const T get(int i) const {
        sric::sc_assert(i >= 0 && i < data.size(), "index out of array");
        return data[i];
    }

    void set(int i, T d) {
        sric::sc_assert(i >= 0 && i < data.size(), "index out of array");
        data[i] = d;
    }

    void add(T d) {
        data.push_back(d);
    }

    int size() {
        return data.size();
    }

    void removeAt(int i) {
        data.erase(data.begin() + i);
    }

    void reserve(int capacity) {
        data.reserve(capacity);
    }

    sric::RefPtr<T> getPtr(int i) {
        return RefPtr(&data[i]);
    }
};
}
#endif