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
import org.watto.Language;
import org.watto.Settings;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
A Color Information Panel GUI <code>Component</code>
***********************************************************************************************/

public class WSColorInfoPanel extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The <code>Color</code> display panel **/
  WSColorPanel colorBox;
  /** The name of the <code>Color</code> **/
  WSLabel colorName;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSColorInfoPanel() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSColorPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSColorPanel</code>
  ***********************************************************************************************/
  public WSColorInfoPanel(XMLNode node) {
    super(node);
  }

  /***********************************************************************************************
  Gets the current <code>Color</code> shown in the <code>colorBox</code>
  @return the <code>Color</code>
  ***********************************************************************************************/
  public Color getColor() {
    return colorBox.getColor();
  }

  /***********************************************************************************************
  Sets the current <code>Color</code> shown in the <code>colorBox</code>
  @param color the <code>Color</code>
  ***********************************************************************************************/
  public void setColor(Color color) {
    int red = color.getRed();
    int green = color.getGreen();
    int blue = color.getBlue();
    setColor(red, green, blue);
  }

  /***********************************************************************************************
  Sets the current <code>Color</code> shown in the <code>colorBox</code>
  @param red the <code>color</code> red value
  @param green the <code>color</code> green value
  @param blue the <code>color</code> blue value
  ***********************************************************************************************/
  public void setColor(int red, int green, int blue) {
    // looking up the color name
    String colorCode = red + "," + green + "," + blue;
    String name = colorCode;

    if (Language.has("WSColorChooser_Color_" + colorCode)) {
      name = Language.get("WSColorChooser_Color_" + colorCode);
    }

    colorName.setText(name);

    colorBox.setColor(red, green, blue);

    revalidate();
    repaint();
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    // getting last selected color
    int red = Settings.getInt("WSColorChooser_ColorRed_Selected");
    int green = Settings.getInt("WSColorChooser_ColorGreen_Selected");
    int blue = Settings.getInt("WSColorChooser_ColorBlue_Selected");

    if (red == -1) {
      red = 0;
    }
    if (green == -1) {
      green = 0;
    }
    if (blue == -1) {
      blue = 0;
    }

    // set the color
    colorName = new WSLabel(XMLReader.read("<WSLabel code=\"ColorInfoPanelName\" />"));
    colorName.setOpaque(false);

    colorBox = new WSColorPanel(XMLReader.read("<WSColorPanel width=\"15\" height=\"15\" />"));

    setColor(red, green, blue);

    // build the panel
    setLayout(new BorderLayout(4, 4));

    add(colorName, BorderLayout.CENTER);
    add(colorBox, BorderLayout.WEST);

    setOpaque(false);
  }

}