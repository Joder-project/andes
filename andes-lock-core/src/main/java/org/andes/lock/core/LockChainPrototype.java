package org.andes.lock.core;

import java.util.*;

/**
 * 锁链链
 */
class LockChainPrototype {

    final LockPool lockPool;
    final String methodName;
    final Map<Class<?>, Integer> paramSort;
    final List<Wrapper> params;

    LockChainPrototype(LockPool lockPool, String methodName, Class<?>... classes) {
        this.lockPool = lockPool;
        this.methodName = methodName;
        var map = new HashMap<Class<?>, Integer>(classes.length);
        Class<?> pre = null;
        int order = 0;
        for (Class<?> clazz : classes) {
            if (map.containsKey(clazz)) {
                continue;
            }
            if (pre != clazz) {
                order++;
                pre = clazz;
            }
            map.put(clazz, order);
        }
        this.paramSort = map;

        this.params = paramSort.keySet()
                .stream()
                .map(e -> new Wrapper(e, paramSort.get(e)))
                .sorted(Comparator.comparing(e -> e.order))
                .toList();
    }

    LockChain newChain(Object... args) {
        return new LockChain(createOrders(args)
                .stream()
                .map(e -> e.arg)
                .map(lockPool::valueOf)
                .toList()
        );
    }

    /**
     * 是否锁顺序冲突
     */
    boolean isConflict(LockChainPrototype other) {
        if (params.isEmpty()) {
            return false;
        }
        int max = -1;
        for (int i = 0; i < other.params.size(); i++) {
            if (paramSort.containsKey(other.params.get(i).clazz)) {
                var sort = paramSort.get(other.params.get(i).clazz);
                if (sort < max) {
                    return true;
                }
                max = sort;
            }
        }
        return false;
    }

    private List<ParamOrder> createOrders(Object... args) {
        List<ParamOrder> orders = new ArrayList<>(args.length);
        int index = 0;
        for (Object arg : args) {
            var order = paramSort.get(arg.getClass());
            orders.add(new ParamOrder(arg, order, arg.hashCode(), index++));
        }
        orders.sort(ParamOrder::compareTo);
        return orders;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("| ");
        for (var temp : params) {
            sb.append(temp.clazz.getName()).append(", ");
        }
        sb.append("|\n");
        return sb.toString();
    }

    record Wrapper(Class<?> clazz, int order) {

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
