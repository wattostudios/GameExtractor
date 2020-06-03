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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.JComponent;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSEvent;
import org.watto.event.WSKeyableInterface;
import org.watto.xml.XMLNode;
import org.watto.xml.XMLReader;

/***********************************************************************************************
 * A Gradient Color Chooser GUI <code>Component</code>
 ***********************************************************************************************/

public class WSGradientColorChooser extends WSPanel implements WSClickableInterface, WSKeyableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The <code>WSGradientColorPanel</code> **/
  WSGradientColorPanel colorPanel;
  /** The <code>WSGradientColorSlider</code> **/
  WSGradientColorSlider colorSlider;
  /** The color slider bar **/
  WSSlider colorSliderBar;

  /** The red color field **/
  WSTextField colorRed;
  /** The green color field **/
  WSTextField colorGreen;
  /** The blue color field **/
  WSTextField colorBlue;
  /** The hex color field **/
  WSTextField colorHex;

  /** The preview <code>WSColorPanel</code> **/
  WSColorPanel colorPreview;

  /***********************************************************************************************
   * Constructor for extended classes only
   ***********************************************************************************************/
  public WSGradientColorChooser() {
    super();
  }

  /***********************************************************************************************
   * Builds a <code>WSColorPanel</code> and sets the properties from the <code>node</code>
   * @param node the <code>XMLNode</code> used to construct the <code>WSColorPanel</code>
   ***********************************************************************************************/
  public WSGradientColorChooser(XMLNode node) {
    super();
    toComponent(node);
    registerEvents();
    setInitialised(true); // so events aren't triggered until after setting the initial properties
  }

  /***********************************************************************************************
   * Gets the current <code>Color</code>
   * @return the <code>color</code>
   ***********************************************************************************************/
  public Color getColor() {
    return colorPanel.getSelectedColor();
  }

  /***********************************************************************************************
   * Sets the global values after the <code>WSComponent</code>s have been constructed from the
   * XML
   ***********************************************************************************************/
  public void loadGlobals() {
    // referencing the components
    colorPanel = (WSGradientColorPanel) ComponentRepository.get("GradientColorPanel");
    colorSlider = (WSGradientColorSlider) ComponentRepository.get("GradientColorSlider");
    colorSliderBar = (WSSlider) ComponentRepository.get("GradientColorSliderBar");
    colorSliderBar.setMinimum(0);
    colorSliderBar.setMaximum(255);

    colorRed = (WSTextField) ComponentRepository.get("GradientColorChooserRedValue");
    colorGreen = (WSTextField) ComponentRepository.get("GradientColorChooserGreenValue");
    colorBlue = (WSTextField) ComponentRepository.get("GradientColorChooserBlueValue");
    colorHex = (WSTextField) ComponentRepository.get("GradientColorChooserHexValue");

    colorPreview = (WSColorPanel) ComponentRepository.get("GradientColorChooserSelectedColor");
  }

  /***********************************************************************************************
   * Performs an action when a <code>MouseEvent</code> event is triggered
   * @param source the <code>JComponent</code> that triggered the event
   * @param event the <code>MouseEvent</code>
   * @return <b>true</b> if the event was handled by this class<br />
   *         <b>false</b> if the event wasn't handled by this class, and thus should be passed on
   *         to the parent class for handling.
   ***********************************************************************************************/
  @Override
  public boolean onClick(JComponent source, MouseEvent event) {
    if (source == colorSliderBar) {
      setColorFromSlider();
    }
    else if (source == colorPanel) {
      setColorFromPanel(event.getX(), event.getY());
    }
    else if (source == colorSlider) {
      colorSliderBar.setValue(255 - event.getY());
      setColorFromSlider();
    }
    else {
      return false;
    }
    return true;
  }

  /***********************************************************************************************
   * Performs an action when a <code>KeyEvent</code> pressed event is triggered
   * @param source the <code>JComponent</code> that triggered the event
   * @param event the <code>KeyEvent</code>
   * @return <b>true</b> if the event was handled by this class<br />
   *         <b>false</b> if the event wasn't handled by this class, and thus should be passed on
   *         to the parent class for handling.
   ***********************************************************************************************/
  @Override
  public boolean onKeyPress(JComponent source, KeyEvent event) {
    if (source == colorSliderBar) {
      setColorFromSlider();
    }
    else {
      return false;
    }
    return true;
  }

  /***********************************************************************************************
   * Sets the current <code>Color</code>
   * @param color the <code>color</code>
   ***********************************************************************************************/
  public void setColor(Color color) {
    setColor(color, true);
  }

  /***********************************************************************************************
   * Sets the current <code>Color</code>
   * @param color the <code>color</code>
   * @param fireChangeEvent whether to fire an event or not
   ***********************************************************************************************/
  public void setColor(Color color, boolean fireChangeEvent) {
    colorSlider.setColor(color);
    colorPanel.setSelectedColor(color);
    colorPanel.setColor(colorSlider.getColor());
    colorSliderBar.setValue(255 - colorSlider.getDotPos());
    colorPanel.repaint();

    updateReportingFields(color, fireChangeEvent);
  }

  /***********************************************************************************************
   * Sets the current <code>Color</code>
   * @param colorRGB the <code>color</code> as an RGB value
   ***********************************************************************************************/
  public void setColor(int colorRGB) {
    setColor(new Color(colorRGB));
  }

  /***********************************************************************************************
   * Sets the current <code>Color</code>
   * @param red the <code>color</code> red value
   * @param green the <code>color</code> green value
   * @param blue the <code>color</code> blue value
   ***********************************************************************************************/
  public void setColor(int red, int green, int blue) {
    setColor(new Color(red, green, blue));
  }

  /***********************************************************************************************
   * Sets the current <code>Color</code> from a click on the gradient panel
   * @param x the x position of the mouse click
   * @param y the y position of the mouse click
   ***********************************************************************************************/
  public void setColorFromPanel(int x, int y) {
    colorPanel.onClick(x, y);
    colorPanel.repaint();

    updateReportingFields(colorPanel.getSelectedColor());
  }

  /***********************************************************************************************
   * Sets the current <code>Color</code> from a click on the slider panel
   ***********************************************************************************************/
  public void setColorFromSlider() {
    colorSlider.setDotPos(255 - colorSliderBar.getValue());

    Color color = colorSlider.getColor();
    colorPanel.setSelectedColor(color);
    colorPanel.setColor(color);

    updateReportingFields(color);

    repaint();
  }

  /***********************************************************************************************
   * Builds this <code>WSComponent</code> from the properties of the <code>node</code>
   * @param node the <code>XMLNode</code> that describes the <code>WSComponent</code> to
   *        construct
   ***********************************************************************************************/
  @Override
  public void toComponent(XMLNode node) {
    //super.toComponent(node);

    XMLNode srcNode = XMLReader.read(new File(Settings.getString("WSGradientColorChooserXML")));
    super.toComponent(srcNode);

    setOpaque(true);

    /*
     * // TODO CHECK IF THIS IS NEEDED, AS PER BELOW COMMENT setLayout(new BorderLayout());
     *
     * // Build an XMLNode tree containing all the elements on the screen TO DELETE!!! XMLNode
     * srcNode = XMLReader.read(new File(Settings.getString("WSGradientColorChooserXML")));
     *
     * // Build the components from the XMLNode tree Component component =
     * WSHelper.toComponent(srcNode); add(component,BorderLayout.CENTER);
     *
     * // setting up this object in the repository setCode(((WSComponent)component).getCode());
     * //ComponentRepository.add(this);
     */

    loadGlobals();

    String tag;

    int red = 0;
    int green = 0;
    int blue = 0;

    tag = node.getAttribute("red");
    if (tag != null) {
      red = WSHelper.parseInt(tag);
    }

    tag = node.getAttribute("green");
    if (tag != null) {
      green = WSHelper.parseInt(tag);
    }

    tag = node.getAttribute("blue");
    if (tag != null) {
      blue = WSHelper.parseInt(tag);
    }

    if (red == -1) {
      red = 0;
    }
    if (green == -1) {
      green = 0;
    }
    if (blue == -1) {
      blue = 0;
    }

    setColor(red, green, blue);
  }

  /***********************************************************************************************
   * Updates the <code>WSTextField</code>s when a color has changed
   * @param color the <code>Color</code>
   ***********************************************************************************************/
  public void updateReportingFields(Color color) {
    updateReportingFields(color, true);
  }

  /***********************************************************************************************
   * Updates the <code>WSTextField</code>s when a color has changed
   * @param color the <code>Color</code>
   * @param fireChangeEvent whether to fire an event or not
   ***********************************************************************************************/
  public void updateReportingFields(Color color, boolean fireChangeEvent) {
    int red = color.getRed();
    int green = color.getGreen();
    int blue = color.getBlue();

    colorRed.setText("" + red);
    colorGreen.setText("" + green);
    colorBlue.setText("" + blue);

    String hex = "#";

    String hexValue = Integer.toHexString(red);
    if (hexValue.length() == 1) {
      hexValue = "0" + hexValue;
    }
    hex += hexValue;

    hexValue = Integer.toHexString(green);
    if (hexValue.length() == 1) {
      hexValue = "0" + hexValue;
    }
    hex += hexValue;

    hexValue = Integer.toHexString(blue);
    if (hexValue.length() == 1) {
      hexValue = "0" + hexValue;
    }
    hex += hexValue;

    colorHex.setText(hex.toUpperCase());

    colorPreview.setColor(red, green, blue);
    colorPreview.repaint();

    Settings.set("WSGradientColorChooser_Color_Selected", color.getRGB());

    if (fireChangeEvent) {
      WSHelper.fireEvent(new WSEvent(this, WSEvent.COLOR_CHANGED), this);
    }
  }

}