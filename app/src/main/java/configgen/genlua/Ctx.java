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

    public VTable vTable() {
        return vTable;
    }

    CtxName ctxName() {
        return ctxName;
    }

    CtxShared ctxShared() {
        return ctxShared;
    }

    void parseShared() {
        ctxShared.parseShared(this);
    }


}
