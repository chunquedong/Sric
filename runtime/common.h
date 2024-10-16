#ifndef _SRIC_COMMON_H_
#define _SRIC_COMMON_H_


namespace sric
{

#if SC_NO_CHECK
    #define sc_assert(c, m) 
#else
    inline void sc_assert(bool c, const char* msg) {
        if (!c) {
            printf("ERROR: %s\n", msg);
            abort();
        }
    }
#endif // SC_NO_CHECK

}

#endif