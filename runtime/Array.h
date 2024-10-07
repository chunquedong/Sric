#ifndef _SRIC_ARRAY_H_
#define _SRIC_ARRAY_H_

namespace sric
{
template<typename T, int n>
class Array {
    T data[n];
public:
    T& operator[](int i) {
        sc_assert(i >= 0 && i < n, "index out of array");
        return data[i];
    }

    const T& operator[](int i) const {
        sc_assert(i >= 0 && i < n, "index out of array");
        return data[i];
    }
};
}
#endif