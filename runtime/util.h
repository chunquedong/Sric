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
}