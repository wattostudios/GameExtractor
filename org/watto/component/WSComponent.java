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

import javax.swing.border.Border;
import java.awt.Dimension;
import java.awt.Container;
import java.awt.AWTEvent;
import org.watto.xml.XMLNode;


/***********************************************************************************************
GUI interface component that extend <code>JComponent</code>s with XML construction and additional
functionality.
***********************************************************************************************/
public interface WSComponent extends WSComparable {

  /** The mask used to register this <code>WSComponent</code> as a generator of <code>WSEvent</code>s **/
  public static final long WS_EVENT_MASK = 72057594037927936L;


  /***********************************************************************************************
  Gets the <code>Border</code> surrounding this <code>WSComponent</code>
  @return the <code>Border</code>
  ***********************************************************************************************/
  public Border getBorder();


  /***********************************************************************************************
  Gets the width of this <code>WSComponent</code>s <code>Border</code>
  @return the <code>Border</code> width
  ***********************************************************************************************/
  public int getBorderWidth();


  /***********************************************************************************************
  Gets the text code for this <code>WSComponent</code>, which is used for <code>Language</code>s
  and other functionality
  @return the text code for this <code>WSComponent</code>
  ***********************************************************************************************/
  public String getCode();


  /***********************************************************************************************
  Gets the fixed height of this <code>WSComponent</code>
  @return the fixed height, or <b>-1</b> if this <code>WSComponent</code> doesn't have a fixed height
  ***********************************************************************************************/
  public int getFixedHeight();


  /***********************************************************************************************
  Gets the fixed minimum height of this <code>WSComponent</code>
  @return the fixed minimum height, or <b>-1</b> if this <code>WSComponent</code> doesn't have a 
          fixed minimum height
  ***********************************************************************************************/
  public int getFixedMinimumHeight();


  /***********************************************************************************************
  Gets the fixed minimum width of this <code>WSComponent</code>
  @return the fixed minimum width, or <b>-1</b> if this <code>WSComponent</code> doesn't have a
          fixed minimum width
  ***********************************************************************************************/
  public int getFixedMinimumWidth();


  /***********************************************************************************************
  Gets the fixed width of this <code>WSComponent</code>
  @return the fixed width, or <b>-1</b> if this <code>WSComponent</code> doesn't have a fixed width
  ***********************************************************************************************/
  public int getFixedWidth();


  /***********************************************************************************************
  Gets the listeners registered on this <code>WSComponent</code>
  @return the registered listeners
  ***********************************************************************************************/
  public Object[] getListenerList();


  /***********************************************************************************************
  Gets the maximum size of this <code>WSComponent</code>
  @return the maximum size
  ***********************************************************************************************/
  public Dimension getMaximumSize();


  /***********************************************************************************************
  Gets the minimum size of this <code>WSComponent</code>
  @return the minimum size
  ***********************************************************************************************/
  public Dimension getMinimumSize();


  /***********************************************************************************************
  Gets the parent <code>Container</code> of this <code>WSComponent</code>
  @return the parent <code>Container</code>
  ***********************************************************************************************/
  public Container getParent();


  /***********************************************************************************************
  Gets the position of this <code>WSComponent</code> in its parent <code>Container</code>
  @return the position. For example, <i>"CENTER"</i> or <i>"NORTH"</i>.
  ***********************************************************************************************/
  public String getPosition();


  /***********************************************************************************************
  Gets the preferred size of this <code>WSComponent</code>
  @return the preferred size
  ***********************************************************************************************/
  public Dimension getPreferredSize();


  /***********************************************************************************************
  Gets the <code>Language</code> small text of this <code>WSComponent</code>
  @return the small text
  ***********************************************************************************************/
  public String getSmallText();


  /***********************************************************************************************
  Gets the <code>Language</code> text of this <code>WSComponent</code>
  @return the text
  ***********************************************************************************************/
  public String getText();


  /***********************************************************************************************
  Gets the <code>Language</code> tooltip text of this <code>WSComponent</code>
  @return the tooltip text
  ***********************************************************************************************/
  public String getToolTipText();


  /***********************************************************************************************
  Is this <code>WSComponent</code> the currently focused component?
  @return <b>true</b> if this <code>WSComponent</code> has the current focus<br />
          <b>false</b> if this <code>WSComponent</code> doesn't have focus.
  ***********************************************************************************************/
  public boolean hasFocus();


  /***********************************************************************************************
  Is this <code>WSComponent</code> enabled?
  @return <b>true</b> if this <code>WSComponent</code> is enabled<br />
          <b>false</b> if this <code>WSComponent</code> is not enabled.
  ***********************************************************************************************/
  public boolean isEnabled();


  /***********************************************************************************************
  Is this <code>WSComponent</code> stored in the <code>ComponentRepository</code>?
  @return <b>true</b> if this <code>WSComponent</code> is in the <code>ComponentRepository</code><br />
          <b>false</b> if this <code>WSComponent</code> is not in the <code>ComponentRepository</code>.
  ***********************************************************************************************/
  public boolean isInRepository();


  /***********************************************************************************************
  Is this <code>WSComponent</code> opaque?
  @return <b>true</b> if this <code>WSComponent</code> is opaque<br />
          <b>false</b> if this <code>WSComponent</code> is not opaque.
  ***********************************************************************************************/
  public boolean isOpaque();


  /***********************************************************************************************
  Is this <code>WSComponent</code> visible?
  @return <b>true</b> if this <code>WSComponent</code> is visible<br />
          <b>false</b> if this <code>WSComponent</code> is not visible.
  ***********************************************************************************************/
  public boolean isVisible();


  /***********************************************************************************************
  Processes an <code>event</code> that was triggered on this <code>WSComponent</code>
  @param event the <code>AWTEvent</code> that was triggered
  ***********************************************************************************************/
  public void processEvent(AWTEvent event);


  /***********************************************************************************************
  Registers the <code>AWTEvent</code>s that this <code>WSComponent</code> generates
  ***********************************************************************************************/
  public void registerEvents();


  /***********************************************************************************************
  Sets the <code>border</code> surrounding this <code>WSComponent</code>
  @param border the <code>Border</code>
  ***********************************************************************************************/
  public void setBorder(Border border);


  /***********************************************************************************************
  Sets the border width attribute value. <i>This does not actually set the width of the border!</i>
  @param borderWidth the new border width attribute value
  ***********************************************************************************************/
  public void setBorderWidth(int borderWidth);


  /***********************************************************************************************
  Sets the text <code>code</code> for this <code>WSComponent</code>
  @param code the text code
  ***********************************************************************************************/
  public void setCode(String code);


  /***********************************************************************************************
  Sets whether this <code>WSComponent</code> is enabled or not
  @param enabled <b>true</b> if this <code>WSComponent</code> is enabled<br />
                 <b>false</b> if this <code>WSComponent</code> is not enabled.
  ***********************************************************************************************/
  public void setEnabled(boolean enabled);


  /***********************************************************************************************
  Sets the fixed height attribute value. <i>This does not actually set the height of the
  <code>WSComponent</code>!</i>
  @param fixedHeight the new fixed height attribute value
  ***********************************************************************************************/
  public void setFixedHeight(boolean fixedHeight);


  /***********************************************************************************************
  Sets the fixed minimum height attribute value. <i>This does not actually set the height of the
  <code>WSComponent</code>!</i>
  @param fixedMinimumHeight the new fixed minimum height attribute value
  ***********************************************************************************************/
  public void setFixedMinimumHeight(boolean fixedMinimumHeight);


  /***********************************************************************************************
  Sets the fixed minimum width attribute value. <i>This does not actually set the width of the
  <code>WSComponent</code>!</i>
  @param fixedMinimumWidth the new fixed minimum width attribute value
  ***********************************************************************************************/
  public void setFixedMinimumWidth(boolean fixedMinimumWidth);


  /***********************************************************************************************
  Sets the fixed width attribute value. <i>This does not actually set the width of the
  <code>WSComponent</code>!</i>
  @param fixedWidth the new fixed width attribute value
  ***********************************************************************************************/
  public void setFixedWidth(boolean fixedWidth);


  /***********************************************************************************************
  Sets whether this <code>WSComponent</code> is focused or not
  @param focused <b>true</b> if this <code>WSComponent</code> is focused<br />
                 <b>false</b> if this <code>WSComponent</code> is not focused.
  ***********************************************************************************************/
  public void setFocus(boolean focused);


  /***********************************************************************************************
  Sets whether this <code>WSComponent</code> is stored in the <code>ComponentRepository</code> or not
  @param inRepository <b>true</b> if this <code>WSComponent</code> is stored in the
                                  <code>ComponentRepository</code><br />
                      <b>false</b> if this <code>WSComponent</code> is not stored in the
                                  <code>ComponentRepository</code>.
  ***********************************************************************************************/
  public void setInRepository(boolean inRepository);


  /***********************************************************************************************
  Sets the maximum size of this <code>WSComponent</code>
  @param dimension the new maximum size
  ***********************************************************************************************/
  public void setMaximumSize(Dimension dimension);


  /***********************************************************************************************
  Sets the minimum size of this <code>WSComponent</code>
  @param dimension the new minimum size
  ***********************************************************************************************/
  public void setMinimumSize(Dimension dimension);


  /***********************************************************************************************
  Sets whether this <code>WSComponent</code> is opaque or not
  @param opaque <b>true</b> if this <code>WSComponent</code> is opaque<br />
                 <b>false</b> if this <code>WSComponent</code> is not opaque.
  ***********************************************************************************************/
  public void setOpaque(boolean opaque);


  /***********************************************************************************************
  Sets the position attribute value. <i>This does not actually set the position of the 
  <code>WSComponent</code>!</i>
  @param position the new position attribute value
  ***********************************************************************************************/
  public void setPosition(String position);


  /***********************************************************************************************
  Sets the preferred size of this <code>WSComponent</code>
  @param dimension the new preferred size
  ***********************************************************************************************/
  public void setPreferredSize(Dimension dimension);


  /***********************************************************************************************
  Sets whether this <code>WSComponent</code> is visible or not
  @param visible <b>true</b> if this <code>WSComponent</code> is visible<br />
                 <b>false</b> if this <code>WSComponent</code> is not visible.
  ***********************************************************************************************/
  public void setVisible(boolean visible);


  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  public void toComponent(XMLNode node);


  /***********************************************************************************************
  Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
  @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
  ***********************************************************************************************/
  public XMLNode toXML();
}