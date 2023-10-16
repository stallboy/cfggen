package configgen.genlua;

import java.util.ArrayList;
import java.util.List;

import static configgen.value.CfgValue.*;

class ValueShared {

    private final List<ValueSharedLayer> layers = new ArrayList<>();
    private final VTable vTable;

    ValueShared(VTable vtable) {
        vTable = vtable;
    }

    void iterateShared() {
        ValueSharedLayer layer1 = new ValueSharedLayer(this);
        for (VStruct vStruct : vTable.valueList()) {
            layer1.visitVStruct(vStruct);
        }
        layers.add(layer1);

        ValueSharedLayer currLayer = layer1;
        while (true) {
            ValueSharedLayer nextLayer = new ValueSharedLayer(this);

            List<ValueSharedLayer.CompositeValueCnt> currLayerCopy = new ArrayList<>(currLayer.getCompositeValueToCnt().values());
            for (ValueSharedLayer.CompositeValueCnt vc : currLayerCopy) {
                if (!vc.isTraversed()) {
                    nextLayer.visit(vc.getFirst());
                    vc.setTraversed();
                }
            }

            if (!nextLayer.getCompositeValueToCnt().isEmpty()) {
                layers.add(nextLayer);
                currLayer = nextLayer;
            } else {
                break;  // 直到下一层，再也没有共用的结构了，就退出
            }
        }
    }

    List<ValueSharedLayer> getLayers() {
        return layers;
    }

    ValueSharedLayer.CompositeValueCnt remove(CompositeValue v) {
        for (ValueSharedLayer layer : layers) {
            ValueSharedLayer.CompositeValueCnt old = layer.getCompositeValueToCnt().remove(v);
            if (old != null) {
                return old;
            }
        }
        return null;
    }

}
