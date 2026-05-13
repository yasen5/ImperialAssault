package game;

import util.MyHashMap;


// Sourced from https://stackoverflow.com/questions/9783020/bidirectional-map as a simple replacement for the third party google guava
public class BiMap<K, V> {
    public MyHashMap<K, V> map = new MyHashMap<K, V>();
    public MyHashMap<V, K> inversedMap = new MyHashMap<V, K>();

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
