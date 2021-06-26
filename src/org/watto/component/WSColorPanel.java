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
import javax.swing.plaf.PanelUI;
import org.watto.ErrorLogger;
import org.watto.plaf.ButterflyColorPanelUI;
import org.watto.xml.XMLNode;

/***********************************************************************************************
A Color Panel GUI <code>Component</code>
***********************************************************************************************/

public class WSColorPanel extends WSPanel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** The current <code>Color</code> **/
  Color color;

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSColorPanel() {
    super();
  }

  /***********************************************************************************************
  Builds a <code>WSColorPanel</code> and sets the properties from the <code>node</code>
  @param node the <code>XMLNode</code> used to construct the <code>WSColorPanel</code>
  ***********************************************************************************************/
  public WSColorPanel(XMLNode node) {
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
  Sets the GUI renderer for this <code>Component</code>. Overwritten to force the use of
  <code>ButterflyColorPanelUI</code>
  @param ui <i>not used</i>
  TODO - need to make generic
  ***********************************************************************************************/
  @Override
  public void setUI(PanelUI ui) {
    super.setUI(ButterflyColorPanelUI.createUI(this));
  }

  /***********************************************************************************************
  Builds this <code>WSComponent</code> from the properties of the <code>node</code>
  @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to construct
  ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    super.toComponent(node);

    String tag;

    int red = 0;
    int green = 0;
    int blue = 0;

    try {
      tag = node.getAttribute("red");
      if (tag != null) {
        red = Integer.parseInt(tag);
      }

      tag = node.getAttribute("green");
      if (tag != null) {
        green = Integer.parseInt(tag);
      }

      tag = node.getAttribute("blue");
      if (tag != null) {
        blue = Integer.parseInt(tag);
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    setColor(red, green, blue);
    setOpaque(false);
  }

  /***********************************************************************************************
  Constructs an <code>XMLNode</code> representation of this <code>WSComponent</code>
  @return the <code>XMLNode</code> representation of this <code>WSComponent</code>
  ***********************************************************************************************/
  @Override
  public XMLNode toXML() {
    XMLNode node = super.toXML();

    node.setAttribute("red", "" + color.getRed());
    node.setAttribute("green", "" + color.getGreen());
    node.setAttribute("blue", "" + color.getBlue());

    return node;
  }

}