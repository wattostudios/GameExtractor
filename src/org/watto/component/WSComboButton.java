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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import org.watto.ErrorLogger;
import org.watto.event.WSClickableInterface;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
A Combo Button GUI <code>Component</code>
***********************************************************************************************/

public class WSComboButton extends WSPanel implements WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** the main <code>WSButton</code> **/
  WSButton mainButton;
  /** The arrow <code>WSButton</code> used to show the <code>WSPopupMenu</code> **/
  WSButton arrowButton;

  /** The popup menu **/
  WSPopupMenu popupMenu;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSComboButton() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSPanel</code>
  ***********************************************************************************************/
  public WSComboButton(XMLNode node) {
    super(node);
  }

  /***********************************************************************************************
  Gets the text code for the <code>mainButton</code>, which is used for <code>Language</code>s
  and other functionality
  @return the text code for the <code>mainButton</code>
  ***********************************************************************************************/
  public String getMainCode() {
    return mainButton.getCode();
  }

  /***********************************************************************************************
  Gets the <code>Language</code> small text of the <code>mainButton</code>
  @return the small text
  ***********************************************************************************************/
  public String getMainSmallText() {
    return mainButton.getSmallText();
  }

  /***********************************************************************************************
  Gets the <code>Language</code> text of the <code>mainButton</code>
  @return the text
  ***********************************************************************************************/
  public String getMainText() {
    return mainButton.getText();
  }

  /***********************************************************************************************
  Gets the <code>Language</code> tooltip text of the <code>mainButton</code>
  @return the tooltip text
  ***********************************************************************************************/
  public String getMainToolTipText() {
    return mainButton.getToolTipText();
  }

  /***********************************************************************************************
  Gets the maximum size of this <code>WSComponent</code>
  @return the maximum size
  ***********************************************************************************************/
  @Override
  public Dimension getMaximumSize() {
    return new Dimension((int) (mainButton.getPreferredSize().getWidth() + arrowButton.getPreferredSize().getWidth()), (int) mainButton.getPreferredSize().getHeight());
  }

  /***********************************************************************************************
  Performs an action when a <code>MouseEvent</code> event is triggered
  @param source the <code>JComponent</code> that triggered the event
  @param event the <code>MouseEvent</code>
  @return <b>true</b> if the event was handled by this class<br />
          <b>false</b> if the event wasn't handled by this class, and thus should be passed on to
          the parent class for handling.
  ***********************************************************************************************/
  @Override
  public boolean onClick(JComponent source, MouseEvent event) {
    if (source == arrowButton) {
      // the arrow button was clicked
      if (popupMenu.isVisible()) {
        popupMenu.setVisible(false);
      }
      else {
        popupMenu.show(this, 0, mainButton.getHeight());
      }
      return true;
    }
    return false;
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    if (node.getAttribute("opaque") == null) {
      setOpaque(false);
    }

    try {
      mainButton = new WSButton(node.getChild("WSButton"));
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    arrowButton = new WSButton(XMLReader.read("<WSButton code=\"ComboArrowButton\" />"));

    add(mainButton, BorderLayout.CENTER);
    add(arrowButton, BorderLayout.EAST);

    try {
      popupMenu = new WSPopupMenu(node.getChild("WSPopupMenu"));
    }
    catch (Throwable t) {
      popupMenu = new WSPopupMenu(new XMLNode());
      //ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
  Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
  @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    XMLNode node = WSHelper.toXML(this);

    node.addChild(mainButton.toXML());
    node.addChild(popupMenu.toXML());

    return node;
  }

}