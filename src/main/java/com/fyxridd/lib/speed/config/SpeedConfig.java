package com.fyxridd.lib.speed.config;

import com.fyxridd.lib.config.api.basic.ListType;
import com.fyxridd.lib.config.api.basic.Path;
import com.fyxridd.lib.config.api.convert.ListConvert;
import com.fyxridd.lib.config.api.limit.Min;

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
}