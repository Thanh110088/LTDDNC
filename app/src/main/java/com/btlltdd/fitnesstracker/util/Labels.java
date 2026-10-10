package com.btlltdd.fitnesstracker.util;

import com.btlltdd.fitnesstracker.model.User;

import java.util.ArrayList;
import java.util.List;

public final class Labels {
    private Labels() {
    }

    public static final class Item {
        public final String key;
        public final String label;

        public Item(String key, String label) {
            this.key = key;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static String goal(String key) {
        if ("lose_weight".equals(key)) return "Giảm cân";
        if ("gain_muscle".equals(key)) return "Tăng cơ";
        if ("endurance".equals(key)) return "Sức bền";
        return "Duy trì";
    }

    public static String muscle(String key) {
        if ("chest".equals(key)) return "Ngực";
        if ("back".equals(key)) return "Lưng";
        if ("legs".equals(key)) return "Chân";
        if ("shoulders".equals(key)) return "Vai";
        if ("arms".equals(key)) return "Tay";
        if ("core".equals(key)) return "Bụng";
        if ("cardio".equals(key)) return "Cardio";
        return "Toàn thân";
    }

    public static String difficulty(String key) {
        if ("intermediate".equals(key)) return "Trung bình";
        if ("advanced".equals(key)) return "Nâng cao";
        return "Mới bắt đầu";
    }

    public static String equipment(String key) {
        if ("dumbbell".equals(key)) return "Tạ đơn";
        if ("barbell".equals(key)) return "Tạ đòn";
        if ("machine".equals(key)) return "Máy";
        if ("band".equals(key)) return "Dây kháng lực";
        if ("mat".equals(key)) return "Thảm";
        return "Không dụng cụ";
    }

    public static String repeat(String key) {
        if ("daily".equals(key)) return "Hằng ngày";
        if ("weekly".equals(key)) return "Hằng tuần";
        return "Một lần";
    }

    public static String role(User user) {
        return user.isAdmin() ? "Quản trị" : "Người dùng";
    }

    public static List<Item> goals() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("lose_weight", "Giảm cân"));
        items.add(new Item("gain_muscle", "Tăng cơ"));
        items.add(new Item("maintain", "Duy trì"));
        items.add(new Item("endurance", "Sức bền"));
        return items;
    }

    public static List<Item> muscles(boolean includeAll) {
        List<Item> items = new ArrayList<>();
        if (includeAll) items.add(new Item("", "Mọi nhóm cơ"));
        items.add(new Item("chest", "Ngực"));
        items.add(new Item("back", "Lưng"));
        items.add(new Item("legs", "Chân"));
        items.add(new Item("shoulders", "Vai"));
        items.add(new Item("arms", "Tay"));
        items.add(new Item("core", "Bụng"));
        items.add(new Item("cardio", "Cardio"));
        items.add(new Item("full_body", "Toàn thân"));
        return items;
    }

    public static List<Item> levels(boolean includeAll) {
        List<Item> items = new ArrayList<>();
        if (includeAll) items.add(new Item("", "Mọi mức"));
        items.add(new Item("beginner", "Mới bắt đầu"));
        items.add(new Item("intermediate", "Trung bình"));
        items.add(new Item("advanced", "Nâng cao"));
        return items;
    }

    public static List<Item> equipment(boolean includeAll) {
        List<Item> items = new ArrayList<>();
        if (includeAll) items.add(new Item("", "Mọi dụng cụ"));
        items.add(new Item("none", "Không dụng cụ"));
        items.add(new Item("dumbbell", "Tạ đơn"));
        items.add(new Item("barbell", "Tạ đòn"));
        items.add(new Item("machine", "Máy"));
        items.add(new Item("band", "Dây kháng lực"));
        items.add(new Item("mat", "Thảm"));
        return items;
    }

    public static List<Item> repeats() {
        List<Item> items = new ArrayList<>();
        items.add(new Item("once", "Một lần"));
        items.add(new Item("daily", "Hằng ngày"));
        items.add(new Item("weekly", "Hằng tuần"));
        return items;
    }

    public static int indexOf(List<Item> items, String key) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).key.equals(key)) {
                return i;
            }
        }
        return 0;
    }

    public static String key(android.widget.Spinner spinner) {
        Object selected = spinner.getSelectedItem();
        if (selected instanceof Item) {
            return ((Item) selected).key;
        }
        return "";
    }
}
