package br.com.naheroback.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class ShuffleUtils {

    private ShuffleUtils() {}

    public static <T> List<T> shuffleDifferentOrder(List<T> items) {
        if (items.size() < 2) return new ArrayList<>(items);

        List<Integer> order = new ArrayList<>(IntStream.range(0, items.size()).boxed().toList());
        do {
            Collections.shuffle(order);
        } while (isIdentity(order));

        return order.stream().map(items::get).toList();
    }

    private static boolean isIdentity(List<Integer> order) {
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i) != i) return false;
        }
        return true;
    }
}
