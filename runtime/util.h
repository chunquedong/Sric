
#ifndef SRIC_UTIL_H_
#define SRIC_UTIL_H_

#include "Ptr.h"
#include "Refable.h"

namespace sric {


	template<typename T>
	T* unsafeAlloc() {
		return new T();
	}

	template<typename T>
	void unsafeFree(T* p) {
		delete p;
	}

	template<typename T>
	bool ptr_is(void* p) {
		return dynamic_cast<T*>(p) != nullptr;
	}

	template<typename T, typename F>
	bool ptr_is(F p) {
		return dynamic_cast<T*>(p.get()) != nullptr;
	}
}

#endif