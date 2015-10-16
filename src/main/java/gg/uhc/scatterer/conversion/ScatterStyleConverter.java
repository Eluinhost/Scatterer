package gg.uhc.scatterer.conversion;

import com.google.common.base.Joiner;
import gg.uhc.flagcommands.joptsimple.ValueConversionException;
import gg.uhc.flagcommands.joptsimple.ValueConverter;
import gg.uhc.scatterer.ScatterStyle;

public class ScatterStyleConverter implements ValueConverter<ScatterStyle> {

    protected static final String VALUES = Joiner.on("|").join(ScatterStyle.values());

    @Override
    public ScatterStyle convert(String value) {
        try {
            return ScatterStyle.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValueConversionException("Unknown scatter style: " + value, e);
        }
    }

    @Override
    public Class<ScatterStyle> valueType() {
        return ScatterStyle.class;
    }

    @Override
    public String valuePattern() {
        return VALUES;
    }
}
