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

package org.watto.io;

import java.io.*;
import org.watto.array.ArrayResizer;


/***********************************************************************************************
A file filter that only shows <code>File</code>s with a particular extension, or show all 
<code>File</code>s by using the wildcard <i>*</i> as the extension.
***********************************************************************************************/
public class FileExtensionFilter implements FilenameFilter, FileFilter {

  String[] extensions = new String[0];


  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public FileExtensionFilter(){}


  /***********************************************************************************************
  Creates a filter for the given <code>extension</code>
  @param extension the <code>File</code> extension to allow
  ***********************************************************************************************/
  public FileExtensionFilter(String extension){
    this.extensions = new String[]{extension};
  }


  /***********************************************************************************************
  Creates a filter for the given <code>extensions</code>
  @param extensions the <code>File</code> extensions to allow
  ***********************************************************************************************/
  public FileExtensionFilter(String ... extensions){
    this.extensions = extensions;
  }


  /***********************************************************************************************
  Whether the <code>File</code> is allowed by this filter
  @param path the <code>File</code> to check
  @return true if the <code>File</code> is allowed, false if not
  ***********************************************************************************************/
  public boolean accept(File path){
    if (!path.isFile()) {
      return true;
    }

    return accept(new File(path.getParent()),path.getName());
  }


  /***********************************************************************************************
  Whether the <code>File</code> with the given <code>directory</code> and <code>name</code> is
  allowed by this filter
  @param directory the directory that contains the <code>File</code>
  @param name the name of the <code>File</code>
  @return true if the <code>File</code> is allowed, false if not
  ***********************************************************************************************/
  public boolean accept(File directory,String name){
    int dotPos = name.lastIndexOf(".");
    String fileExtension = "";
    if (dotPos != 0) {
      fileExtension = name.substring(dotPos + 1);
    }

    int extensionCount = extensions.length;
    for (int i = 0;i < extensionCount;i++) {
      String extension = extensions[i];
      if (fileExtension.equals(extension)) {
        return true;
      }
      else if (extension.equals("*")) {
        return true;
      }
    }

    return false;
  }


  /***********************************************************************************************
  Adds an extension to this filter
  @param newExtension the extension to add
  ***********************************************************************************************/
  public void add(String newExtension){
    int length = extensions.length;
    ArrayResizer.resize(extensions,length + 1);
    extensions[length] = newExtension;
  }


  /***********************************************************************************************
  Adds a number of extensions to this filter
  @param newExtensions the extensions to add
  ***********************************************************************************************/
  public void add(String ... newExtensions){
    int length = extensions.length;
    int newExtensionsLength = newExtensions.length;
    ArrayResizer.resize(extensions,length + newExtensionsLength);
    System.arraycopy(extensions,length,newExtensions,0,newExtensionsLength);
  }


  /***********************************************************************************************
  Removes all the extensions from the filter
  ***********************************************************************************************/
  public void clear(){
    extensions = new String[0];
  }


  /***********************************************************************************************
  Gets the list of extensions accepted by this filter
  @return the extensions accepted by this filter
  ***********************************************************************************************/
  public String[] getExtensions(){
    return extensions;
  }


  /***********************************************************************************************
  Removes an extension from this filter
  @param newExtension the extension to remove
  ***********************************************************************************************/
  public void remove(String newExtension){
    int length = extensions.length;
    for (int i = 0;i < length;i++) {
      if (extensions[i].equals(newExtension)) {
        // move the last extension to overwrite the matched extension, then resize the array
        extensions[i] = extensions[length - 1];
        ArrayResizer.resize(extensions,length - 1);
        return;
      }
    }
  }


  /***********************************************************************************************
  Removes a number of extensions from this filter
  @param newExtensions the extensions to remove
  ***********************************************************************************************/
  public void remove(String ... newExtensions){

    int extensionsLength = extensions.length;
    int newExtensionsLength = newExtensions.length;

    for (int n = 0;n < newExtensionsLength;n++) {
      String newExtension = newExtensions[n];

      for (int e = 0;e < extensionsLength;e++) {
        if (extensions[e].equals(newExtension)) {
          // move the last extension to overwrite the matched extension.
          extensions[e] = extensions[extensionsLength - 1];
          extensionsLength--;

          // found it, so break out of the "e" loop
          break;
        }
      }

    }

    // extensionsLength should have the new correct length, so do the resize
    ArrayResizer.resize(extensions,extensionsLength);

  }

}