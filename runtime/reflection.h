
#ifndef SRIC_REFLECT_H_
#define SRIC_REFLECT_H_

#include "Ptr.h"

namespace sric {
	struct Module;
	void registModule(Module* m);
	RefPtr<Module> findModule(const char* name);
}

#define SC_AUTO_REGIST_MODULE(name) \
	class ScAutoRegistModule ## name { \
	public:\
		ScAutoRegistModule ## name () {\
			registReflection_ ## name();\
		}\
	};\
	ScAutoRegistModule ## name __scAutoRegistModuleInstance;

#endif