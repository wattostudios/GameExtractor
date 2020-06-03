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

package org.watto;

import org.watto.component.WSPopup;

/***********************************************************************************************
 * A singleton class that captures changes and can ask the user to save the
 * changes they made, via a <code>WSPopup</code>
 ***********************************************************************************************/
public class ChangeMonitor {

  /** whether a modification has occurred or not **/
  static boolean changed = false;

  /***********************************************************************************************
   * Sets the value to indicate that a change has been made
   ***********************************************************************************************/
  public static void change() {
    changed = true;
  }

  /***********************************************************************************************
   * Checks whether a change has been made or not
   * @return true if a change was made, false otherwise
   ***********************************************************************************************/
  public static boolean check() {
    return changed;
  }

  /***********************************************************************************************
   * Displays a "Do you want to save your changes" popup if a change has
   * occurred
   * @return true if the user wants to save their changes, false otherwise
   ***********************************************************************************************/
  public static boolean popup() {
    String ok = WSPopup.showConfirm("SaveChanges");
    if (ok.equals(WSPopup.BUTTON_YES)) {
      return true;
    }
    else {
      return false;
    }
  }

  /***********************************************************************************************
   * Resets the value to indicate that no changes have been made.
   ***********************************************************************************************/
  public static void reset() {
    changed = false;
  }

  /***********************************************************************************************
   * Initialises the change with no changes
   ***********************************************************************************************/
  public ChangeMonitor() {
    changed = false;
  }
}