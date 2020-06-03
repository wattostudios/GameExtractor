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

package org.watto.event;

import java.awt.AWTEvent;
import org.watto.component.WSComponent;

/***********************************************************************************************
 A custom event that can be triggered by an <code>Object</code> and captured by a
 <code>WSEventableInterface</code> monitor.
 ***********************************************************************************************/

public class WSEvent extends AWTEvent {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** A generic event **/
  public static final int GENERIC_EVENT = 9900;
  /** A color has changed **/
  public static final int COLOR_CHANGED = 9901;
  /** The task list has changed **/
  public static final int TASK_LIST_CHANGED = 9902;
  /** The list of recent files has changed **/
  public static final int RECENT_FILES_CHANGED = 9903;
  /** The <code>showBorder</code> property of a <code>WSComponent</code> has changed **/
  public static final int SHOW_BORDER_CHANGED = 9904;
  /** The <code>showLabel</code> property of a <code>WSComponent</code> has changed **/
  public static final int SHOW_LABEL_CHANGED = 9905;
  /** An open or expand event had occurred, such as on a <code>WSCollapsePanel</code> **/
  public static final int EXPAND_PANEL = 9906;
  /** A close or collapse event had occurred, such as on a <code>WSCollapsePanel</code> **/
  public static final int COLLAPSE_PANEL = 9907;

  /** The source object that triggered the event **/
  Object source = null;
  /** The type of the event **/
  int type = -1;

  /***********************************************************************************************
   Creates a <code>type</code> event triggered by the <code>source</code>
   @param source the object that triggered the event
   @param type the type of event that was triggered
   ***********************************************************************************************/
  public WSEvent(Object source, int type) {
    super(source, type);
    this.type = type;
    this.source = source;
  }

  /***********************************************************************************************
   Gets the <code>WSComponent</code> that triggered the event, if the <code>source</code> is
   a <code>WSComponent</code>
   @return the source component, or null if the source is not a <code>WSComponent</code>
   ***********************************************************************************************/
  public WSComponent getComponent() {
    if (source instanceof WSComponent) {
      return (WSComponent) source;
    }
    else {
      return null;
    }
  }

  /***********************************************************************************************
   Gets the <code>Object</code> that triggered the event
   @return the source of the event
   ***********************************************************************************************/
  @Override
  public Object getSource() {
    return source;
  }

  /***********************************************************************************************
   Gets the event type
   @return the event type
   ***********************************************************************************************/
  public int getType() {
    return type;
  }

}