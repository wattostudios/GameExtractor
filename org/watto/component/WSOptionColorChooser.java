/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.plaf.LookAndFeel;
import org.watto.plaf.LookAndFeelManager;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
 **********************************************************************************************
 * A ExtendedTemplate
 **********************************************************************************************
 **/
public class WSOptionColorChooser extends WSOptionPanel implements WSKeyableInterface,
    WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  WSTextField textField;

  WSGradientColorChooser colorChooser;
  WSPopupMenu popup;

  /**
   **********************************************************************************************
   * Constructor to construct the component from an XMLNode <i>tree</i>
   * @param node the XMLNode describing this component
   **********************************************************************************************
   **/
  public WSOptionColorChooser(XMLNode node) {
    // NEED TO DO THIS HERE, OTHERWISE THE SETTING VARIABLE DOESN'T GET SAVED!!! (not sure why)
    //super(node);
    super();
    toComponent(node);
    registerEvents();
  }

  ///////////////
  //
  // Class-Specific Methods
  //
  ///////////////

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void changeColor(String color) {
    if (color == null || color.length() <= 0) {
      color = Settings.get("DefaultInterfaceColors");
    }

    try {
      new Color(Integer.parseInt(color));
    }
    catch (Throwable t) {
      color = Settings.get("DefaultInterfaceColors");
      try {
        new Color(Integer.parseInt(color));
      }
      catch (Throwable t2) {
        color = "0";
      }
    }

    //Settings.set(setting, color);

    LookAndFeel laf = LookAndFeelManager.getLookAndFeel();
    Settings.set("Theme_" + laf.getName() + "_InterfaceColors", color);
    laf.generateColors(new Color(Integer.parseInt(color)));

    textField.repaint();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public String getComparison() {
    return textField.getLabel();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, MouseEvent e) {
    if (c instanceof WSComponent) {
      String code = ((WSComponent) c).getCode();
      if (code.equals("Choose")) {

        //// using the normal color chooser
        //Color newColor = new JColorChooser().showDialog(null,Language.get("ColorChooserTitle"),Color.BLACK);

        // Using WSGradientColorChooser
        colorChooser = new WSGradientColorChooser(new XMLNode());
        colorChooser.setColor(new Color(Settings.getInt("DefaultInterfaceColors")));

        WSPanel panel = new WSPanel(new XMLNode());
        panel.add(colorChooser, BorderLayout.CENTER);
        panel.add(new WSButton(XMLReader.read("<WSButton code=\"ColorChooserClosePopup\" />")), BorderLayout.SOUTH);

        popup = new WSPopupMenu(new XMLNode());
        popup.add(panel);
        popup.setOpaque(true);

        popup.show(this, 0, 0);
      }
      else if (code.equals("ColorChooserClosePopup")) {
        //closing the popup happens automatically, we just want to trigger the setting of the new color
        if (colorChooser != null) {
          popup.setVisible(false);

          int colorVal = colorChooser.getColor().getRGB();
          textField.setText("" + colorVal);

          changeColor("" + colorVal);
        }
      }
      else {
        return false;
      }
      return true;

    }
    return false;
  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSSelectableListener when an item is selected
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onKeyPress(JComponent c, KeyEvent e) {
    changeColor(textField.getText());
    return true;
  }

  /**
   **********************************************************************************************
   * Build this object from the <i>node</i>
   * @param node the XML node that indicates how to build this object
   **********************************************************************************************
   **/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    setLayout(new BorderLayout());

    textField = new WSTextField(XMLReader.read("<WSTextField code=\"" + getSetting() + "\" showLabel=\"true\" />"));
    add(textField, BorderLayout.CENTER);

    WSButton button = new WSButton(XMLReader.read("<WSButton code=\"Choose\" />"));
    add(button, BorderLayout.EAST);

    textField.setText(Settings.get(getSetting()));

    //if (icon != null){
    //  JLabel label = new JLabel(icon);
    //  add(label,BorderLayout.WEST);
    //  }
  }

}