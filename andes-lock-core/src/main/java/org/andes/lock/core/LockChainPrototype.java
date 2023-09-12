package org.andes.lock.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


}
