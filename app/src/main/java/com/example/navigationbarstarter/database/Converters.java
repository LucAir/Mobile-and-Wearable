package com.example.navigationbarstarter.database;

import androidx.room.TypeConverter;

import com.example.navigationbarstarter.database.item.Type;

import java.util.ArrayList;
import java.util.List;

public class Converters {

    // Convert List<Long> to a single String (comma-separated)
    @TypeConverter
    public static String fromList(List<Long> list) {
        if (list == null || list.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Long l : list) {
            sb.append(l).append(",");
        }
        sb.deleteCharAt(sb.length() - 1); // remove last comma
        return sb.toString();
    }

    // Convert String back to List<Long>
    @TypeConverter
    public static List<Long> toList(String data) {
        List<Long> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;
        String[] arr = data.split(",");
        for (String s : arr) {
            list.add(Long.parseLong(s));
        }
        return list;
    }

    @TypeConverter
    public static String fromType(Type type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static Type toType(String type) {
        return type == null ? null : Type.valueOf(type);
    }
}
