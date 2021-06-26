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

package org.watto.component;

/***********************************************************************************************
Allows all WSComponents to be compared to each other based on their name
***********************************************************************************************/
public interface WSComparable extends Comparable<WSComparable> {

  /***********************************************************************************************
  Compares the <code>getName()</code> of this <code>WSComparable</code> to the <code>getName()</code>
  of another <code>WSComparable</code>
  @param otherPlugin the <code>WSComparable</code> to compare to
  @return <b>0</b> if the <code>WSComparable</code>s are equal<br />
          <b>1</b> if the <code>otherPlugin</code> comes after this <code>WSComparable</code><br />
          <b>-1</b> if the <code>otherPlugin</code> comes before this <code>WSComparable</code>
  ***********************************************************************************************/
  public int compareTo(WSComparable otherPlugin);


  /***********************************************************************************************
  Gets the name of this <code>WSComparable</code>
  @return the name
  ***********************************************************************************************/
  public String getName();


  /***********************************************************************************************
  Sets the name of this <code>WSComparable</code>
  @param name the name
  ***********************************************************************************************/
  public void setName(String name);


}