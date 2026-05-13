package util;

public class MyHashMap<K, V> {
  private Object[] hashArray;
  private int size;
  private MyHashSet<K> keySet;

  public MyHashMap() {
    hashArray = new Object[26 * 26 * 26 + 26 * 26 + 26];
    size = 0;
    keySet = new MyHashSet<>();
  }

  @SuppressWarnings("unchecked")
  public V put(K key, V value) {
    if (keySet.add(key)) {
      size++;
    }
    V ret = (V) hashArray[key.hashCode()];
    hashArray[key.hashCode()] = value;
    return ret;
  }

  @SuppressWarnings("unchecked")
  public V get(Object o) {
    return (V) hashArray[o.hashCode()];
  }

  @SuppressWarnings("unchecked")
  public V remove(Object o) {
    V ret = (V) hashArray[o.hashCode()];
    hashArray[o.hashCode()] = null;
    if (keySet.remove(o)) {
      size--;
    }
    return ret;
  }

  public int size() {
    return size;
  }

  public MyHashSet<K> keySet() {
    return keySet;
  }

  public String toString() {
    String str = "[";
    for (K key : keySet.toDLList()) {
      str += key + " - " + hashArray[key.hashCode()] + ",";
    }
    str += "]";
    return str;
  }
}
