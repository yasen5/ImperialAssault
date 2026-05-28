package util;

import java.io.Serializable;

public class MyHashSet<E> implements Iterable<E>, Serializable {
  private static final int DEFAULT_CAPACITY = 128;
  private static final double MAX_LOAD_FACTOR = 0.65;
  private static final Object DELETED = new Object();

  private Object[] hashArray;
  private int size;
  private int usedSlots;

  public MyHashSet() {
    hashArray = new Object[DEFAULT_CAPACITY];
    size = 0;
    usedSlots = 0;
  }

  public boolean add(E obj) {
    if (contains(obj)) {
      return false;
    }
    ensureCapacity();
    int slot = findInsertSlot(obj);
    if (hashArray[slot] == null) {
      usedSlots++;
    }
    hashArray[slot] = obj;
    size++;
    return true;
  }

  public void clear() {
    size = 0;
    usedSlots = 0;
    hashArray = new Object[DEFAULT_CAPACITY];
  }

  public boolean contains(Object obj) {
    return findExistingSlot(obj) >= 0;
  }

  public boolean remove(Object obj) {
    int slot = findExistingSlot(obj);
    if (slot < 0) {
      return false;
    }
    hashArray[slot] = DELETED;
    size--;
    compactIfSparse();
    return true;
  }

  public int size() {
    return size;
  }

  @SuppressWarnings("unchecked")
  public MyDLList<E> toDLList() {
    MyDLList<E> dll = new MyDLList<E>();
    for (int i = 0; i < hashArray.length; i++) {
      if (isActive(hashArray[i])) {
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

  @SuppressWarnings("unchecked")
  public E getFullEntry(int hashCode) {
    for (Object entry : hashArray) {
      if (isActive(entry) && entry.hashCode() == hashCode) {
        return (E) entry;
      }
    }
    return null;
  }

  private int findExistingSlot(Object obj) {
    int slot = Math.floorMod(obj.hashCode(), hashArray.length);
    while (hashArray[slot] != null) {
      if (hashArray[slot] != DELETED && hashArray[slot].equals(obj)) {
        return slot;
      }
      slot = (slot + 1) % hashArray.length;
    }
    return -1;
  }

  private int findInsertSlot(Object obj) {
    int slot = Math.floorMod(obj.hashCode(), hashArray.length);
    int firstDeleted = -1;
    while (hashArray[slot] != null) {
      if (hashArray[slot] == DELETED) {
        if (firstDeleted < 0) {
          firstDeleted = slot;
        }
      } else if (hashArray[slot].equals(obj)) {
        return slot;
      }
      slot = (slot + 1) % hashArray.length;
    }
    return firstDeleted >= 0 ? firstDeleted : slot;
  }

  private void ensureCapacity() {
    if ((usedSlots + 1) / (double) hashArray.length > MAX_LOAD_FACTOR) {
      rebuild(hashArray.length * 2);
    }
  }

  private void compactIfSparse() {
    int deletedSlots = usedSlots - size;
    if (deletedSlots > size && usedSlots > hashArray.length / 2) {
      rebuild(hashArray.length);
    }
  }

  @SuppressWarnings("unchecked")
  private void rebuild(int capacity) {
    Object[] values = hashArray;
    hashArray = new Object[Math.max(DEFAULT_CAPACITY, capacity)];
    size = 0;
    usedSlots = 0;
    for (Object value : values) {
      if (isActive(value)) {
        add((E) value);
      }
    }
  }

  private boolean isActive(Object value) {
    return value != null && value != DELETED;
  }
}
