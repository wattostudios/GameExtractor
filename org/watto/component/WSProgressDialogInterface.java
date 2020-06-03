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

import java.awt.Cursor;

/***********************************************************************************************
A Progress Dialog GUI <code>Component</code>
***********************************************************************************************/

public interface WSProgressDialogInterface {

  /***********************************************************************************************
  Sets the cursor being displayed
  ***********************************************************************************************/
  public void setCursor(Cursor cursor);

  /***********************************************************************************************
  Sets this <code>WSProgressDialog</code> to show the main <code>JProgressBar</code> as indeterminate
  @param indeterminate <b>true</b> if the main <code>JProgressBar</code> is indeterminate<br />
                       <b>false</b> if it shows real values
  ***********************************************************************************************/
  public void setIndeterminate(boolean indeterminate);

  /***********************************************************************************************
  Sets this <code>WSProgressDialog</code> to show indeterminate <code>JProgressBar</code>s
  @param indeterminate <b>true</b> if the <code>JProgressBar</code>s are indeterminate<br />
                       <b>false</b> if they show real values
  @param barNumber the <code>JProgressBar</code> to set as indeterminate
  ***********************************************************************************************/
  public void setIndeterminate(boolean indeterminate, int barNumber);

  /***********************************************************************************************
  Sets the maximum value of the main <code>JProgressBar</code>
  @param newMaximum the new maximum value
  ***********************************************************************************************/
  public void setMaximum(long newMaximum);

  /***********************************************************************************************
  Sets the maximum value of the given <code>JProgressBar</code>
  @param newMaximum the new maximum value
  @param barNumber the <code>JProgressBar</code> to set the maximum value of
  ***********************************************************************************************/
  public void setMaximum(long newMaximum, int barNumber);

  /***********************************************************************************************
  Sets the message shown on the <code>WSProgressDialog</code>
  @param newMessage the message to show
  ***********************************************************************************************/
  public void setMessage(String newMessage);

  /***********************************************************************************************
  Sets the number of <code>JProgressBar</code>s to show on the <code>WSProgressDialog</code>
  @param newNumbars the number of <code>JProgressBar</code>s to show
  ***********************************************************************************************/
  public void setNumberOfBars(int newNumBars);

  /***********************************************************************************************
  Sets the current value of the main <code>JProgressBar</code>
  @param newValue the new current value
  ***********************************************************************************************/
  public void setValue(long newValue);

  /***********************************************************************************************
  Sets the current value of the given <code>JProgressBar</code>
  @param newValue the new current value
  @param barNumber the <code>JProgressBar</code> to set the current value of
  ***********************************************************************************************/
  public void setValue(long newValue, int barNumber);

  /***********************************************************************************************
  Shows or hides the <code>WSProgressDialog</code>
  @param visible <b>true</b> to show the <code>WSProgressDialog</code><br />
                 <b>false</b> to hide the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  public void setVisible(boolean visible);

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with a single <code>JProgressBar</code> and a <i>Please
  Wait</i> message
  @param newMaximum the maximum value of the <code>JProgressBar</code>
  ***********************************************************************************************/
  public void show(int newMaximum);

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with multiple <code>JProgressBar</code>s
  @param numBars the number of <code>JProgressBar</code>s to show
  @param newMaximum the maximum value of the <code>JProgressBar</code>s
  @param newMessage the message to show on the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  public void show(int numBars, int newMaximum, String newMessage);

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with a single <code>JProgressBar</code>
  @param newMaximum the maximum value of the <code>JProgressBar</code>
  @param newMessage the message to show on the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  public void show(int newMaximum, String newMessage);

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with a single <code>JProgressBar</code>
  @param newMessage the message to show on the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  public void show(String newMessage);
}