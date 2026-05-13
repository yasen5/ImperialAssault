package util;

import java.io.Serializable;

public class MyHashSet<E> implements Serializable {
  private Object[] hashArray;
  private int size;

  public MyHashSet() {
    hashArray = new Object[26 * 26 * 26 + 26 * 26 + 26];
    size = 0;
  }

  public boolean add(E obj) {
    if (contains(obj)) {
      return false;
    } else {
      hashArray[obj.hashCode()] = obj;
      size++;
    }
    return true;
  }

  public void clear() {
    size = 0;
    hashArray = new Object[128];
  }

  public boolean contains(Object obj) {
    if (hashArray[obj.hashCode()] != null && obj.equals(hashArray[obj.hashCode()])) {
      return true;
    }
    return false;
  }

  public boolean remove(Object obj) {
    if (contains(obj)) {
      hashArray[obj.hashCode()] = null;
      size--;
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

  // public E getFullEntry(E incomplete) {
  // return (E) hashArray[incomplete.hashCode()];
  // }

  public E getFullEntry(int hashCode) {
    return (E) (hashArray[hashCode]);
  }
}
