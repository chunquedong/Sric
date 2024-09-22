#include "Ptr.h"
#include "Refable.h"

using namespace sric;

namespace sric {
	//template<typename T>
	//OwnPtr<T> alloc() {
	//	return sric::alloc<T>();
	//}

	template<typename T>
	T* unsafeAlloc() {
		return new T();
	}

	template<typename T>
	void unsafeFree(T* p) {
		delete p;
	}
}