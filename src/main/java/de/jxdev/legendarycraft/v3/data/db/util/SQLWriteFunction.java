package de.jxdev.legendarycraft.v3.data.db.util;

import java.sql.SQLException;

@FunctionalInterface
public interface SQLWriteFunction<T> {
    void apply(T t) throws SQLException;
}