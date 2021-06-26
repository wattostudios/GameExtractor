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

package org.watto.component.tree;

import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import javax.swing.tree.TreeNode;
import org.watto.Language;
import org.watto.array.ArrayEnumeration;
import org.watto.io.DirectoriesOnlyFileFilter;


/***********************************************************************************************

***********************************************************************************************/
public class FileTreeNode implements TreeNode {

  /** The <code>File</code> that this <code>TreeNode</code> represents **/
  File file = null;

  /** The <code>FileTreeNode</code>s that represent the children <code>File</code>s of this <code>file</code> **/
  FileTreeNode[] children = null;

  /** Whether to list all children on this <code>File</code>, or only the directories and drives **/
  boolean directoriesOnly = false;

  /** The parent <code>FileTreeNode</code> **/
  FileTreeNode parent = null;


  /***********************************************************************************************

   ***********************************************************************************************/
  public FileTreeNode(File file, FileTreeNode parent){
    this.file = file;
    this.parent = parent;
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public FileTreeNode(File file, FileTreeNode parent, boolean directoriesOnly){
    this.file = file;
    this.parent = parent;
    this.directoriesOnly = directoriesOnly;
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public void checkChildren(){
    if (children == null) {
      if (file == null) {
        children = new FileTreeNode[0];
        return;
      }
      File[] childFiles;
      if (directoriesOnly) {
        childFiles = file.listFiles(new DirectoriesOnlyFileFilter());
      }
      else {
        childFiles = file.listFiles();
      }

      if (childFiles == null) {
        children = new FileTreeNode[0];
        return;
      }

      // sort the array
      Arrays.sort(childFiles);

      int childCount = childFiles.length;
      children = new FileTreeNode[childCount];

      for (int i = 0;i < childCount;i++) {
        children[i] = new FileTreeNode(childFiles[i],this,directoriesOnly);
      }
    }
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public Enumeration<Object> children(){
    checkChildren();
    return new ArrayEnumeration(children);
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public boolean getAllowsChildren(){
    return true;
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public FileTreeNode getChild(File file){
    checkChildren();

    String filePath = file.getAbsolutePath();

    int childCount = children.length;
    for (int i = 0;i < childCount;i++) {
      FileTreeNode child = children[i];
      if (child.getFile().getAbsolutePath().equals(filePath)) {
        return child;
      }
    }
    return null;
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public TreeNode getChildAt(int childIndex){
    checkChildren();
    return children[childIndex];
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public int getChildCount(){
    checkChildren();
    return children.length;
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public File getFile(){
    return file;
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public int getIndex(TreeNode node){
    checkChildren();

    int childCount = children.length;
    for (int i = 0;i < childCount;i++) {
      if (children[i] == node) {
        return i;
      }
    }
    return -1;
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public TreeNode getParent(){
    return parent;
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public boolean isLeaf(){
    checkChildren();
    return (children.length <= 0);
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public void setChildren(FileTreeNode[] children){
    this.children = children;

    int childCount = children.length;
    for (int i = 0;i < childCount;i++) {
      children[i].setParent(this);
    }
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public void setParent(FileTreeNode parent){
    this.parent = parent;
  }


  /***********************************************************************************************

   ***********************************************************************************************/
  public String toString(){
    if (file == null) {
      return Language.get("Computer");
    }

    String name = file.getName();
    if (name.equals("")) { // occurs for drive letters
      name = file.getAbsolutePath();
    }
    return name;
  }

}