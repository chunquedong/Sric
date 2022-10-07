// To change this License template, choose Tools / Templates
// and edit Licenses / FanDefaultLicense.txt
//
// History:
//   2022Äê10ÔÂ6ÈÕ yangjiandong Creation
//


mixin Symbol {
    abstract Str name()
}

**
** Scope
**
mixin Scope
{
  abstract Scope? parentScope()
  abstract Symbol? doFindSymbol(Str name)
  
  Symbol? findSymbol(Str name) {
    sym := doFindSymbol(name)
    if (sym == null && parentScope != null) {
        return parentScope.findSymbol(name)
    }
    return sym
  }
  
  
}
