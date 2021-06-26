
package com.github.barubary.dsdecmp;

/**
 * Very simple wrapper to contain two values in one object. 
 */
public class Pair<T, U> {

  /** The first value */
  private T first;

  /** The second value */
  private U second;

  /**
   * Creates a new Pair of values.
   * @param one The first value.
   * @param two The second value.
   */
  public Pair(T one, U two) {
    this.first = one;
    this.second = two;
  }

  /**
   * Creates a new Pair of values, both initialized to {@code null}
   */
  public Pair() {
    this(null, null);
  }

  /** Returns the first value of this Pair. */
  public T getFirst() {
    return this.first;
  }

  /** Sets the first value of this Pair. */
  public void setFirst(T value) {
    this.first = value;
  }

  /** Returns the second value of this Pair. */
  public U getSecond() {
    return this.second;
  }

  /** Sets the second value of this Pair. */
  public void setSecond(U value) {
    this.second = value;
  }

  /** Returns true iff both values in this Pair are non-null. */
  public boolean allSet() {
    return this.first != null && this.second != null;
  }

}
