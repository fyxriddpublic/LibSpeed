package com.fyxridd.lib.speed.config;

import com.fyxridd.lib.core.api.config.basic.ListType;
import com.fyxridd.lib.core.api.config.basic.Path;
import com.fyxridd.lib.core.api.config.convert.ConfigConvert;
import com.fyxridd.lib.core.api.config.convert.ListConvert;
import com.fyxridd.lib.core.api.config.limit.Min;
import com.fyxridd.lib.core.api.lang.LangConverter;
import com.fyxridd.lib.core.api.lang.LangGetter;

import java.util.List;

public class SpeedConfig {
    private class ShortLevelsConverter implements ListConvert.ListConverter<Integer[]> {
        @Override
        public Integer[] convert(String plugin, List list) {
            Integer[] result = new Integer[list.size()];
            for (int index=0;index<list.size();index++) {
                result[index] = (Integer) list.get(index);
            }
            return result;
        }
    }

    @Path("short.clear")
    @Min(1)
    private int shortClearInterval;

    @Path("short.levels")
    @ListConvert(value = ShortLevelsConverter.class, listType = ListType.Integer)
    private Integer[] shortLevels;

    @Path("side.enable")
    private boolean sideEnable;

    @Path("side.clear")
    @Min(1)
    private long sideClearTimeout;

    @Path("lang")
    @ConfigConvert(LangConverter.class)
    private LangGetter lang;

    public int getShortClearInterval() {
        return shortClearInterval;
    }

    public Integer[] getShortLevels() {
        return shortLevels;
    }

    public boolean isSideEnable() {
        return sideEnable;
    }

    public long getSideClearTimeout() {
        return sideClearTimeout;
    }

    public LangGetter getLang() {
        return lang;
    }
}
