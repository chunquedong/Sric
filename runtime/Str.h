#ifndef _SRIC_STRING_H_
#define _SRIC_STRING_H_

#include <string>
#include "DArray.h"

namespace sric
{

class String {
    std::string str;
public:
    String(const char* c) : str(c) {}
    String(const std::string& c) : str(c) {}

    const std::string& cpp_str() const {
        return str;
    }
    const char* c_str() const {
        return str.c_str();
    }
    int size() const {
        return str.size();
    }
    char get(int i) const {
        return str[i];
    }
    char operator[](int i) const {
        return get(i);
    }

    int hashCode();
    bool iequals(RefPtr<String> other);
    bool contains(RefPtr<String> s);
    bool startsWith(RefPtr<String> s);
    bool endsWith(RefPtr<String> s);
    int find(RefPtr<String> s, int start = 0);

    String& operator+(RefPtr<String> other) {
        plus(other);
    }
    String& plus(RefPtr<String> other);
    void add(const char* cstr);

    void replace(RefPtr<String> src, RefPtr<String> dst);
    DArray<String> split(RefPtr<String> sep);
    String substr(int pos, int len = -1);

    void trimEnd();
    void trimStart();
    void trim() { trimStart(); trimEnd(); }
    void removeLastChar();

    String toLower();
    String toUpper();

    int toInt();
    int64_t toLong();
    float toFloat();
    double toDouble();

    static String fromInt(int i);
    static String fromLong(int64_t i);
    static String fromDouble(double f);
    static String fromFloat(float f);

    /**
    * 'printf' style format
    */
    static String format(const char* fmt, ...);
};


inline String asStr(const char* cstr) {
    return String(cstr);
}

}
#endif