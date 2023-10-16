package configgen.genlua;

import static configgen.value.CfgValue.VTable;

class Ctx {
    private final VTable vTable;
    private final CtxShared ctxShared;
    private final CtxName ctxName;


    Ctx(VTable vtable) {
        vTable = vtable;
        ctxName = new CtxName();
        ctxShared = new CtxShared();
    }


    CtxName getCtxName() {
        return ctxName;
    }

    CtxShared getCtxShared() {
        return ctxShared;
    }

    void parseShared() {
        ctxShared.parseShared(this);
    }


}
