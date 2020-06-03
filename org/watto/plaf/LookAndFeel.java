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

package org.watto.plaf;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.plaf.metal.MetalLookAndFeel;
import org.watto.ErrorLogger;
import org.watto.Settings;

/***********************************************************************************************
 * An abstract LookAndFeel for GUI <code>Component</code>s
 ***********************************************************************************************/

public abstract class LookAndFeel extends MetalLookAndFeel {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The background <code>Color</code> for <code>Component</code>s **/
  Color backgroundColor = new Color(255, 255, 255);
  /** The darkest foreground <code>Color</code> **/
  Color darkColor = new Color(61, 100, 49);
  /** The <code>Font</code> used for writing on <code>Component</code>s **/
  Font font = Font.decode("Arial");
  /** The <code>Font</code> point size **/
  int fontSize = 8;
  /** The height of the text **/
  int textHeight = 12;
  /** The lightest foreground <code>Color</code> **/
  Color lightColor = new Color(181, 204, 174);
  /** The middle foreground <code>Color</code> **/
  Color midColor = new Color(114, 159, 100);
  /** The name of this <code>LookAndFeel</code> **/
  String name;
  /** The package of this <code>LookAndFeel</code> **/
  String packageName;
  /** The text foreground <code>Color</code> **/
  Color textColor = new Color(0, 0, 0);
  /** Whether to use anti-aliasing when painting? **/
  boolean useAntialias = true;

  /***********************************************************************************************
   * Sets the properties of the <code>LookAndFeel</code> such as the <code>Font</code> and
   * <code>Color</code>s
   * @param name the name of the <code>LookAndFeel</code>
   * @param packageName the package of the <code>LookAndFeel</code>
   ***********************************************************************************************/
  public LookAndFeel(String name, String packageName) {
    this.name = name;
    this.packageName = packageName;

    loadProperties();
  }

  /***********************************************************************************************
   * Calculates the proper text height using the given Graphics
   ***********************************************************************************************/
  public void calculateTextHeight(Graphics g) {
    try {

      FontMetrics metrics = g.getFontMetrics(getFont());
      setTextHeight(metrics.getHeight());

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Generates the <code>lightColor</code>, <code>midColor</code>, and <code>darkColor</code>
   * from a single <code>Color</code> value;
   * @param color the <code>Color</code> to use as the base color
   ***********************************************************************************************/
  public void generateColors(Color color) {
    if (color == null) {
      return;
    }

    int r = color.getRed();
    int g = color.getGreen();
    int b = color.getBlue();

    darkColor = color;

    int rRange = (255 - r) / 5;
    if (rRange > 25) {
      //rRange = 25;
    }

    int gRange = (255 - g) / 5;
    if (gRange > 25) {
      //gRange = 25;
    }

    int bRange = (255 - b) / 5;
    if (bRange > 25) {
      //bRange = 25;
    }

    r += rRange;
    g += gRange;
    b += bRange;

    midColor = new Color(r, g, b);

    r += rRange + rRange;
    g += gRange + gRange;
    b += bRange + bRange;

    lightColor = new Color(r, g, b);

    //System.out.println(darkColor.getRGB());
    //System.out.println(midColor.getRGB());
    //System.out.println(lightColor.getRGB());
    //System.out.println(Color.WHITE.getRGB());
    //System.out.println(Color.BLACK.getRGB());
  }

  /***********************************************************************************************
   * Gets the <code>backgroundColor</code>
   * @return the <code>backgroundColor</code>
   ***********************************************************************************************/
  public Color getBackgroundColor() {
    return backgroundColor;
  }

  /***********************************************************************************************
   * Gets the <code>darkColor</code>
   * @return the <code>darkColor</code>
   ***********************************************************************************************/
  public Color getDarkColor() {
    return darkColor;
  }

  /***********************************************************************************************
   * Gets the <code>font</code>
   * @return the <code>font</code>
   ***********************************************************************************************/
  public Font getFont() {
    return font;
  }

  /***********************************************************************************************
   * Gets the <code>fontSize</code>
   * @return the <code>fontSize</code>
   ***********************************************************************************************/
  public int getFontSize() {
    return fontSize;
  }

  /***********************************************************************************************
   * Gets the <code>name</code> of the <code>LookAndFeel</code>
   * @return the <code>name</code>
   ***********************************************************************************************/
  @Override
  public String getID() {
    return name;
  }

  /***********************************************************************************************
   * Gets the <code>lightColor</code>
   * @return the <code>lightColor</code>
   ***********************************************************************************************/
  public Color getLightColor() {
    return lightColor;
  }

  /***********************************************************************************************
   * Gets the <code>midColor</code>
   * @return the <code>midColor</code>
   ***********************************************************************************************/
  public Color getMidColor() {
    return midColor;
  }

  /***********************************************************************************************
   * Gets the <code>name</code> of the <code>LookAndFeel</code>
   * @return the <code>name</code>
   ***********************************************************************************************/
  @Override
  public String getName() {
    return name;
  }

  /***********************************************************************************************
   * Gets the <code>packageName</code> of the <code>LookAndFeel</code>
   * @return the <code>packageName</code>
   ***********************************************************************************************/
  public String getPackage() {
    return packageName;
  }

  /***********************************************************************************************
   * Gets the value of a named <code>property</code>
   * @param property the property to get the value of
   * @return the value of a <code>property</code>
   ***********************************************************************************************/
  public Object getProperty(String property) {
    if (property.equals("BACKGROUND_COLOR")) {
      return backgroundColor;
    }
    else if (property.equals("DARK_COLOR")) {
      return darkColor;
    }
    else if (property.equals("MID_COLOR")) {
      return midColor;
    }
    else if (property.equals("LIGHT_COLOR")) {
      return lightColor;
    }
    else if (property.equals("FONT")) {
      return font;
    }
    else if (property.equals("FONT_SIZE")) {
      return fontSize;
    }
    else if (property.equals("TEXT_COLOR")) {
      return textColor;
    }
    else if (property.equals("TEXT_HEIGHT")) {
      return textHeight;
    }
    else if (property.equals("USE_ANTIALIAS")) {
      return useAntialias;
    }
    return null;
  }

  /***********************************************************************************************
   * Whether the <code>LookAndFeel</code> supports window decorations or not?
   * @return <b>true</b>, as it always supports window decorations
   ***********************************************************************************************/
  @Override
  public boolean getSupportsWindowDecorations() {
    return true;
  }

  /***********************************************************************************************
   * Gets the <code>textColor</code>
   * @return the <code>textColor</code>
   ***********************************************************************************************/
  public Color getTextColor() {
    return textColor;
  }

  /***********************************************************************************************
   * Gets the <code>textHeight</code>
   * @return the <code>textHeight</code>
   ***********************************************************************************************/
  public int getTextHeight() {
    return textHeight;
  }

  /***********************************************************************************************
   * Whether the <code>LookAndFeel</code> is a native look and feel
   * @return <b>false</b>, as it is not a native look and feel
   ***********************************************************************************************/
  @Override
  public boolean isNativeLookAndFeel() {
    return false;
  }

  /***********************************************************************************************
   * Whether the <code>LookAndFeel</code> is supported or not
   * @return <b>true</b>, as it is always supported
   ***********************************************************************************************/
  @Override
  public boolean isSupportedLookAndFeel() {
    return true;
  }

  /***********************************************************************************************
   * Whether to use anti-aliasing or not?
   * @return <b>true</b> to use anti-aliasing<br />
   *         <b>false</b> to not use any anti-aliasing
   ***********************************************************************************************/
  public boolean isUseAntialias() {
    return useAntialias;
  }

  /***********************************************************************************************
   * Loads the properties of this <code>LookAndFeel</code> from the <code>Settings</code>
   ***********************************************************************************************/
  public void loadProperties() {
    try {

      String fontName = Settings.get("WSFontChooser_FontName_Selected");
      String fontSize = Settings.get("WSFontChooser_FontSize_Selected");
      String fontStyle = Settings.get("WSFontChooser_FontStyle_Selected");

      setFont(Font.decode(fontName + "-" + fontStyle + "-" + fontSize));
      setFontSize(Integer.parseInt(fontSize));
      setTextHeight(this.fontSize * 2); // good as an approximate, calculated more properly by a subsequent call to calculateTextHeight()

      // Load the colors if specified manually
      setBackgroundColor(new Color(Settings.getInt("Theme_" + name + "_BackgroundColor")));
      setDarkColor(new Color(Settings.getInt("Theme_" + name + "_DarkColor")));
      setMidColor(new Color(Settings.getInt("Theme_" + name + "_MidColor")));
      setLightColor(new Color(Settings.getInt("Theme_" + name + "_LightColor")));
      setTextColor(new Color(Settings.getInt("Theme_" + name + "_TextColor")));
      setUseAntialias(Settings.getBoolean("Theme_" + name + "_UseAntiAlias"));

      // Generate the colors if not manually specified
      generateColors(new Color(Settings.getInt("Theme_" + name + "_InterfaceColors")));

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Sets the <code>backgroundColor</code>
   * @param backgroundColor the <code>backgroundColor</code>
   ***********************************************************************************************/
  public void setBackgroundColor(Color backgroundColor) {
    this.backgroundColor = backgroundColor;
  }

  /***********************************************************************************************
   * Sets the <code>darkColor</code>
   * @param darkColor the <code>darkColor</code>
   ***********************************************************************************************/
  public void setDarkColor(Color darkColor) {
    this.darkColor = darkColor;
  }

  /***********************************************************************************************
   * Sets the <code>font</code>
   * @param font the <code>font</code>
   ***********************************************************************************************/
  public void setFont(Font font) {
    this.font = font;
  }

  /***********************************************************************************************
   * Sets the <code>fontSize</code>
   * @param fontSize the <code>fontSize</code>
   ***********************************************************************************************/
  public void setFontSize(int fontSize) {
    this.fontSize = fontSize;
  }

  /***********************************************************************************************
   * Sets the <code>lightColor</code>
   * @param lightColor the <code>lightColor</code>
   ***********************************************************************************************/
  public void setLightColor(Color lightColor) {
    this.lightColor = lightColor;
  }

  /***********************************************************************************************
   * Sets the <code>midColor</code>
   * @param midColor the <code>midColor</code>
   ***********************************************************************************************/
  public void setMidColor(Color midColor) {
    this.midColor = midColor;
  }

  /***********************************************************************************************
   * Sets the <code>name</code> of the <code>LookAndFeel</code>
   * @param name the <code>name</code>
   ***********************************************************************************************/
  public void setName(String name) {
    this.name = name;
  }

  /***********************************************************************************************
   * Sets the <code>packageName</code> of the <code>LookAndFeel</code>
   * @param packageName the <code>packageName</code>
   ***********************************************************************************************/
  public void setPackage(String packageName) {
    this.packageName = packageName;
  }

  /***********************************************************************************************
   * Sets the <code>textColor</code>
   * @param textColor the <code>textColor</code>
   ***********************************************************************************************/
  public void setTextColor(Color textColor) {
    this.textColor = textColor;
  }

  /***********************************************************************************************
   * Sets the <code>textHeight</code>
   * @param textHeight the <code>textHeight</code>
   ***********************************************************************************************/
  public void setTextHeight(int textHeight) {
    this.textHeight = textHeight;
  }

  /***********************************************************************************************
   * Whether to use anti-aliasing or not?
   * @param useAntialias <b>true</b> to use anti-aliasing<br />
   *        <b>false</b> to not use any anti-aliasing
   ***********************************************************************************************/
  public void setUseAntialias(boolean useAntialias) {
    this.useAntialias = useAntialias;
  }

  /***********************************************************************************************
   * Gets the <code>name</code> of the <code>LookAndFeel</code>
   * @return the <code>name</code>
   ***********************************************************************************************/
  @Override
  public String toString() {
    return name;
  }
}