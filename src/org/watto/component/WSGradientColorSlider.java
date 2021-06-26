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
import org.watto.plaf.ButterflyGradientColorSliderUI;
import org.watto.xml.XMLNode;

/***********************************************************************************************
A Gradient Color Slider GUI <code>Component</code>
***********************************************************************************************/

public class WSGradientColorSlider extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** The current <code>Color</code> **/
  Color color = new Color(90, 100, 110);
  /** Whether to show the dot marker or not **/
  boolean showDot = false;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSGradientColorSlider() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSColorPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSColorPanel</code>
  ***********************************************************************************************/
  public WSGradientColorSlider(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
  Gets the current <code>Color</code>
  @return the <code>color</code>
  ***********************************************************************************************/
  public Color getColor() {
    return color;
  }

  /***********************************************************************************************
  Gets the position of the dot on the slider
  @return the dot position
  ***********************************************************************************************/
  public int getDotPos() {

    double increment42 = ((double) 255 / 42);
    double increment43 = ((double) 255 / 43);

    int selRed = color.getRed();
    int selGreen = color.getGreen();
    int selBlue = color.getBlue();

    // determine where the current color is located
    int dotPos = 0;
    if (selRed >= selGreen && selRed >= selBlue) {
      if (selGreen >= selBlue) {
        // 1 = Red, 2 = Green      Yellow --> Red
        dotPos = 212 + (int) ((255 - selGreen) / increment43);
      }
      else {
        // 1 = Red, 2 = Blue       Red --> Purple
        dotPos = 0 + (int) (selBlue / increment42);
      }
    }
    else if (selGreen >= selRed && selGreen >= selBlue) {
      if (selRed >= selBlue) {
        // 1 = Green, 2 = Red      Green --> Yellow
        dotPos = 170 + (int) (selRed / increment42);
      }
      else {
        // 1 = Green, 2 = Blue     Cyan --> Green
        dotPos = 127 + (int) ((255 - selBlue) / increment43);
      }
    }
    else if (selBlue >= selRed && selBlue >= selGreen) {
      if (selRed >= selGreen) {
        // 1 = Blue, 2 = Red       Purple --> Blue
        dotPos = 42 + (int) ((255 - selRed) / increment43);
      }
      else {
        // 1 = Blue, 2 = Green     Blue --> Cyan
        dotPos = 85 + (int) (selGreen / increment42);
      }
    }

    return dotPos;
  }

  /***********************************************************************************************
  Gets the maximum size of this <code>WSComponent</code>
  @return the maximum size
  ***********************************************************************************************/
  @Override
  public Dimension getMaximumSize() {
    return getMinimumSize();
  }

  /***********************************************************************************************
  Gets the minimum size of this <code>WSComponent</code>
  @return the minimum size
  ***********************************************************************************************/
  @Override
  public Dimension getMinimumSize() {
    if (showDot) {
      return new Dimension(25, 256);
    }
    else {
      return new Dimension(20, 256);
    }
  }

  /***********************************************************************************************
  Gets the preferred size of this <code>WSComponent</code>
  @return the preferred size
  ***********************************************************************************************/
  @Override
  public Dimension getPreferredSize() {
    return getMinimumSize();
  }

  /***********************************************************************************************
  Gets whether the dot marker is shown or not
  @return <b>true</b> if the dot marker is shown<br />
          <b>false</b> if the dot marker is not shown
  ***********************************************************************************************/
  public boolean getShowDot() {
    return showDot;
  }

  /***********************************************************************************************
  Sets the current <code>Color</code>
  @param color the <code>color</code>
  ***********************************************************************************************/
  public void setColor(Color color) {
    this.color = color;
  }

  /***********************************************************************************************
  Sets the current <code>Color</code>
  @param colorRGB the <code>color</code> as an RGB value
  ***********************************************************************************************/
  public void setColor(int colorRGB) {
    color = new Color(colorRGB);
  }

  /***********************************************************************************************
  Sets the current <code>Color</code>
  @param red the <code>color</code> red value
  @param green the <code>color</code> green value
  @param blue the <code>color</code> blue value
  ***********************************************************************************************/
  public void setColor(int red, int green, int blue) {
    color = new Color(red, green, blue);
  }

  /***********************************************************************************************
  Sets the position of the dot on the slider
  @param dotPos the position of the dot
  ***********************************************************************************************/
  public void setDotPos(int dotPos) {

    double increment42 = ((double) 255 / 42);
    double increment43 = ((double) 255 / 43);

    int selRed = 0;
    int selGreen = 0;
    int selBlue = 0;

    // determine where the current color is located
    if (dotPos > 212) {
      // 1 = Red, 2 = Green      Yellow --> Red
      dotPos -= 212;
      selGreen = 255 - (int) (dotPos * increment43);
      selRed = 255;// - selGreen;
    }
    else if (dotPos > 170) {
      // 1 = Green, 2 = Red      Green --> Yellow
      dotPos -= 170;
      selRed = (int) (dotPos * increment42);
      selGreen = 255;// - selRed;
    }
    else if (dotPos > 127) {
      // 1 = Green, 2 = Blue     Cyan --> Green
      dotPos -= 127;
      selBlue = 255 - (int) (dotPos * increment43);
      selGreen = 255;// - selBlue;
    }
    else if (dotPos > 85) {
      // 1 = Blue, 2 = Green     Blue --> Cyan
      dotPos -= 85;
      selGreen = (int) (dotPos * increment42);
      selBlue = 255;// - selGreen;
    }
    else if (dotPos > 42) {
      // 1 = Blue, 2 = Red       Purple --> Blue
      dotPos -= 42;
      selRed = 255 - (int) (dotPos * increment43);
      selBlue = 255;// - selRed;
    }
    else if (dotPos > 0) {
      // 1 = Red, 2 = Blue       Red --> Purple
      dotPos -= 0;
      selBlue = (int) (dotPos * increment42);
      selRed = 255;// - selBlue;
    }

    if (selRed == 0 && selGreen == 0 && selBlue == 0) {
      selRed = 255;
    }

    setColor(selRed, selGreen, selBlue);

  }

  /***********************************************************************************************
  Sets whether the dot marker is shown or not
  @param showDot <b>true</b> if the dot marker is shown<br />
                 <b>false</b> if the dot marker is not shown
  ***********************************************************************************************/
  public void setShowDot(boolean showDot) {
    this.showDot = showDot;
  }

  /***********************************************************************************************
  Sets the GUI renderer for this <code>Component</code>. Overwritten to force the use of
  <code>ButterflyGradientColorSliderUI</code>
  @param ui <i>not used</i>
  TODO - need to make generic
  ***********************************************************************************************/
  @Override
  public void setUI(PanelUI ui) {
    super.setUI(ButterflyGradientColorSliderUI.createUI(this));
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    String tag = node.getAttribute("showDot");
    if (tag != null) {
      setShowDot(WSHelper.parseBoolean(tag));
    }
  }

  /***********************************************************************************************
  Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
  @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    XMLNode node = super.toXML();

    if (showDot) {
      node.addAttribute("showDot", "true");
    }

    return node;
  }

}