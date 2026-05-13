package util;

import java.io.Serializable;

public class MyHashSet<E> implements Iterable<E>, Serializable {
  private static final int DEFAULT_CAPACITY = 128;
  private static final double MAX_LOAD_FACTOR = 0.65;

  private Object[] hashArray;
  private int size;

  public MyHashSet() {
    hashArray = new Object[DEFAULT_CAPACITY];
    size = 0;
  }

  public boolean add(E obj) {
    ensureCapacity();
    if (contains(obj)) {
      return false;
    } else {
      hashArray[findSlot(obj)] = obj;
      size++;
    }
    return true;
  }

  public void clear() {
    size = 0;
    hashArray = new Object[DEFAULT_CAPACITY];
  }

  public boolean contains(Object obj) {
    int slot = findSlot(obj);
    return hashArray[slot] != null && hashArray[slot].equals(obj);
  }

  public boolean remove(Object obj) {
    if (contains(obj)) {
      hashArray[findSlot(obj)] = null;
      size--;
      rebuild(hashArray);
      return true;
    }
    return false;
  }

  public int size() {
    return size;
  }

  public MyDLList<E> toDLList() {
    MyDLList<E> dll = new MyDLList<E>();
    for (int i = 0; i < hashArray.length; i++) {
      if (hashArray[i] != null) {
        dll.add((E) hashArray[i]);
      }
    }
    return dll;
  }

  @Override
  public java.util.Iterator<E> iterator() {
    return toDLList().iterator();
  }

  // public E getFullEntry(E incomplete) {
  // return (E) hashArray[incomplete.hashCode()];
  // }

  public E getFullEntry(int hashCode) {
    return (E) (hashArray[Math.floorMod(hashCode, hashArray.length)]);
  }

  private int findSlot(Object obj) {
    int slot = Math.floorMod(obj.hashCode(), hashArray.length);
    while (hashArray[slot] != null && !hashArray[slot].equals(obj)) {
      slot = (slot + 1) % hashArray.length;
    }
    return slot;
  }

  private void ensureCapacity() {
    if ((size + 1) / (double) hashArray.length > MAX_LOAD_FACTOR) {
      Object[] oldArray = hashArray;
      hashArray = new Object[oldArray.length * 2];
      rebuild(oldArray);
    }
  }

  @SuppressWarnings("unchecked")
  private void rebuild(Object[] oldArray) {
    Object[] values = oldArray;
    hashArray = new Object[Math.max(DEFAULT_CAPACITY, oldArray.length)];
    int oldSize = size;
    size = 0;
    for (Object value : values) {
      if (value != null) {
        add((E) value);
      }
    }
    size = oldSize;
  }
}
