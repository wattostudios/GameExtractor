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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSEvent;
import org.watto.event.WSHoverableInterface;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
A Small Color Chooser GUI <code>Component</code>
***********************************************************************************************/

public class WSSmallColorChooser extends WSPanel implements WSClickableInterface, WSHoverableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** The <code>WSColorInfoPanel</code> on the popup **/
  WSColorInfoPanel infoPanel; // on the popup
  /** The <code>WSColorInfoPanel</code> on the main panel **/
  WSColorInfoPanel infoPanelMain; // on the main panel

  /** The arrow <code>WSButton</code> that shows the popup **/
  WSButton arrowButton;
  /** The popup **/
  WSPopupMenu popupMenu;

  /** The <code>WSGradientColorChooser</code> if the user chooses to pick a full color **/
  WSGradientColorChooser colorChooser;
  /** The <code>WSGradientColorChooser</code> popup **/
  WSPopupMenu popup;

  /** Whether the popup is open or not **/
  boolean popupOpen = false;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSSmallColorChooser() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSColorPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSColorPanel</code>
  ***********************************************************************************************/
  public WSSmallColorChooser(XMLNode node) {
    super(node);
  }

  /***********************************************************************************************
  Closes the popup
  ***********************************************************************************************/
  public void closePopup() {
    popupOpen = false;
    popupMenu.setVisible(false);
  }

  /***********************************************************************************************
  Gets the current <code>Color</code>
  @return the <code>Color</code>
  ***********************************************************************************************/
  public Color getColor() {
    return infoPanelMain.getColor();
  }

  /***********************************************************************************************
  Gets the maximum size of this <code>WSComponent</code>
  @return the maximum size
  ***********************************************************************************************/
  @Override
  public Dimension getMaximumSize() {
    return new Dimension((int) (infoPanel.getPreferredSize().getWidth() + arrowButton.getPreferredSize().getWidth()), (int) infoPanel.getPreferredSize().getHeight());
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
    if (source instanceof JButton) {
      if (source == arrowButton) {
        // the arrow button was clicked
        if (popupOpen) {
          closePopup();
        }
        else {
          openPopup();
        }
      }
      else if (source instanceof WSButton) {
        String code = ((WSButton) source).getCode();

        if (code.equals("ColorChooserClosePopup")) {
          //closing the popup happens automatically, we just want to trigger the setting of the new color
          if (colorChooser != null) {
            popup.setVisible(false);
            setColor(colorChooser.getColor());
            closePopup();
          }
        }
        else if (code.equals("ColorMoreColors")) {
          //// using the normal color chooser
          //Color color = JColorChooser.showDialog(this,Language.get("ColorChooser",false),infoPanel.getColor());

          // Using WSGradientColorChooser
          colorChooser = new WSGradientColorChooser(new XMLNode());
          colorChooser.setColor(getColor());

          WSPanel panel = new WSPanel(new XMLNode());
          panel.add(colorChooser, BorderLayout.CENTER);
          panel.add(new WSButton(XMLReader.read("<WSButton code=\"ColorChooserClosePopup\" />")), BorderLayout.SOUTH);

          popup = new WSPopupMenu(new XMLNode());
          popup.add(panel);
          popup.setOpaque(true);

          popup.show(this, 0, 0);
        }
      }
    }
    else if (source instanceof WSColorPanel) {
      try {
        // set the color
        WSColorPanel panel = (WSColorPanel) source;
        Color color = panel.getColor();

        setColor(color);
        closePopup();
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
        return false;
      }
    }
    else if (source == infoPanelMain) {
      if (popupOpen) {
        closePopup();
      }
      else {
        openPopup();
      }
    }

    return true;
  }

  /***********************************************************************************************
  Performs an action when a <i>start hover</i> <code>MouseEvent</code> event is triggered
  @param source the <code>JComponent</code> that triggered the event
  @param event the <code>MouseEvent</code>
  @return <b>true</b> if the event was handled by this class<br />
          <b>false</b> if the event wasn't handled by this class, and thus should be passed on to
          the parent class for handling.
  ***********************************************************************************************/
  @Override
  public boolean onHover(JComponent source, MouseEvent event) {
    if (source instanceof WSColorPanel) {
      try {
        // set the color
        WSColorPanel panel = (WSColorPanel) source;
        Color color = panel.getColor();

        infoPanel.setColor(color);
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
        return false;
      }
    }

    return true;
  }

  /***********************************************************************************************
  Performs an action when a <i>stop hover</i> <code>MouseEvent</code> event is triggered
  @param source the <code>JComponent</code> that triggered the event
  @param event the <code>MouseEvent</code>
  @return <b>true</b> if the event was handled by this class<br />
          <b>false</b> if the event wasn't handled by this class, and thus should be passed on to
          the parent class for handling.
  ***********************************************************************************************/
  @Override
  public boolean onHoverOut(JComponent source, MouseEvent event) {

    if (!(source instanceof WSColorPanel)) {
      try {
        // Change the color back to the current color when the user leaves something other
        // than a color cell. The reason: each color cell has a few pixels of padding between
        // them - we don't want to flicker back and forth to the current color when simply
        // moving around the color grid - we only want to change the color back to the
        // current color when it moves away from the color cells (thus, out of the color grid)
        infoPanel.setColor(infoPanelMain.getColor());
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
        return false;
      }
    }

    return true;
  }

  /***********************************************************************************************
  Opens the popup
  ***********************************************************************************************/
  public void openPopup() {
    popupOpen = true;
    popupMenu.show(this, 0, arrowButton.getHeight());
  }

  /***********************************************************************************************
  Sets the current <code>Color</code>
  @param color the <code>Color</code>
  ***********************************************************************************************/
  public void setColor(Color color) {
    if (color == null) {
      return;
    }

    int red = color.getRed();
    int green = color.getGreen();
    int blue = color.getBlue();

    Settings.set("WSColorChooser_ColorRed_Selected", red);
    Settings.set("WSColorChooser_ColorGreen_Selected", green);
    Settings.set("WSColorChooser_ColorBlue_Selected", blue);

    infoPanel.setColor(red, green, blue);
    infoPanelMain.setColor(red, green, blue);

    WSHelper.fireEvent(new WSEvent(this, WSEvent.COLOR_CHANGED), this);
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    /* // TODO CHECK IF THIS IS NEEDED, AS PER BELOW COMMENT
    // Build an XMLNode tree containing all the elements on the screen
    TO DELETE!!!    XMLNode srcNode = XMLReader.read(new File(Settings.getString("WSSmallColorChooserXML")));

    // Build the components from the XMLNode tree
    Component component = WSHelper.toComponent(srcNode);
    popupMenu = new WSPopupMenu(XMLReader.read("<WSPopupMenu />"));
    popupMenu.add(component);
    //add(component,BorderLayout.CENTER);
    */

    infoPanel = (WSColorInfoPanel) ComponentRepository.get("ColorCurrentColor");
    arrowButton = new WSButton(XMLReader.read("<WSButton code=\"WSComboButtonArrow\" opaque=\"false\" />"));

    //WSColorInfoPanel infoPanelMain = new WSColorInfoPanel(this);
    //infoPanelMain.setColor(infoPanel.getColor());

    infoPanelMain = new WSColorInfoPanel(XMLReader.read("<WSColorInfoPanel />"));
    infoPanelMain.setOpaque(false);

    WSPanel bp = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" />"));
    bp.setOpaque(false);
    bp.add(infoPanelMain, BorderLayout.CENTER);

    add(bp, BorderLayout.CENTER);
    add(arrowButton, BorderLayout.EAST);

    //infoPanel.setOpaque(false);

  }

}