#ifndef _SRIC_COMMON_H_
#define _SRIC_COMMON_H_


namespace sric
{

inline void sc_assert(bool c, const char *msg = "") {
    if (!c) {
        printf("ERROR: %s\n", msg);
        abort();
    }
}

}

#endif