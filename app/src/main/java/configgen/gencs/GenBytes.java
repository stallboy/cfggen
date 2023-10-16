package configgen.gencs;

import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.CachedFileOutputStream;
import configgen.value.CfgValue;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class GenBytes extends Generator {

    private final File file;

    public GenBytes(Parameter parameter) {
        super(parameter);
        file = new File(parameter.get("file", "config.bytes", "文件名"));
        parameter.end();
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);

        try (CachedFileOutputStream stream = new CachedFileOutputStream(file, 2048 * 1024)) {
            this.byter = new DataOutputStream(stream);
            for (VTable vTable : cfgValue.sortedTables()) {
                addVTable(vTable);
            }
        }
    }

    private DataOutputStream byter;
    private final byte[] writeBuffer = new byte[8];

    private void addVTable(VTable vTable) {
        addString(vTable.name());
        addInt(vTable.valueList().size());
        for (VStruct v : vTable.valueList()) {
            addValue(v);
        }
    }

    private void addValue(Value value) {
        switch (value) {
            case VBool vBool -> addBool(vBool.value());
            case VInt vInt -> addInt(vInt.value());
            case VLong vLong -> addLong(vLong.value());
            case VFloat vFloat -> addFloat(vFloat.value());
            case VString vStr -> addString(vStr.value());
            case VText vText -> addString(vText.value());
            case VStruct vStruct -> {
                for (Value v : vStruct.values()) {
                    addValue(v);
                }
            }
            case VInterface vInterface -> {
                addString(vInterface.child().name());
                for (Value v : vInterface.child().values()) {
                    addValue(v);
                }
            }
            case VList vList -> {
                addInt(vList.valueList().size());
                for (SimpleValue v : vList.valueList()) {
                    addValue(v);
                }
            }
            case VMap vMap -> {
                addInt(vMap.valueMap().size());
                for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
                    addValue(e.getKey());
                    addValue(e.getValue());
                }
            }
        }
    }


    private void addBool(boolean v) {
        try {
            byter.writeBoolean(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addInt(int v) {
        try {
            byter.write((v) & 0xFF);
            byter.write((v >>> 8) & 0xFF);
            byter.write((v >>> 16) & 0xFF);
            byter.write((v >>> 24) & 0xFF);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addLong(long v) {
        writeBuffer[0] = (byte) (v);
        writeBuffer[1] = (byte) (v >>> 8);
        writeBuffer[2] = (byte) (v >>> 16);
        writeBuffer[3] = (byte) (v >>> 24);
        writeBuffer[4] = (byte) (v >>> 32);
        writeBuffer[5] = (byte) (v >>> 40);
        writeBuffer[6] = (byte) (v >>> 48);
        writeBuffer[7] = (byte) (v >>> 56);
        try {
            byter.write(writeBuffer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addFloat(float v) {
        addInt(Float.floatToIntBits(v));
    }

    private void addString(String v) {
        try {
            byte[] b = v.getBytes(StandardCharsets.UTF_8);
            addInt(b.length);
            byter.write(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}
