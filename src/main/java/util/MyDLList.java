package util;

import java.util.Iterator;
import java.io.Serializable;

public class MyDLList<E> implements Iterable<E>, Serializable {
  private DLNode<E> head = new DLNode<E>(null, null, null);
  private DLNode<E> tail = new DLNode<E>(null, null, null);
  private int size = 0;
  private boolean readReversed = false;

  public MyDLList() {
    head.setNext(tail);
    tail.setPrev(head);
  }

  public E get(int index) {
    return getNode(index).getValue();
  }

  public void add(E element) {
    DLNode<E> newNode = new DLNode<E>(tail.getPrev(), element, tail);
    tail.getPrev().setNext(newNode);
    tail.setPrev(newNode);
    size++;
  }

  public void add(E element, int index) {
    DLNode<E> nodeBefore = getNode(index - 1);
    DLNode<E> newNode = new DLNode<E>(nodeBefore, element, nodeBefore.getNext());
    nodeBefore.setNext(newNode);
    newNode.getNext().setPrev(newNode);
    size++;
  }

  public E remove(int index) {
    DLNode<E> nodeToRemove = getNode(index);
    nodeToRemove.getPrev().setNext(nodeToRemove.getNext());
    nodeToRemove.getNext().setPrev(nodeToRemove.getPrev());
    size--;
    return nodeToRemove.getValue();
  }

  public boolean remove(E element) {
    DLNode<E> currNode = head;
    while (currNode.getNext() != tail && !currNode.getNext().getValue().equals(element)) {
      currNode = currNode.getNext();
    }
    if (currNode.getNext() != tail) {
      currNode.setNext(currNode.getNext().getNext());
      currNode.getNext().setPrev(currNode);
      size--;
      return true;
    }
    return false;
  }

  public void set(int index, E element) {
    getNode(index).setValue(element);
  }

  public DLNode<E> getNode(int index) {
    DLNode<E> currNode;
    if (index > size / 2) {
      currNode = tail;
      for (int i = size; i > index; i--) {
        currNode = currNode.getPrev();
      }
      return currNode;
    } else {
      currNode = head;
      for (int i = 0; i <= index; i++) {
        currNode = currNode.getNext();
      }
      return currNode;
    }
  }

  public String toString() {
    String joined = "";
    DLNode<E> currNode = head.getNext();
    int counter = 0;
    do {
      counter++;
      joined += counter + ", ";
      joined += currNode.getValue();
      currNode = currNode.getNext();
    } while (currNode != tail);
    return joined;
  }

  public int size() {
    return size;
  }

  public void scramble() {
    for (int i = 0; i < size; i++) {
      int randIdx = (int) (Math.random() * size);
      E temp = get(i);
      set(i, get(randIdx));
      set(randIdx, temp);
    }
  }

  @Override
  public Iterator<E> iterator() {
    return new MyDLListIterator();
  }

  private class MyDLListIterator implements Iterator<E> {
    private DLNode<E> current = readReversed ? tail.getPrev() : head.getNext();

    @Override
    public boolean hasNext() {
      return readReversed ? current != head : current != tail;
    }

    @Override
    public E next() {
      E value = current.getValue();
      current = readReversed ? current.getPrev() : current.getNext();
      return value;
    }
  }

  public boolean contains(E elementToCheck) {
    for (E element : this) {
      if (element instanceof Enum) {
        Enum elem = (Enum) element;
        Enum elemToCheck = (Enum) elementToCheck;
        if (elem.name().equals(elemToCheck.name())) {
          return true;
        }
      }
      if (element.equals(elementToCheck)) {
        return true;
      }
    }
    return false;
  }

  public void readReversed(boolean reversed) {
    this.readReversed = reversed;
  }

  public E last() {
    return tail.getPrev().getValue();
  }
}
