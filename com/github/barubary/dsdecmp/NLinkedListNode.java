
package com.github.barubary.dsdecmp;

public class NLinkedListNode<E> {

  private E value;

  private NLinkedListNode<E> previous = null, next = null;

  public NLinkedListNode(E value) {
    this.setValue(value);
  }

  public E getValue() {
    return this.value;
  }

  public void setValue(E value) {
    this.value = value;
  }

  public NLinkedListNode<E> getNext() {
    return this.next;
  }

  public void setNext(NLinkedListNode<E> next) {
    if (this.next != null && this.next.previous == this)
      this.next.previous = null;
    this.next = next;
    if (this.next != null)
      this.next.previous = this;
  }

  public NLinkedListNode<E> getPrevious() {
    return this.previous;
  }

  public void setPrevious(NLinkedListNode<E> previous) {
    if (this.previous != null && this.previous.next == this)
      this.previous.next = null;
    this.previous = previous;
    if (this.previous != null)
      this.previous.next = this;
  }

  /**
   * Adds this node after another node
   * @param node the node this node should be after
   */
  public void addAfter(NLinkedListNode<E> node) {
    assert node != null : "Cannot add a node after null";
    NLinkedListNode<E> next = node.next;
    this.setPrevious(node);
    this.setNext(next);
  }

  /**
   * Adds this node before another node
   * @param node the node this node should be before
   */
  public void addBefore(NLinkedListNode<E> node) {
    assert node != null : "Cannot add a node before null";
    NLinkedListNode<E> prev = node.previous;
    this.setNext(node);
    this.setPrevious(prev);
  }

  public void remove() {
    if (this.previous != null)
      this.previous.setNext(this.next);
    if (this.next != null)
      this.next.setPrevious(this.previous);
  }

}
