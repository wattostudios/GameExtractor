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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/**
**********************************************************************************************
A ExtendedTemplate
**********************************************************************************************
**/
public class WSOptionFileChooser extends WSOptionPanel implements WSKeyableInterface,
    WSClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  WSTextField textField;

  /**
  **********************************************************************************************
  Constructor to construct the component from an XMLNode <i>tree</i>
  @param node the XMLNode describing this component
  **********************************************************************************************
  **/
  public WSOptionFileChooser(XMLNode node) {
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

        JFileChooser fileChooser = new JFileChooser(Settings.getString(setting));
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
          // pressed cancel
          return true;
        }

        String path = fileChooser.getSelectedFile().getAbsolutePath();
        textField.setText(path);

        onKeyPress(c, null);

        return true;
      }
    }
    return false;
  }

  /**
  **********************************************************************************************
  The event that is triggered from a WSSelectableListener when an item is selected
  @param c the component that triggered the event
  @param e the event that occurred
  **********************************************************************************************
  **/
  @Override
  public boolean onKeyPress(JComponent c, KeyEvent e) {
    Settings.set(setting, textField.getText());
    return true;
  }

  /**
  **********************************************************************************************
  Build this object from the <i>node</i>
  @param node the XML node that indicates how to build this object
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