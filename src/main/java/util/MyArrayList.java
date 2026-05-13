package util;

import java.io.Serializable;
import java.util.Iterator;

public class MyArrayList<E> implements Iterable<E>, Serializable {
  private Object[] list;
  private int size = 0;
  private int capacity = 6;

  public MyArrayList() {
    list = new Object[capacity];
  }

  public MyArrayList(int startingCapacity) {
    capacity = Math.max(1, startingCapacity);
    list = new Object[capacity];
  }

  public MyArrayList(Iterable<? extends E> values) {
    this();
    addAll(values);
  }

  @SafeVarargs
  public static <E> MyArrayList<E> of(E... values) {
    MyArrayList<E> list = new MyArrayList<>();
    if (values != null) {
      for (E value : values) {
        list.add(value);
      }
    }
    return list;
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
    for (int i = index; i < size - 1; i++) {
      list[i] = list[i + 1];
    }
    size--;
    list[size] = null;
    return removed;
  }

  @SuppressWarnings("unchecked")
  public E remove(Object element) {
    for (int i = 0; i < size; i++) {
      if (element == null ? list[i] == null : element.equals(list[i])) {
        return remove(i);
      }
    }
    return null;
  }

  public void set(int index, E newElement) {
    list[index] = newElement;
  }

  public boolean contains(Object val) {
    for (int i = 0; i < size; i++) {
      Object obj = list[i];
      if (obj == null ? val == null : obj.equals(val)) {
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

  public void addAll(Iterable<? extends E> values) {
    if (values == null) {
      return;
    }
    for (E value : values) {
      add(value);
    }
  }

  public void clear() {
    list = new Object[capacity];
    size = 0;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public Object[] toArray() {
    Object[] copy = new Object[size];
    for (int i = 0; i < size; i++) {
      copy[i] = list[i];
    }
    return copy;
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
