package org.andes.lock.core;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 锁池
 */
class LockPool {

    private final Cache<Object, Lock> cache;

    LockPool() {
        this.cache = CacheBuilder.newBuilder().maximumSize(Integer.MAX_VALUE)
                .expireAfterAccess(Duration.ofSeconds(60))
                .build();
    }

    Lock valueOf(Object lockObj) {
        try {
            return cache.get(lockObj, ReentrantLock::new);
        } catch (Exception ex) {
            throw new AndesException(ex);
        }
    }

    LockChain newChain(Object... args) {
        return new LockChain(createOrders(args)
                .stream()
                .map(e -> e.arg)
                .map(this::valueOf)
                .toList()
        );
    }

    private Map<Class<?>, Integer> buildParamSort(Object... args) {
        var map = new HashMap<Class<?>, Integer>(args.length);
        Class<?> pre = null;
        int order = 0;
        for (var arg : args) {
            if (map.containsKey(arg.getClass())) {
                continue;
            }
            if (pre != arg.getClass()) {
                order++;
                pre = arg.getClass();
            }
            map.put(arg.getClass(), order);
        }
        return map;
    }

    private List<ParamOrder> createOrders(Object... args) {
        List<ParamOrder> orders = new ArrayList<>(args.length);
        int index = 0;
        var paramSort = buildParamSort(args);
        for (Object arg : args) {
            var order = paramSort.get(arg.getClass());
            orders.add(new ParamOrder(arg, order, arg.hashCode(), index++));
        }
        orders.sort(ParamOrder::compareTo);
        return orders;
    }

    /**
     * 参数排序
     */
    record ParamOrder(Object arg, int order, int subOrder, int thirdOrder) implements Comparable<ParamOrder> {
        @Override
        public int compareTo(ParamOrder o) {
            var first = Integer.compare(order, o.order);
            if (first != 0) {
                return first;
            }
            var second = Integer.compare(subOrder, o.subOrder);
            if (second != 0) {
                return second;
            }
            return Integer.compare(thirdOrder, o.thirdOrder);
        }
    }

}
