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

package org.watto.array;

import java.util.Enumeration;

/***********************************************************************************************
An <code>Enumeration</code> that wraps around an abstract <code>Object</code> <code>Array</code>
***********************************************************************************************/
public class ArrayEnumeration implements Enumeration<Object>{
  
  /** The <code>Array</code> to enumerate over **/
  Object[] array;
  
  /** The number of <code>Object</code>s in the <code>array</code> **/
  int arrayCount = 0;
  
  /** The current reading position in the <code>array</code> **/
  private int currentPosition = 0;
  

  /***********************************************************************************************
  Creates a new <code>Enumeration</code> around the <code>array</code>
  @param array the <code>Array</code> to enumerate over
  ***********************************************************************************************/
  public ArrayEnumeration(Object[] array){
    this.array = array;
    this.arrayCount = array.length;
  }


  /***********************************************************************************************
  Whether the end of the <code>array</code> has been reached or not
  @return <b>true</b>  if there are still more <code>Object</code>s to read from the <code>array</code><br />
          <b>false</b> if the end of the <code>array</code> has been reached
  ***********************************************************************************************/
  public boolean hasMoreElements(){
    return (currentPosition < arrayCount);
  }


  /***********************************************************************************************
  Gets the next <code>Object</code> from the <code>array</code>
  @return the next <code>Object</code> in the <code>array</code>
  ***********************************************************************************************/
  public Object nextElement(){
    Object object = array[currentPosition];
    currentPosition++;
    return object;
  }



}