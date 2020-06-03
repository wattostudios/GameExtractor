
package com.github.barubary.dsdecmp;

import java.util.Iterator;

public class NLinkedList<E> implements Iterable<E> {

  private NLinkedListNode<E> head, tail;

  public NLinkedListNode<E> getFirst() {
    return this.head;
  }

  public NLinkedListNode<E> getLast() {
    return this.tail;
  }

  public NLinkedList() {
    head = null;
    tail = null;
  }

  public void addFirst(E value) {
    this.addFirst(new NLinkedListNode<E>(value));
  }

  public void addFirst(NLinkedListNode<E> node) {
    assert (head == null ? tail == null : false) : "Both head and tail must be null, or neither";

    if (head == null) {
      head = tail = node;
      node.setNext(null);
      node.setPrevious(null);
    }
    else {
      node.addBefore(head);
      head = node;
    }
  }

  public void removeFirst() {
    if (head != null) {
      NLinkedListNode<E> nHead = head.getNext();
      head.remove();
      this.head = nHead;
    }
  }

  public void addLast(E value) {
    this.addLast(new NLinkedListNode<E>(value));
  }

  public void addLast(NLinkedListNode<E> node) {
    assert (head == null ? tail == null : false) : "Both head and tail must be null, or neither";

    if (tail == null) {
      head = tail = node;
      node.setNext(null);
      node.setPrevious(null);
    }
    else {
      node.addAfter(tail);
      tail = node;
    }
  }

  public void removeLast() {
    if (tail != null) {
      NLinkedListNode<E> nTail = tail.getPrevious();
      tail.remove();
      this.tail = nTail;
    }
  }

  public boolean isEmpty() {
    return this.head == null;
  }

  public void clear() {
    while (!this.isEmpty())
      this.removeFirst();
  }

  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {

      NLinkedListNode<E> current = getFirst();

      boolean removed = false;

      @Override
      public boolean hasNext() {
        return current != null && current.getNext() != null;
      }

      @Override
      public E next() {
        removed = false;
        if (this.current != null)
          this.current = this.current.getNext();
        return this.current != null ? this.current.getValue() : null;
      }

      @Override
      public void remove() {
        if (removed || this.current == null)
          return;
        removed = true;
        this.current.remove();
      }
    };
  }

}
