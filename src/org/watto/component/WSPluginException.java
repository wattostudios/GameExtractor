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
An <code>Exception</code> when dealing with <code>WSPlugin</code>s
***********************************************************************************************/

public class WSPluginException extends Exception {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
  Creates an <code>Exception</code> with the given <code>message</code>
  @param message the <code>String</code> describing the error
  ***********************************************************************************************/
  public WSPluginException(String message) {
    super(message);
  }
}