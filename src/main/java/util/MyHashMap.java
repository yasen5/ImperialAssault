package util;

import java.io.Serializable;

public class MyHashMap<K, V> implements Serializable {
  private static final int DEFAULT_CAPACITY = 128;
  private static final double MAX_LOAD_FACTOR = 0.65;

  private Entry<K, V>[] hashArray;
  private int size;
  private MyHashSet<K> keySet;

  public static record Entry<K, V>(K key, V value) implements Serializable {
    public K getKey() {
      return key;
    }

    public V getValue() {
      return value;
    }
  }

  @SuppressWarnings("unchecked")
  public MyHashMap() {
    hashArray = (Entry<K, V>[]) new Entry[DEFAULT_CAPACITY];
    size = 0;
    keySet = new MyHashSet<>();
  }

  public MyHashMap(Class<K> keyType) {
    this();
  }

  @SuppressWarnings("unchecked")
  public MyHashMap(MyHashMap<? extends K, ? extends V> other) {
    this();
    if (other != null) {
      for (Entry<? extends K, ? extends V> entry : other.entrySet()) {
        put(entry.key(), entry.value());
      }
    }
  }

  public V put(K key, V value) {
    ensureCapacity();
    int slot = findSlot(key);
    Entry<K, V> current = hashArray[slot];
    V previous = current == null ? null : current.value();
    hashArray[slot] = new Entry<>(key, value);
    if (current == null) {
      keySet.add(key);
      size++;
    }
    return previous;
  }

  public V get(Object key) {
    int slot = findSlot(key);
    Entry<K, V> entry = hashArray[slot];
    return entry == null ? null : entry.value();
  }

  public V remove(Object key) {
    int slot = findSlot(key);
    Entry<K, V> entry = hashArray[slot];
    if (entry == null) {
      return null;
    }
    V previous = entry.value();
    hashArray[slot] = null;
    keySet.remove(key);
    size--;
    rebuild(hashArray);
    return previous;
  }

  public boolean containsKey(Object key) {
    return hashArray[findSlot(key)] != null;
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public MyHashSet<K> keySet() {
    return keySet;
  }

  public MyArrayList<V> values() {
    MyArrayList<V> values = new MyArrayList<>();
    for (Entry<K, V> entry : entrySet()) {
      values.add(entry.value());
    }
    return values;
  }

  public MyArrayList<Entry<K, V>> entrySet() {
    MyArrayList<Entry<K, V>> entries = new MyArrayList<>();
    for (Entry<K, V> entry : hashArray) {
      if (entry != null) {
        entries.add(entry);
      }
    }
    return entries;
  }

  public String toString() {
    String str = "[";
    for (Entry<K, V> entry : entrySet()) {
      str += entry.key() + " - " + entry.value() + ",";
    }
    str += "]";
    return str;
  }

  private int findSlot(Object key) {
    int slot = Math.floorMod(key.hashCode(), hashArray.length);
    while (hashArray[slot] != null && !hashArray[slot].key().equals(key)) {
      slot = (slot + 1) % hashArray.length;
    }
    return slot;
  }

  private void ensureCapacity() {
    if ((size + 1) / (double) hashArray.length > MAX_LOAD_FACTOR) {
      rebuildInto(hashArray.length * 2);
    }
  }

  private void rebuild(Entry<K, V>[] oldArray) {
    rebuildInto(Math.max(DEFAULT_CAPACITY, oldArray.length), oldArray);
  }

  private void rebuildInto(int capacity) {
    rebuildInto(capacity, hashArray);
  }

  @SuppressWarnings("unchecked")
  private void rebuildInto(int capacity, Entry<K, V>[] oldArray) {
    Entry<K, V>[] entries = oldArray;
    hashArray = (Entry<K, V>[]) new Entry[capacity];
    keySet = new MyHashSet<>();
    int oldSize = size;
    size = 0;
    for (Entry<K, V> entry : entries) {
      if (entry != null) {
        put(entry.key(), entry.value());
      }
    }
    size = oldSize;
  }
}
