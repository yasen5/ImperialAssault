package util;

import java.io.Serializable;

public class DLNode<T> implements Serializable {
  private T value;
  private DLNode<T> next;
  private DLNode<T> previous;

  public DLNode(DLNode<T> prev, T value, DLNode<T> next) {
    this.previous = prev;
    this.value = value;
    this.next = next;
  }

  public void setNext(DLNode<T> next) {
    this.next = next;
  }

  public void setPrev(DLNode<T> prev) {
    this.previous = prev;
  }

  public DLNode<T> getNext() {
    return next;
  }

  public DLNode<T> getPrev() {
    return previous;
  }

  public T getValue() {
    return value;
  }

  public void setValue(T value) {
    this.value = value;
  }
}
