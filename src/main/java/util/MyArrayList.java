package util;

import java.util.Iterator;

public class MyArrayList<E> implements Iterable<E> {
  private Object[] list;
  private int size = 0;
  private int capacity = 6;

  public MyArrayList() {
    list = new Object[capacity];
  }

  public MyArrayList(int startingCapacity) {
    capacity = startingCapacity;
    list = new Object[capacity];
  }

  public boolean add(E element) {
    checkCapacity();
    list[size] = element;
    size++;
    return true;
  }

  private void checkCapacity() {
    if (size == capacity) {
      capacity *= 2;
      Object[] newList = new Object[capacity];
      for (int i = 0; i < list.length; i++) {
        newList[i] = list[i];
      }
      list = newList;
    }
  }

  @SuppressWarnings("unchecked")
  public E get(int index) {
    return (E) list[index];
  }

  public int size() {
    return size;
  }

  public E remove(int index) {
    E removed = get(index);
    for (int i = index; i < list.length - 1; i++) {
      list[index] = list[index + 1];
    }
    size--;
    list[size] = null;
    return removed;
  }

  @SuppressWarnings("unchecked")
  public E remove(Object element) {
    for (int i = 0; i < list.length; i++) {
      E cast = (E) element;
      if (cast.equals(list[i])) {
        return remove(i);
      }
    }
    return null;
  }

  public void set(int index, E newElement) {
    list[index] = newElement;
  }

  public boolean contains(Object val) {
    for (Object obj : list) {
      if (obj.equals(val)) {
        return true;
      }
    }
    return false;
  }

  public String toString() {
    String joined = "";
    for (int i = 0; i < list.length; i++) {
      if (list[i] != null)
        joined += list[i] + ", ";
    }
    return joined;
  }

  public void add(int index, Object element) {
    checkCapacity();
    for (int i = size; i > index; i--) {
      list[i] = list[i - 1];
    }
    list[index] = element;
    size++;
  }

  @Override
  public Iterator<E> iterator() {
    return new MyDLListIterator();
  }

  private class MyDLListIterator implements Iterator<E> {
    private int i = 0;

    @Override
    public boolean hasNext() {
      return i < size;
    }

    @Override
    public E next() {
      E curr = get(i);
      i++;
      return curr;
    }
  }
}
