package util;

import java.io.Serializable;

public class MyHashMap<K, V> implements Serializable {
  private static final int DEFAULT_CAPACITY = 128;
  private static final double MAX_LOAD_FACTOR = 0.65;
  private static final Entry<?, ?> DELETED = new Entry<>(null, null);

  private Entry<K, V>[] hashArray;
  private int size;
  private int usedSlots;
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
    usedSlots = 0;
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
    int existingSlot = findExistingSlot(key);
    if (existingSlot >= 0) {
      Entry<K, V> current = hashArray[existingSlot];
      hashArray[existingSlot] = new Entry<>(key, value);
      return current.value();
    }
    ensureCapacity();
    int slot = findInsertSlot(key);
    Entry<K, V> current = hashArray[slot];
    if (current == null) {
      usedSlots++;
    }
    hashArray[slot] = new Entry<>(key, value);
    keySet.add(key);
    size++;
    return null;
  }

  public V get(Object key) {
    int slot = findExistingSlot(key);
    return slot < 0 ? null : hashArray[slot].value();
  }

  public V remove(Object key) {
    int slot = findExistingSlot(key);
    if (slot < 0) {
      return null;
    }
    Entry<K, V> entry = hashArray[slot];
    V previous = entry.value();
    hashArray[slot] = deletedEntry();
    keySet.remove(key);
    size--;
    compactIfSparse();
    return previous;
  }

  public boolean containsKey(Object key) {
    return findExistingSlot(key) >= 0;
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
      if (isActive(entry)) {
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

  private int findExistingSlot(Object key) {
    int slot = Math.floorMod(key.hashCode(), hashArray.length);
    while (hashArray[slot] != null) {
      if (hashArray[slot] != DELETED && hashArray[slot].key().equals(key)) {
        return slot;
      }
      slot = (slot + 1) % hashArray.length;
    }
    return -1;
  }

  private int findInsertSlot(Object key) {
    int slot = Math.floorMod(key.hashCode(), hashArray.length);
    int firstDeleted = -1;
    while (hashArray[slot] != null) {
      if (hashArray[slot] == DELETED) {
        if (firstDeleted < 0) {
          firstDeleted = slot;
        }
      } else if (hashArray[slot].key().equals(key)) {
        return slot;
      }
      slot = (slot + 1) % hashArray.length;
    }
    return firstDeleted >= 0 ? firstDeleted : slot;
  }

  private void ensureCapacity() {
    if ((usedSlots + 1) / (double) hashArray.length > MAX_LOAD_FACTOR) {
      rebuildInto(hashArray.length * 2);
    }
  }

  private void compactIfSparse() {
    int deletedSlots = usedSlots - size;
    if (deletedSlots > size && usedSlots > hashArray.length / 2) {
      rebuildInto(hashArray.length);
    }
  }

  private void rebuildInto(int capacity) {
    rebuildInto(capacity, hashArray);
  }

  @SuppressWarnings("unchecked")
  private void rebuildInto(int capacity, Entry<K, V>[] oldArray) {
    Entry<K, V>[] entries = oldArray;
    hashArray = (Entry<K, V>[]) new Entry[capacity];
    keySet = new MyHashSet<>();
    size = 0;
    usedSlots = 0;
    for (Entry<K, V> entry : entries) {
      if (isActive(entry)) {
        put(entry.key(), entry.value());
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Entry<K, V> deletedEntry() {
    return (Entry<K, V>) DELETED;
  }

  private boolean isActive(Entry<K, V> entry) {
    return entry != null && entry != DELETED;
  }
}
