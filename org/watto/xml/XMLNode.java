////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto.xml;

import java.util.Arrays;
import java.util.Enumeration;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;


/***********************************************************************************************
 A tree node used to store the contents of an XML tag. Also provides methods for navigating
 through the tree. Implements the standard interfaces <code>TreeNode</code> and
 <code>MutableTreeNode</code> so that the tree can be used by all existing classes that handle
 trees, such as <code>JTree</code>
 @see javax.swing.tree.MutableTreeNode
 @see javax.swing.tree.TreeNode
 @see javax.swing.JTree
***********************************************************************************************/
public class XMLNode implements TreeNode, MutableTreeNode, Comparable<XMLNode> {

  /** The name of the tag **/
  public String tag = "";
  /** The content of the tag **/
  public String content = "";
  /** The attributes of the tag **/
  public String[][] attributes = new String[0][2];
  /** The children under this tag **/
  protected XMLNode[] children = new XMLNode[0];
  /** The parent of this tag **/
  protected XMLNode parent = null;


  /***********************************************************************************************
  Creates an empty node
  ***********************************************************************************************/
  public XMLNode(){}


  /***********************************************************************************************
  Creates an XML node with the given <code>tag</code>
  @param tag the XML tag for this node
  ***********************************************************************************************/
  public XMLNode(String tag){
    this.tag = tag;
  }


  /***********************************************************************************************
  Creates an XML node with the given <code>tag</code> and <code>content</code>
  @param tag the XML tag for this node
  @param content the content of the XML node
  ***********************************************************************************************/
  public XMLNode(String tag, String content){
    this.tag = tag;
    this.content = content;
  }


  /***********************************************************************************************
  Creates an XML node with the given <code>tag</code> and <code>attributes</code>
  @param tag the XML tag for this node
  @param attributes the XML tag attributes
  ***********************************************************************************************/
  public XMLNode(String tag, String[][] attributes){
    this.tag = tag;
    this.attributes = attributes;
  }


  /***********************************************************************************************
  Creates an XML node with the given <code>tag</code>, <code>content</code>, and <code>attributes</code>
  @param tag the XML tag for this node
  @param attributes the XML tag attributes
  @param content the content of the XML node
  ***********************************************************************************************/
  public XMLNode(String tag, String[][] attributes, String content){
    this.tag = tag;
    this.content = content;
    this.attributes = attributes;
  }


  /***********************************************************************************************
  Adds an attribute
  @param key the attribute key
  @param value the value of the attribute
  ***********************************************************************************************/
  public void addAttribute(String key,String value){
    int oldSize = attributes.length;

    String[][] oldAttributes = attributes;
    attributes = new String[oldSize + 1][2];
    System.arraycopy(oldAttributes,0,attributes,0,oldSize);

    attributes[oldSize] = new String[]{key,value};
  }


  /***********************************************************************************************
  Adds a <code>child</code> node
  @param child the node to add as a child
  ***********************************************************************************************/
  public void addChild(XMLNode child){
    addChildren(new XMLNode[]{child});
  }


  /***********************************************************************************************
  Adds a <code>child</code> node at the given <code>index</code>
  @param child the node to add as a child
  @param index the index to insert the <code>child</code> node
  ***********************************************************************************************/
  public void addChild(XMLNode child,int index){
    addChildren(new XMLNode[]{child},index);
  }


  /***********************************************************************************************
  Adds a number of children nodes
  @param newChildren the children nodes to add
  ***********************************************************************************************/
  public void addChildren(XMLNode[] newChildren){
    addChildren(newChildren,children.length);
  }


  /***********************************************************************************************
  Adds a number of children nodes at the given <code>index</code>
  @param newChildren the children nodes to add
  @param index the index to insert the children
  ***********************************************************************************************/
  public void addChildren(XMLNode[] newChildren,int index){
    int oldSize = children.length;

    if (index > oldSize) {
      throw new IndexOutOfBoundsException("Index " + index + " is greater than the child array length " + children.length);
    }
    else if (index < 0) {
      throw new IndexOutOfBoundsException("Index " + index + " must be positive");
    }

    //if (index == oldSize) {
    //  addChildren(newChildren);
    //}

    int newChildrenLength = newChildren.length;
    int newSize = oldSize + newChildrenLength;

    XMLNode[] oldChildren = children;
    children = new XMLNode[newSize];;

    // copy the first half of the array
    System.arraycopy(oldChildren,0,children,0,index);
    // add in the new children at the index
    System.arraycopy(newChildren,0,children,oldSize,newChildrenLength);
    // copy the last half of the array
    System.arraycopy(oldChildren,0,children,index + newChildrenLength,oldSize - index);

    for (int i = 0;i < newSize;i++) {
      children[i].setParent(this);
    }

  }


  /***********************************************************************************************
  Appends <code>newContent</code> to the end of the current node <code>content</code>
  @param newContent the content to append
  ***********************************************************************************************/
  public void addContent(String newContent){
    content += newContent;
  }


  /***********************************************************************************************
  Throws an <code>XMLException</code> if this node has no <code>children</code>  
  @throws XMLException if this node has no <code>children</code>
  ***********************************************************************************************/
  protected void checkIsBranch() throws XMLException{
    if (isLeaf()) {
      throw new XMLException("This child has no children");
    }
  }


  /***********************************************************************************************
  Throws an <code>XMLException</code> if the <code>object</code> is null
  @param object the <code>Object</code> to check
  @throws XMLException if the <code>object</code> is null
  ***********************************************************************************************/
  protected void checkNotNull(Object object) throws XMLException{
    if (object == null) {
      throw new XMLException("Object cannot be null");
    }
  }


  /***********************************************************************************************
  Throws an <code>XMLException</code> if the <code>object</code> is not an <code>XMLNode</code>
  @param object the <code>Object</code> to check
  @throws XMLException if the <code>object</code> is not an <code>XMLNode</code>
  ***********************************************************************************************/
  protected void checkXMLNode(Object object) throws XMLException{
    if (!(object instanceof XMLNode)) {
      throw new XMLException("Object is not an XMLNode");
    }
  }


  /***********************************************************************************************
  Gets an <code>Enumeration</code> of this nodes' <code>children</code>
  @return an <code>Enumeration</code> of the <code>children</code>
  ***********************************************************************************************/
  public Enumeration<XMLNode> children(){
    return new XMLChildEnumeration(this);
  }


  /***********************************************************************************************
  Compares this node to another <code>XMLNode</code>
  @param otherNode the <code>XMLNode</code> to compare to
  @return <b>0</b> if the nodes are identical<br />
          <b>1</b> if this node comes before the <code>otherNode</code><br />
          <b>-1</b> if this node comes after the <code>otherNode</code>
  ***********************************************************************************************/
  public int compareTo(XMLNode otherNode){
    int tagCompare = tag.compareTo(((XMLNode)otherNode).getTag());
    if (tagCompare == 0) {
      return content.compareTo(((XMLNode)otherNode).getContent());
    }
    return tagCompare;
  }


  /***********************************************************************************************
  Does this node allow children?
  @return true
  ***********************************************************************************************/
  public boolean getAllowsChildren(){
    return true;
  }


  /***********************************************************************************************
  Gets the key and value of the attribute with the given <code>index</code> number
  @param index the index number of an attribute
  @return the attribute key and value
  ***********************************************************************************************/
  public String[] getAttribute(int index){
    if (index >= attributes.length) {
      throw new IndexOutOfBoundsException("The index " + index + " is larger than the attribute array length " + attributes.length);
    }
    else if (index < 0) {
      throw new IndexOutOfBoundsException("The index " + index + " must be positive");
    }
    return attributes[index];
  }


  /***********************************************************************************************
  Gets the value of the attribute with the given <code>key</code>
  @param key the key of the attribute that you wish to get the value of
  @return the attribute value, or <b>null</b> if no attribute was found with the given
          <code>key</code>
  ***********************************************************************************************/
  public String getAttribute(String key){
    for (int i = 0;i < attributes.length;i++) {
      if (attributes[i][0].equals(key)) {
        return attributes[i][1];
      }
    }
    return null;
  }


  /***********************************************************************************************
  Gets the number of <code>attributes</code>
  @return the number of <code>attributes</code>
  ***********************************************************************************************/
  public int getAttributeCount(){
    return attributes.length;
  }


  /***********************************************************************************************
  Gets the <code>attributes</code> of this tag
  @return the <code>attributes</code>
  ***********************************************************************************************/
  public String[][] getAttributes(){
    return attributes;
  }


  /***********************************************************************************************
  Gets the child with the given <code>index</code> number
  @param index the index number of a child
  @return the child
  @throws XMLException if there are no <code>children</code> on this node 
  ***********************************************************************************************/
  public XMLNode getChild(int index) throws XMLException{
    checkIsBranch();

    if (index >= children.length) {
      throw new IndexOutOfBoundsException("The index " + index + " is larger than the children array length " + children.length);
    }
    else if (index < 0) {
      throw new IndexOutOfBoundsException("The index " + index + " must be positive");
    }

    return children[index];
  }


  /***********************************************************************************************
  Gets the first child with the given <code>tag</code>
  @param tag the tag of the child to search for
  @return the child, or <b>null</b> if no child with the <code>tag</code> name exists
  @throws XMLException if there are no <code>children</code> on this node 
  ***********************************************************************************************/
  public XMLNode getChild(String tag) throws XMLException{
    return getChild(tag,0);
  }


  /***********************************************************************************************
  Gets the first child with the given <code>tag</code>
  @param tag the tag of the child to search for
  @param startIndex the starting position in the <code>children</code> array
  @return the child, or <b>null</b> if no child with the <code>tag</code> name exists
  @throws XMLException if there are no <code>children</code> on this node
  ***********************************************************************************************/
  public XMLNode getChild(String tag,int startIndex) throws XMLException{
    return getChild(tag,startIndex,false);
  }


  /***********************************************************************************************
  Gets the first child with the given <code>tag</code>
  @param tag the tag of the child to search for
  @param startIndex the starting position in the <code>children</code> array
  @param loopOver whether to start searching from the beginning of the <code>children</code> array
  if the child hadn't been found after the <code>startIndex</code>
  @return the child, or <b>null</b> if no child with the <code>tag</code> name exists
  @throws XMLException if there are no <code>children</code> on this node
  ***********************************************************************************************/
  public XMLNode getChild(String tag,int startIndex,boolean loopOver) throws XMLException{
    checkIsBranch();

    int childrenCount = children.length;

    if (startIndex >= childrenCount) {
      throw new IndexOutOfBoundsException("The index " + startIndex + " is larger than the children array length " + childrenCount);
    }
    else if (startIndex < 0) {
      throw new IndexOutOfBoundsException("The index " + startIndex + " must be positive");
    }

    for (int i = startIndex;i < childrenCount;i++) {
      if (children[i].getTag().equals(tag)) {
        return children[i];
      }
    }

    for (int i = 0;i < startIndex;i++) {
      if (children[i].getTag().equals(tag)) {
        return children[i];
      }
    }

    return null;
  }


  /***********************************************************************************************
  Gets the next child node after the given <code>child</code> 
  @param child the child to get the next child of
  @return the next child, or <b>null</b> if the <code>child</code> is the last child in the
  <code>children</code> array
  @throws XMLException <ul><li>if there are no <code>children</code> on this node</li>
                           <li>if <code>child</code> is null</li>
                           <li>if <code>child</code> is not a child of this node</li></ul> 
  ***********************************************************************************************/

  public XMLNode getChildAfter(XMLNode child) throws XMLException{
    checkNotNull(child);
    checkIsBranch();

    for (int i = 0;i < children.length;i++) {
      if (children[i] == child) {
        int nextChildPos = i + 1;
        if (nextChildPos >= children.length) {
          return null;
        }
        return children[nextChildPos];
      }
    }

    throw new XMLException("The child " + child.getTag() + " is not a child of this node");
  }


  /***********************************************************************************************
  Gets the child with the given <code>index</code> number
  @param index the index number of a child
  @return the child
  ***********************************************************************************************/
  public TreeNode getChildAt(int index){
    if (index >= children.length) {
      throw new IndexOutOfBoundsException("The index " + index + " is larger than the children array length " + children.length);
    }
    else if (index < 0) {
      throw new IndexOutOfBoundsException("The index " + index + " must be positive");
    }

    return children[index];
  }


  /***********************************************************************************************
  Gets the child node before the given <code>child</code> 
  @param child the child to get the previous child of
  @return the previous child, or <b>null</b> if the <code>child</code> is the first child in the
  <code>children</code> array
  @throws XMLException <ul><li>if there are no <code>children</code> on this node</li>
                           <li>if <code>child</code> is null</li>
                           <li>if <code>child</code> is not a child of this node</li></ul> 
  ***********************************************************************************************/
  public XMLNode getChildBefore(XMLNode child) throws XMLException{
    checkNotNull(child);
    checkIsBranch();

    for (int i = 0;i < children.length;i++) {
      if (children[i] == child) {
        int prevChildPos = i - 1;
        if (prevChildPos < 0) {
          return null;
        }
        return children[prevChildPos];
      }
    }

    throw new XMLException("The child " + child.getTag() + " is not a child of this node");
  }


  /***********************************************************************************************
  Gets the number of <code>children</code> of this node
  @return the number of <code>children</code>
  ***********************************************************************************************/
  public int getChildCount(){
    return children.length;
  }


  /***********************************************************************************************
  Gets the index of the <code>child</code> in the <code>children</code> array of this node
  @param child the child to get the position of
  @return the index of the <code>child</code>, or <b>-1</b> if the <code>child</code> is not a
  child of this node
  ***********************************************************************************************/
  public int getChildPosition(XMLNode child){
    for (int i = 0;i < children.length;i++) {
      if (children[i] == child) {
        return i;
      }
    }
    return -1;
  }


  /***********************************************************************************************
  Gets all the <code>children</code> of this node
  @return the <code>children</code>
  ***********************************************************************************************/
  public XMLNode[] getChildren(){
    try {
      checkIsBranch();
    }
    catch (XMLException e) {
      return new XMLNode[0];
    }
    return children;
  }


  /***********************************************************************************************
  Gets all the <code>children</code> of this node that have the given <code>tag</code> name
  @param tag the tag name of the <code>children</code> to get
  @return the <code>children</code> with the <code>tag</code> name
  ***********************************************************************************************/
  public XMLNode[] getChildren(String tag){
    try {
      checkIsBranch();
    }
    catch (XMLException e) {
      return new XMLNode[0];
    }

    XMLNode[] matchingChildren = new XMLNode[children.length];
    int matchingChildPos = 0;

    for (int i = 0;i < children.length;i++) {
      if (children[i].getTag().equals(tag)) {
        matchingChildren[matchingChildPos] = children[i];
        matchingChildPos++;
      }
    }

    if (matchingChildPos == matchingChildren.length) {
      return matchingChildren;
    }

    XMLNode[] temp = matchingChildren;
    matchingChildren = new XMLNode[matchingChildPos];
    System.arraycopy(temp,0,matchingChildren,0,matchingChildPos);

    return matchingChildren;
  }


  /***********************************************************************************************
  Gets the <code>content</code> of this node
  @return the <code>content</code>
  ***********************************************************************************************/
  public String getContent(){
    return content;
  }


  /***********************************************************************************************
  Gets the maximum depth of the tree rooted at this node
  @return the maximum depth
  ***********************************************************************************************/
  public int getDepth(){
    int depth = 0;

    for (int i = 0;i < children.length;i++) {
      int childDepth = children[i].getDepth() + 1;
      if (childDepth > depth) {
        depth = childDepth;
      }
    }

    return depth;
  }


  /***********************************************************************************************
  Gets the first child of this node
  @return the first child
  @throws XMLException if this node had no <code>children</code> 
  ***********************************************************************************************/
  public XMLNode getFirstChild() throws XMLException{
    checkIsBranch();
    return children[0];
  }


  /***********************************************************************************************
  Gets the index of the <code>child</code> in the <code>children</code> array of this node
  @param child the child to get the position of
  @return the index of the <code>child</code>, or <b>-1</b> if the <code>child</code> is not a
  child of this node
  ***********************************************************************************************/
  public int getIndex(TreeNode child){
    try {
      checkXMLNode(child);
    }
    catch (XMLException e) {
      return -1;
    }
    return getChildPosition((XMLNode)child);
  }


  /***********************************************************************************************
  Gets the last child of this node
  @return the last child
  @throws XMLException if this node had no <code>children</code> 
  ***********************************************************************************************/
  public XMLNode getLastChild() throws XMLException{
    checkIsBranch();
    return children[children.length - 1];
  }


  /***********************************************************************************************
  Gets the depth of this node from the root
  @return the depth of this node, or <b>0</b> if this node is the root
  ***********************************************************************************************/
  public int getLevel(){
    int level = 0;

    XMLNode parentNode = parent;
    while (parentNode != null) {
      parentNode = (XMLNode)parentNode.getParent();
      level++;
    }

    return level;
  }


  /***********************************************************************************************
  Gets the <code>parent</code> of this node
  @return the <code>parent</code>, or <b>null</b> if this node is the root
  ***********************************************************************************************/
  public TreeNode getParent(){
    return parent;
  }


  /***********************************************************************************************
  Gets the <code>parent</code> <code>XMLNode</code> of this node
  @return the <code>parent</code> <code>XMLNode</code>, or <b>null</b> if this node is the root
  ***********************************************************************************************/
  public XMLNode getParentNode(){
    return parent;
  }


  /***********************************************************************************************
  Gets the path from this node to the root. This node is the last node of the path.
  @return the path to the root
  ***********************************************************************************************/
  public XMLNode[] getPath(){
    int levels = getLevel() + 1;

    XMLNode[] path = new XMLNode[levels];
    path[levels - 1] = this;

    XMLNode parentNode = parent;
    for (int i = levels - 2;i >= 0;i--) {
      path[i] = parentNode;
      parentNode = (XMLNode)parentNode.getParent();
    }

    return path;
  }


  /***********************************************************************************************
  Gets the root of the tree that this node belongs to
  @return the root node
  ***********************************************************************************************/
  public XMLNode getRoot(){
    if (isRoot()) {
      return this;
    }

    XMLNode parentNode = parent;
    while (parentNode != null) {
      XMLNode newParentNode = (XMLNode)parentNode.getParent();
      if (newParentNode == null) {
        return parentNode;
      }
      else {
        parentNode = newParentNode;
      }
    }

    return null;
  }


  /***********************************************************************************************
  Gets the <code>tag</code> name of this node
  @return the <code>tag</code> name
  ***********************************************************************************************/
  public String getTag(){
    return tag;
  }


  /***********************************************************************************************
  Does this node have an attribute with the given <code>key</code>?
  @return <b>true</b> if an attribute with <code>key</code> exists<br />
          <b>false</b> if no attribute exists with the <code>key</code>
  ***********************************************************************************************/
  public boolean hasAttribute(String key){
    for (int i = 0;i < attributes.length;i++) {
      if (attributes[i][0].equals(key)) {
        return true;
      }
    }
    return false;
  }


  /***********************************************************************************************
  Does this node have any <code>attributes</code>?
  @return <b>true</b> if at least 1 attribute exists<br />
          <b>false</b> if no attributes exist
  ***********************************************************************************************/
  public boolean hasAttributes(){
    if (attributes.length == 0) {
      return false;
    }
    else {
      return true;
    }
  }


  /***********************************************************************************************
  Does this node have any <code>children</code>?
  @return <b>true</b> if at least 1 child exists<br />
          <b>false</b> if no children exist
  ***********************************************************************************************/
  public boolean hasChildren(){
    if (children.length == 0) {
      return false;
    }
    else {
      return true;
    }
  }


  /***********************************************************************************************
  Does this node have any <code>content</code>?
  @return <b>true</b> if this node has <code>content</code><br />
          <b>false</b> if this node doesn't have any <code>content</code>
  ***********************************************************************************************/
  public boolean hasContent(){
    if (content.length() == 0) {
      return false;
    }
    else {
      return true;
    }
  }


  /***********************************************************************************************
  Does this node have a <code>parent</code>?
  @return <b>true</b> if this node has a <code>parent</code><br />
          <b>false</b> if this node doesn't have a <code>parent</code>
  ***********************************************************************************************/
  public boolean hasParent(){
    if (parent == null) {
      return false;
    }
    else {
      return true;
    }
  }


  /***********************************************************************************************
  Adds a <code>child</code> node at the given <code>index</code>
  @param child the node to add as a child
  @param index the index to insert the <code>child</code> node
  ***********************************************************************************************/
  public void insert(MutableTreeNode child,int index){
    try {
      checkXMLNode(child);
      addChild((XMLNode)child,index);
    }
    catch (XMLException e) {
      return;
    }
  }


  /***********************************************************************************************
  Is this node an ancestor of the <code>node</code>?
  @param node the node to check against
  @return <b>true</b> if this node is an ancestor of the <code>node</code><br />
          <b>false</b> if this node is not an ancestor
  ***********************************************************************************************/
  public boolean isAncestorOf(XMLNode node){
    return node.isNodeDescendant(this);
  }


  /***********************************************************************************************
  Is this node a branch? ie. Does this node have any children?
  @return <b>true</b> if this node has at least 1 child<br />
          <b>false</b> if this node has no children
  ***********************************************************************************************/
  public boolean isBranch(){
    if (children.length == 0) {
      return false;
    }
    else {
      return true;
    }
  }


  /***********************************************************************************************
  Is this node a child of the <code>node</code>?
  @return <b>true</b> if this node is a child of <code>node</code><br />
          <b>false</b> if this node is not a child of the <code>node</code>
  ***********************************************************************************************/
  public boolean isChildOf(XMLNode node){
    try {
      checkNotNull(node);
    }
    catch (XMLException e) {
      return false;
    }

    if (node == parent) {
      return true;
    }
    else {
      return false;
    }
  }


  /***********************************************************************************************
  Is this node a descendant of the <code>node</code>?
  @param node the node to check against
  @return <b>true</b> if this node is a descendant of the <code>node</code><br />
          <b>false</b> if this node is not a descendant
  ***********************************************************************************************/
  public boolean isDescendantOf(XMLNode node){
    return node.isNodeDescendant(this);
  }


  /***********************************************************************************************
  Is this node empty? ie Does this node have no <code>parent</code> and no <code>children</code>?
  @return <b>true</b> if this node has no <code>parent</code> and no <code>children</code><br />
          <b>false</b> if this node has a <code>parent</code> or at least 1 child
  ***********************************************************************************************/
  public boolean isEmpty(){
    if (parent == null && children.length == 0) {
      return true;
    }
    else {
      return false;
    }
  }


  /***********************************************************************************************
  Is this node a leaf? ie. Does this node have no <code>children</code>?
  @return <b>true</b> if this node has no <code>children</code><br />
          <b>false</b> if this node has at least 1 child
  ***********************************************************************************************/
  public boolean isLeaf(){
    if (children.length == 0) {
      return true;
    }
    else {
      return false;
    }
  }


  /***********************************************************************************************
  Is the <code>node</code> an ancestor of this node?
  @param node the node to check for
  @return <b>true</b> if the <code>node</code> is an ancestor of this node<br />
          <b>false</b> if the <code>node</code> is not an ancestor
  ***********************************************************************************************/
  public boolean isNodeAncestor(XMLNode node){
    try {
      checkNotNull(node);
    }
    catch (XMLException e) {
      return false;
    }

    if (node == this) {
      return true;
    }

    XMLNode parentNode = parent;
    while (parentNode != null) {
      if (node == parentNode) {
        return true;
      }
      parentNode = (XMLNode)parentNode.getParent();
    }

    return false;
  }


  /***********************************************************************************************
  Is the <code>node</code> a child of this node?
  @param node the node to check for
  @return <b>true</b> if the <code>node</code> is a child of this node<br />
          <b>false</b> if the <code>node</code> is not a child
  ***********************************************************************************************/
  public boolean isNodeChild(XMLNode node){
    try {
      checkNotNull(node);
    }
    catch (XMLException e) {
      return false;
    }

    if (node.getParent() == this) {
      return true;
    }
    else {
      return false;
    }
  }


  /***********************************************************************************************
  Is the <code>node</code> a descendant of this node?
  @param node the node to check for
  @return <b>true</b> if the <code>node</code> is a descendant of this node<br />
          <b>false</b> if the <code>node</code> is not a descendant
  ***********************************************************************************************/
  public boolean isNodeDescendant(XMLNode node){
    try {
      checkNotNull(node);
    }
    catch (XMLException e) {
      return false;
    }

    if (node == this) {
      return true;
    }

    XMLNode parentNode = (XMLNode)node.getParent();
    while (parentNode != null) {
      if (parentNode == this) {
        return true;
      }
      parentNode = (XMLNode)parentNode.getParent();
    }

    return false;
  }


  /***********************************************************************************************
  Is the <code>node</code> related to this node? ie. Are both nodes in the same tree?
  @param node the node to check for
  @return <b>true</b> if the <code>node</code> is related to this node<br />
          <b>false</b> if the <code>node</code> is not related
  ***********************************************************************************************/
  public boolean isNodeRelated(XMLNode node){
    try {
      checkNotNull(node);
    }
    catch (XMLException e) {
      return false;
    }

    if (getRoot() == node.getRoot()) {
      return true;
    }
    else {
      return false;
    }
  }


  /***********************************************************************************************
  Is the <code>node</code> a sibling to this node? ie. Do both nodes have the same parent?
  @param node the node to check for
  @return <b>true</b> if the <code>node</code> is a sibling to this node<br />
          <b>false</b> if the <code>node</code> is not a sibling
  ***********************************************************************************************/
  public boolean isNodeSibling(XMLNode node){
    try {
      checkNotNull(node);
      checkNotNull(parent);
    }
    catch (XMLException e) {
      return false;
    }

    if (parent == node.getParent()) {
      return true;
    }
    else {
      return false;
    }
  }


  /***********************************************************************************************
  Is this node the root? ie. Does this node have no <code>parent</code>?
  @return <b>true</b> if this node is the root<br />
          <b>false</b> if this node has a parent, and is therefore not the root
  ***********************************************************************************************/
  public boolean isRoot(){
    if (parent == null) {
      return true;
    }
    else {
      return false;
    }
  }


  /***********************************************************************************************
  Writes the output of the tree under this node to the command prompt
  ***********************************************************************************************/
  public void outputTree(){
    try {
      String output = "";

      int level = getLevel() * 2;
      for (int i = 0;i < level;i++) {
        output += " ";
      }

      output += getTag();

      if (attributes.length > 0) {
        output += " (";

        for (int i = 0;i < attributes.length;i++) {
          if (i != 0) {
            output += ", ";
          }
          output += attributes[i][0];
          output += "=";
          output += attributes[i][1];
        }

        output += ")";
      }

      if (content.length() != 0) {
        output += " : ";
        output += getContent();
      }

      System.out.println(output);

      int numChildren = getChildCount();
      for (int i = 0;i < numChildren;i++) {
        getChild(i).outputTree();
      }
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }


  /***********************************************************************************************
  Removes the child node from the given <code>index</code> in the <code>children</code> array
  @param index the index of the child to remove
  ***********************************************************************************************/
  public void remove(int index){
    removeChild(index);
  }


  /***********************************************************************************************
  Removes the <code>child</code> node from the <code>children</code> array
  @param child the child node to remove
  ***********************************************************************************************/
  public void remove(MutableTreeNode child){
    try {
      checkXMLNode(child);
    }
    catch (XMLException e) {
      return;
    }
    removeChild((XMLNode)child);
  }


  /***********************************************************************************************
  Removes all <code>attributes</code> from the node
  @return the number of removed <code>attributes</code>
  ***********************************************************************************************/
  public int removeAllAttributes(){
    int numRemoved = attributes.length;
    attributes = new String[0][2];
    return numRemoved;
  }


  /***********************************************************************************************
  Removes all <code>children</code> from the node
  @return the number of removed <code>children</code>
  ***********************************************************************************************/
  public int removeAllChildren(){
    int numRemoved = children.length;
    children = new XMLNode[0];
    return numRemoved;
  }


  /***********************************************************************************************
  Removes an attribute from the node
  @param key the key of the attribute to remove
  ***********************************************************************************************/
  public void removeAttribute(String key){
    int oldSize = attributes.length;

    int attributePos = -1;
    for (int i = 0;i < oldSize;i++) {
      if (attributes[i][0].equals(key)) {
        attributePos = i;
        break;
      }
    }

    if (attributePos == -1) {
      // no attribute with this key was found
      return;
    }

    if (oldSize <= 1) {
      removeAllAttributes();
    }

    String[][] oldAttributes = attributes;
    attributes = new String[oldSize - 1][2];
    // copy the first half
    System.arraycopy(oldAttributes,0,attributes,0,attributePos);
    // skip the attribute to remove, and instead copy the remaining half
    System.arraycopy(oldAttributes,attributePos + 1,attributes,attributePos,oldSize - attributePos - 1);
  }


  /***********************************************************************************************
  Removes the child node from the given <code>index</code> in the <code>children</code> array
  @param index the index of the child to remove
  @return the removed node
  ***********************************************************************************************/
  public XMLNode removeChild(int index){
    try {
      checkIsBranch();
    }
    catch (XMLException e) {
      return null;
    }

    if (index < 0 || index >= children.length) {
      return null;
    }

    XMLNode removedChild = children[index];

    int oldSize = children.length;

    XMLNode[] temp = children;
    children = new XMLNode[oldSize - 1];
    System.arraycopy(temp,0,children,0,index);

    System.arraycopy(temp,index + 1,children,index,oldSize - index - 1);

    removedChild.setParent(null);
    return removedChild;
  }


  /***********************************************************************************************
  Removes the first child with the <code>tag</code> name from the <code>children</code> array
  @param tag the tag name of the child to remove
  @return the removed node
  ***********************************************************************************************/
  public XMLNode removeChild(String tag){
    for (int i = 0;i < children.length;i++) {
      if (children[i].getTag().equals(tag)) {
        return removeChild(i);
      }
    }

    return null;
  }


  /***********************************************************************************************
  Removes the <code>child</code> node from the <code>children</code> array
  @param child the child node to remove
  @return the removed node
  ***********************************************************************************************/
  public XMLNode removeChild(XMLNode child){
    for (int i = 0;i < children.length;i++) {
      if (children[i] == child) {
        return removeChild(i);
      }
    }

    return null;
  }


  /***********************************************************************************************
  Removes all children with the <code>tag</code> name from the <code>children</code> array
  @param tag the tag name of the child to remove
  @return the number of removed children
  ***********************************************************************************************/
  public int removeChildren(String tag){
    try {
      checkIsBranch();
    }
    catch (XMLException e) {
      return 0;
    }

    XMLNode[] oldChildren = children;
    children = new XMLNode[oldChildren.length];
    int insertPos = 0;

    for (int i = 0;i < oldChildren.length;i++) {
      if (oldChildren[i].getTag().equals(tag)) {
        // this child should be removed
      }
      else {
        children[insertPos] = oldChildren[i];
        insertPos++;
      }
    }

    int numRemoved = oldChildren.length - insertPos;

    oldChildren = children;
    children = new XMLNode[insertPos];
    System.arraycopy(oldChildren,0,children,0,insertPos);

    return numRemoved;
  }


  /***********************************************************************************************
  Removes the <code>removeChildren</code> nodes from the <code>children</code> array
  @param removeChildren the children nodes to remove
  @return the number of removed children
  ***********************************************************************************************/
  public int removeChildren(XMLNode[] removeChildren){
    try {
      checkIsBranch();
    }
    catch (XMLException e) {
      return 0;
    }

    XMLNode[] oldChildren = children;
    children = new XMLNode[oldChildren.length];
    int insertPos = 0;

    int numRemChildren = removeChildren.length;

    for (int i = 0;i < oldChildren.length;i++) {

      String curName = oldChildren[i].getTag();
      boolean removed = false;

      for (int c = 0;c < numRemChildren;c++) {
        if (removeChildren[c].getTag().equals(curName)) {
          // this child should be removed
          removed = true;

          // reduce the search size of the remChildren array so it gets quicker over time
          numRemChildren--;
          removeChildren[c] = removeChildren[numRemChildren];

          break;
        }
      }

      if (!removed) {
        children[insertPos] = oldChildren[i];
        insertPos++;
      }

    }

    int numRemoved = oldChildren.length - insertPos;

    oldChildren = children;
    children = new XMLNode[insertPos];
    System.arraycopy(oldChildren,0,children,0,insertPos);

    return numRemoved;
  }


  /***********************************************************************************************
  Removes this node from its <code>parent</code>
  ***********************************************************************************************/
  public void removeFromParent(){
    parent.removeChild(this);
    parent = null;
  }


  /***********************************************************************************************
  Sets the value of the <code>key</code> attribute to the given <code>value</code>. If an attribute
  with the <code>key</code> does not exist, it will be created.
  @param key the key of the attribute to set
  @param value the new value of the attribute
  ***********************************************************************************************/
  public void setAttribute(String key,String value){
    for (int i = 0;i < attributes.length;i++) {
      if (attributes[i][0].equals(key)) {
        attributes[i][1] = value;
        return;
      }
    }

    addAttribute(key,value);
  }


  /***********************************************************************************************
  Sets the <code>content</code> of this node
  @param newContent the new <code>content</code>
  ***********************************************************************************************/
  public void setContent(String newContent){
    content = newContent;
  }


  /***********************************************************************************************
  Sets the <code>parent</code> of this node
  @param newParent the new <code>parent</code>
  ***********************************************************************************************/
  public void setParent(MutableTreeNode newParent){
    try {
      checkXMLNode(newParent);
    }
    catch (XMLException e) {
      return;
    }
    setParent((XMLNode)newParent);
  }


  /***********************************************************************************************
  Sets the <code>parent</code> of this node
  @param newParent the new <code>parent</code>
  ***********************************************************************************************/
  public void setParent(XMLNode newParent){
    parent = newParent;
  }


  /***********************************************************************************************
  Sets the name of this <code>tag</code>
  @param newTag the new <code>tag</code> name
  ***********************************************************************************************/
  public void setTag(String newTag){
    tag = newTag;
  }


  /***********************************************************************************************
  Sets the name of this <code>tag</code>. If the <code>object</code> is not a <code>String</code>,
  it will set the <code>tag</code> name to the <code>toString()</code> of the <code>object</code>
  @param object the new <code>tag</code> name
  ***********************************************************************************************/
  public void setUserObject(Object object){
    tag = object.toString();
  }


  /***********************************************************************************************
  Sorts the <code>children</code> of this node
  @param recurrance <b>true</b> to sort the <code>children</code> of this node, and all the children
                                under them<br />
                    <b>false</b> to only sort the <code>children</code> of this node
  ***********************************************************************************************/
  public void sort(boolean recurrance){
    Arrays.sort(children);
    if (recurrance) {
      for (int i = 0;i < children.length;i++) {
        children[i].sort(recurrance);
      }
    }
  }


  /***********************************************************************************************
  Gets the <code>tag</code> name of this node
  @return the <code>tag</code> name
  ***********************************************************************************************/
  public String toString(){
    return getTag();
  }
}