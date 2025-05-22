package src.game;

import java.util.HashMap;

// Sourced from https://stackoverflow.com/questions/9783020/bidirectional-map as a simple replacement for the third party google guava
public class BiMap<K, V> {
    public HashMap<K, V> map = new HashMap<K, V>();
    public HashMap<V, K> inversedMap = new HashMap<V, K>();

    public void put(K k, V v) {
        map.put(k, v);
        inversedMap.put(v, k);
    }

    public V get(K k) {
        return map.get(k);
    }

    public K getKey(V v) {
        return inversedMap.get(v);
    }

}
