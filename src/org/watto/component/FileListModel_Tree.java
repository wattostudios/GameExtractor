/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.component;

import javax.swing.tree.DefaultMutableTreeNode;
import org.watto.datatype.FakeResource;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************
  A node used to construct the tree in the file tree list
**********************************************************************************************
**/

public class FileListModel_Tree extends DefaultMutableTreeNode implements Comparable<DefaultMutableTreeNode> {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  Resource resource = null;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public FileListModel_Tree(String name) {
    super(name);
    this.resource = new FakeResource(name);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public FileListModel_Tree(String name, Resource resource) {
    super(name);
    this.resource = resource;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int compareTo(DefaultMutableTreeNode otherNode) {

    DefaultMutableTreeNode node = otherNode;

    if (!node.isLeaf()) {
      if (!isLeaf()) {
        // both nodes have children, so sort alphabetically
        //System.out.println(((String)getUserObject()).toLowerCase() + " -- vs -- " + ((String)node.getUserObject()).toLowerCase());
        return ((String) getUserObject()).toLowerCase().compareTo(((String) node.getUserObject()).toLowerCase());
      }
      else {
        // branch comes first - other node
        return 1;
      }
    }
    else {
      if (!isLeaf()) {
        // branch comes first - this node
        return -1;
      }
      else {
        // no nodes have children, so sort alphabetically
        return ((String) getUserObject()).toLowerCase().compareTo(((String) node.getUserObject()).toLowerCase());
      }
    }
  }

  /**
  **********************************************************************************************
  Gets the Resource[] for all children of this node. It does not iterate down through the tree,
  it only grabs all direct children.
  **********************************************************************************************
  **/
  public Resource[] getChildrenResources() {
    return getChildrenResources(true, true);
  }

  /**
  **********************************************************************************************
  Gets the Resource[] for all children of this node. It does not iterate down through the tree,
  it only grabs all direct children.
  @param branches include children that are branches
  @param leaves include children that are leaves
  **********************************************************************************************
  **/
  public Resource[] getChildrenResources(boolean branches, boolean leaves) {
    int numChildren = getChildCount();

    Resource[] resources = new Resource[numChildren];
    int numFound = 0;
    for (int i = 0; i < numChildren; i++) {
      FileListModel_Tree child = (FileListModel_Tree) getChildAt(i);
      boolean isLeaf = child.isLeaf();
      if ((isLeaf && leaves) || (!isLeaf && branches)) {
        resources[numFound] = child.getResource();
        numFound++;
      }
    }

    if (numFound < numChildren) {
      resources = ArchivePlugin.resizeResources(resources, numFound);
    }

    return resources;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Resource getResource() {
    return resource;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void sort() {
    if (isLeaf()) {
      return;
    }

    int numChildren = getChildCount();

    DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[numChildren];

    for (int i = 0; i < numChildren; i++) {
      // sort all children that are branches
      DefaultMutableTreeNode child = (DefaultMutableTreeNode) getChildAt(i);
      ((FileListModel_Tree) child).sort();
      nodes[i] = child;
    }
    // then sort the children of this node
    java.util.Arrays.sort(nodes);

    // now add the children back to the node, in the right order.
    removeAllChildren();

    //children = new Vector(numChildren);
    //for (int i=0;i<numChildren;i++){
    //  children.add(i,nodes[i]);
    //  }
    for (int i = 0; i < numChildren; i++) {
      add(nodes[i]);
    }

  }

}