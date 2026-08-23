package configgen.genlua;

import static configgen.value.CfgValue.VTable;

class Ctx {
    private final AContext aContext;
    private final VTable vTable;
    private final CtxShared ctxShared;
    private final CtxName ctxName;

    Ctx(AContext aContext, VTable vtable) {
        this.aContext = aContext;
        vTable = vtable;
        ctxName = new CtxName(aContext);
        ctxShared = new CtxShared(aContext);
    }

    AContext aCtx() {
        return aContext;
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
