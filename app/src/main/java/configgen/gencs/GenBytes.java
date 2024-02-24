package configgen.gencs;

import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.LangSwitchRuntime;
import configgen.gen.Parameter;
import configgen.util.CachedFileOutputStream;
import configgen.util.XorCipherOutputStream;
import configgen.value.CfgValue;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class GenBytes extends Generator {
    private final File file;
    private final String cipher;

    private LangSwitchRuntime langSwitchRuntime;

    public GenBytes(Parameter parameter) {
        super(parameter);
        file = new File(parameter.get("file", "config.bytes"));
        cipher = parameter.get("cipher", "");
        parameter.end();
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue(tag);

        if (ctx.nullableLangSwitch() != null) {
            langSwitchRuntime = new LangSwitchRuntime(ctx.nullableLangSwitch());
        }

        try (CachedFileOutputStream stream = new CachedFileOutputStream(file, 2048 * 1024)) {
            OutputStream st = stream;
            if (!cipher.isEmpty()) {
                st = new XorCipherOutputStream(stream, cipher);
            }
            this.stream = new DataOutputStream(st);
            for (VTable vTable : cfgValue.sortedTables()) {
                addVTable(vTable);
            }
        }
    }

    private DataOutputStream stream;
    private final byte[] writeBuffer = new byte[8];

    private void addVTable(VTable vTable) {
        if (langSwitchRuntime != null) {
            langSwitchRuntime.enterTable(vTable.name());
        }

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
            case VText vText -> {
                if (langSwitchRuntime != null) {
                    //这里全部写进去，作为一个Text的Bean
                    String[] i18nStrings = langSwitchRuntime.findAllLangText(vText.value());
                    for (String i18nStr : i18nStrings) {
                        addString(i18nStr);
                    }
                } else {
                    addString(vText.value());
                }
            }
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
            stream.writeBoolean(v);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addInt(int v) {
        try {
            stream.write((v) & 0xFF);
            stream.write((v >>> 8) & 0xFF);
            stream.write((v >>> 16) & 0xFF);
            stream.write((v >>> 24) & 0xFF);
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
            stream.write(writeBuffer);
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
            stream.write(b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
