
package com.github.barubary.dsdecmp;

import java.io.IOException;

public class HuffTreeNode {

  public static int maxInpos = 0;

  public HuffTreeNode node0, node1;

  public int data = -1; // [-1,0xFF]
  /// <summary>
  /// To get a value, provide the last node of a list of bytes &lt; 2. 
  /// the list will be read from back to front.
  /// </summary>

  public Pair<Boolean, Integer> getValue(NLinkedListNode<Integer> code) throws InvalidFileException {
    Pair<Boolean, Integer> outData = new Pair<Boolean, Integer>();
    outData.setSecond(data);
    if (code == null) {
      outData.setFirst(node0 == null && node1 == null && data >= 0);
      return outData;
    }

    if (code.getValue() > 1)
      throw new InvalidFileException("The list should be a list of bytes < 2. got: " + code.getValue());

    int c = code.getValue();
    HuffTreeNode n = c == 0 ? node0 : node1;
    if (n == null)
      outData.setFirst(false);
    return n.getValue(code.getPrevious());
  }

  protected int getValue(String code) throws InvalidFileException {
    NLinkedList<Integer> c = new NLinkedList<Integer>();
    for (char ch : code.toCharArray())
      c.addFirst((int) ch);

    Pair<Boolean, Integer> attempt = this.getValue(c.getLast());
    if (attempt.getFirst())
      return attempt.getSecond();
    else
      return -1;
  }

  public void parseData(HexInputStream his) throws IOException {
    /*
     * Tree Table (list of 8bit nodes, starting with the root node)
             Root Node and Non-Data-Child Nodes are:
               Bit0-5   Offset to next child node,
                        Next child node0 is at (CurrentAddr AND NOT 1)+Offset*2+2
                        Next child node1 is at (CurrentAddr AND NOT 1)+Offset*2+2+1
               Bit6     Node1 End Flag (1=Next child node is data)
               Bit7     Node0 End Flag (1=Next child node is data)
             Data nodes are (when End Flag was set in parent node):
               Bit0-7   Data (upper bits should be zero if Data Size is less than 8)
     */
    this.node0 = new HuffTreeNode();
    this.node1 = new HuffTreeNode();
    long currPos = his.getPosition();
    int b = his.readU8();
    long offset = b & 0x3F;
    boolean end0 = (b & 0x80) > 0, end1 = (b & 0x40) > 0;
    // parse data for node0
    his.setPosition((currPos - (currPos & 1)) + offset * 2 + 2);
    if (his.getPosition() < maxInpos) {
      if (end0)
        node0.data = his.readU8();
      else
        node0.parseData(his);
    }
    // parse data for node1
    his.setPosition((currPos - (currPos & 1)) + offset * 2 + 2 + 1);
    if (his.getPosition() < maxInpos) {
      if (end1)
        node1.data = his.readU8();
      else
        node1.parseData(his);
    }
    // reset position
    his.setPosition(currPos);
  }

  @Override
  public String toString() {
    if (data < 0)
      return "<" + node0.toString() + ", " + node1.toString() + ">";
    else
      return "[" + Integer.toHexString(data) + "]";
  }

  protected int getDepth() {
    if (data < 0)
      return 0;
    else
      return 1 + Math.max(node0.getDepth(), node1.getDepth());
  }
}
