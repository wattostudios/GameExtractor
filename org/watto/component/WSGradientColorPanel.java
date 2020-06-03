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

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.plaf.PanelUI;
import org.watto.plaf.ButterflyGradientColorPanelUI;
import org.watto.xml.XMLNode;

/***********************************************************************************************
A Gradient Color Panel GUI <code>Component</code>
***********************************************************************************************/

public class WSGradientColorPanel extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** The current <code>Color</code> in the top-left, which is used as the base of the gradient **/
  Color color = Color.RED;
  /** The currently selected <code>Color</code> **/
  Color selectedColor = Color.RED;
  /** The position of the dot selection mark **/
  Dimension dotPos = null;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSGradientColorPanel() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSColorPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSColorPanel</code>
  ***********************************************************************************************/
  public WSGradientColorPanel(XMLNode node) {
    super(node);
  }

  /***********************************************************************************************
  Gets the current <code>Color</code>
  @return the <code>color</code>
  ***********************************************************************************************/
  public Color getColor() {
    return color;
  }

  /***********************************************************************************************
  Gets the <code>Color</code> at a given point in the gradient
  @param x the x position
  @param y the y position
  @return the <code>Color</code> at the point
  ***********************************************************************************************/
  public Color getColorAtPoint(int x, int y) {

    if (x > 255) {
      x = 255;
    }
    if (y > 255) {
      y = 255;
    }

    if (x < 0) {
      x = 0;
    }
    if (y < 0) {
      y = 0;
    }

    x = 255 - x;
    y = 255 - y;

    double red = color.getRed();
    double green = color.getGreen();
    double blue = color.getBlue();

    double redBlack = 255 - ((double) y) / 255 * red;
    double greenBlack = 255 - ((double) y) / 255 * green;
    double blueBlack = 255 - ((double) y) / 255 * blue;

    int redBlackWhite = (int) (255 - ((255 - ((double) x)) / 255 * redBlack));
    int greenBlackWhite = (int) (255 - ((255 - ((double) x)) / 255 * greenBlack));
    int blueBlackWhite = (int) (255 - ((255 - ((double) x)) / 255 * blueBlack));

    return new Color(redBlackWhite, greenBlackWhite, blueBlackWhite);
  }

  /***********************************************************************************************
  Gets the position of the dot marker
  @return the position of the dot
  ***********************************************************************************************/
  public Dimension getDotPos() {
    return dotPos;
  }

  /***********************************************************************************************
  Gets the maximum size of this <code>WSComponent</code>
  @return the maximum size of <b>new Dimension(256,256)</b>
  ***********************************************************************************************/
  @Override
  public Dimension getMaximumSize() {
    return new Dimension(256, 256);
  }

  /***********************************************************************************************
  Gets the minimum size of this <code>WSComponent</code>
  @return the minimum size of <b>new Dimension(256,256)</b>
  ***********************************************************************************************/
  @Override
  public Dimension getMinimumSize() {
    return new Dimension(256, 256);
  }

  /***********************************************************************************************
  Gets the preferred size of this <code>WSComponent</code>
  @return the preferred size of <b>new Dimension(256,256)</b>
  ***********************************************************************************************/
  @Override
  public Dimension getPreferredSize() {
    return new Dimension(256, 256);
  }

  /***********************************************************************************************
  Gets the currently selected <code>Color</code>
  @return the selected <code>color</code>
  ***********************************************************************************************/
  public Color getSelectedColor() {
    return selectedColor;
  }

  /***********************************************************************************************
  Sets the new selected <code>Color</code> when the user click a point in the gradient
  @param x the x position
  @param y the y position
  ***********************************************************************************************/
  public void onClick(int x, int y) {
    dotPos = new Dimension(x, y);
    setSelectedColor(getColorAtPoint(x, y));
  }

  /***********************************************************************************************
  Sets the current <code>Color</code>
  @param color the <code>Color</code>
  ***********************************************************************************************/
  public void setColor(Color color) {
    this.color = color;
    dotPos = null;
  }

  /***********************************************************************************************
  Sets the current <code>Color</code> as given in RGB format
  @param colorRGB the <code>Color</code>
  ***********************************************************************************************/
  public void setColor(int colorRGB) {
    setColor(new Color(colorRGB));
  }

  /***********************************************************************************************
  Sets the current <code>Color</code>
  @param red the <code>Color</code> red value
  @param green the <code>Color</code> green value
  @param blue the <code>Color</code> blue value
  ***********************************************************************************************/
  public void setColor(int red, int green, int blue) {
    setColor(new Color(red, green, blue));
  }

  /***********************************************************************************************
  Sets the currently selected <code>Color</code>
  @param selectedColor the <code>Color</code>
  ***********************************************************************************************/
  public void setSelectedColor(Color selectedColor) {
    this.selectedColor = selectedColor;
  }

  /***********************************************************************************************
  Sets the currently selected <code>Color</code> as given in RGB format
  @param colorRGB the <code>Color</code>
  ***********************************************************************************************/
  public void setSelectedColor(int colorRGB) {
    selectedColor = new Color(colorRGB);
  }

  /***********************************************************************************************
  Sets the currently selected <code>Color</code>
  @param red the <code>Color</code> red value
  @param green the <code>Color</code> green value
  @param blue the <code>Color</code> blue value
  ***********************************************************************************************/
  public void setSelectedColor(int red, int green, int blue) {
    selectedColor = new Color(red, green, blue);
  }

  /***********************************************************************************************
  Sets the GUI renderer for this <code>Component</code>. Overwritten to force the use of
  <code>ButterflyGradientColorPanelUI</code>
  @param ui <i>not used</i>
  TODO - need to make generic
  ***********************************************************************************************/
  @Override
  public void setUI(PanelUI ui) {
    super.setUI(ButterflyGradientColorPanelUI.createUI(this));
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);
  }

  /***********************************************************************************************
  Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
  @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    return super.toXML();
  }

}