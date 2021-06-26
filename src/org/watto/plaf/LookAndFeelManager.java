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
import java.awt.Graphics;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import org.watto.ErrorLogger;

/***********************************************************************************************
 * Manages the GUI <code>LookAndFeel</code>
 ***********************************************************************************************/
public class LookAndFeelManager {

  /** The current <code>LookAndFeel</code> **/
  private static LookAndFeel lookAndFeel;

  /** The installed <code>LookAndFeel</code>s **/
  private static LookAndFeel[] installedLookAndFeels = new LookAndFeel[0];;

  /***********************************************************************************************
   * Calculates the proper text height using the given Graphics
   ***********************************************************************************************/
  public static void calculateTextHeight(Graphics g) {
    lookAndFeel.calculateTextHeight(g);
  }

  /***********************************************************************************************
   * Gets the <code>backgroundColor</code>
   * @return the <code>backgroundColor</code>
   ***********************************************************************************************/
  public static Color getBackgroundColor() {
    return lookAndFeel.getBackgroundColor();
  }

  /***********************************************************************************************
   * Gets the <code>darkColor</code>
   * @return the <code>darkColor</code>
   ***********************************************************************************************/
  public static Color getDarkColor() {
    return lookAndFeel.getDarkColor();
  }

  /***********************************************************************************************
   * Gets the <code>font</code>
   * @return the <code>font</code>
   ***********************************************************************************************/
  public static Font getFont() {
    return lookAndFeel.getFont();
  }

  /***********************************************************************************************
   * Gets the <code>fontSize</code>
   * @return the <code>fontSize</code>
   ***********************************************************************************************/
  public static int getFontSize() {
    return lookAndFeel.getFontSize();
  }

  /***********************************************************************************************
   * Loads an <code>ImageIcon</code> from the filesystem
   * @return the <code>ImageIcon</code> for the <i>imagePath</i>
   ***********************************************************************************************/
  public static ImageIcon getImageIcon(String imagePath) {
    return new ImageIcon(imagePath);
  }

  /***********************************************************************************************
   * Gets the <code>lightColor</code>
   * @return the <code>lightColor</code>
   ***********************************************************************************************/
  public static Color getLightColor() {
    return lookAndFeel.getLightColor();
  }

  /***********************************************************************************************
   * Gets the current <code>LookAndFeel</code>
   * @return the <code>LookAndFeel</code>
   ***********************************************************************************************/
  public static LookAndFeel getLookAndFeel() {
    return lookAndFeel;
  }

  /***********************************************************************************************
   * Gets the <code>LookAndFeel</code> with the given <code>name</code>
   * @param theme the name of the <code>LookAndFeel</code> to get
   * @return the <code>LookAndFeel</code>
   ***********************************************************************************************/
  public static LookAndFeel getLookAndFeel(String theme) {
    int lookAndFeelCount = installedLookAndFeels.length;
    for (int i = 0; i < lookAndFeelCount; i++) {
      if (installedLookAndFeels[i].getName().equals(theme)) {
        return installedLookAndFeels[i];
      }
    }
    return null;
  }

  /***********************************************************************************************
   * Gets the <code>midColor</code>
   * @return the <code>midColor</code>
   ***********************************************************************************************/
  public static Color getMidColor() {
    return lookAndFeel.getMidColor();
  }

  /***********************************************************************************************
   * Gets the <code>name</code> of the <code>LookAndFeel</code>
   * @return the <code>name</code>
   ***********************************************************************************************/
  public static String getName() {
    return lookAndFeel.getName();
  }

  /***********************************************************************************************
   * Gets the <code>packageName</code> of the <code>LookAndFeel</code>
   * @return the <code>packageName</code>
   ***********************************************************************************************/
  public static String getPackage() {
    return lookAndFeel.getPackage();
  }

  /***********************************************************************************************
   * Gets the value of a named <code>property</code>
   * @param property the property to get the value of
   * @return the value of a <code>property</code>
   ***********************************************************************************************/
  public static Object getProperty(String property) {
    return lookAndFeel.getProperty(property);
  }

  /***********************************************************************************************
   * Gets the value of a named <code>property</code> as a <code>boolean</code>
   * @param property the property to get the value of
   * @return the value of a <code>property</code>, as a <code>boolean</code>
   ***********************************************************************************************/
  public static boolean getPropertyBoolean(String property) {
    try {
      Object propertyObject = lookAndFeel.getProperty(property);
      if (propertyObject instanceof Boolean) {
        return ((Boolean) propertyObject);
      }
      else if (propertyObject.toString().equalsIgnoreCase("false")) {
        return false;
      }
      else {
        return true;
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return true;
    }
  }

  /***********************************************************************************************
   * Gets the value of a named <code>property</code> as a <code>Color</code>
   * @param property the property to get the value of
   * @return the value of a <code>property</code>, as a <code>Color</code>
   ***********************************************************************************************/
  public static Color getPropertyColor(String property) {
    try {
      return (Color) lookAndFeel.getProperty(property);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return Color.BLACK;
    }
  }

  /***********************************************************************************************
   * Gets the value of a named <code>property</code> as an <code>int</code>
   * @param property the property to get the value of
   * @return the value of a <code>property</code>, as an <code>int</code>
   ***********************************************************************************************/
  public static int getPropertyInt(String property) {
    try {
      Object propertyObject = lookAndFeel.getProperty(property);
      if (propertyObject instanceof Integer) {
        return ((Integer) propertyObject);
      }
      else {
        return Integer.parseInt(propertyObject.toString());
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return 0;
    }
  }

  /***********************************************************************************************
   * Gets the <code>textColor</code>
   * @return the <code>textColor</code>
   ***********************************************************************************************/
  public static Color getTextColor() {
    return lookAndFeel.getTextColor();
  }

  /***********************************************************************************************
   * Gets the <code>textHeight</code>
   * @return the <code>textHeight</code>
   ***********************************************************************************************/
  public static int getTextHeight() {
    return lookAndFeel.getTextHeight();
  }

  /***********************************************************************************************
   * Registers the given <code>LookAndFeel</code> with the <code>UIManager</code>
   * @param theme the <code>LookAndFeel</code> to install
   ***********************************************************************************************/
  public static void installLookAndFeel(LookAndFeel theme) {
    UIManager.installLookAndFeel(theme.getName(), theme.getPackage() + "." + theme.getName());

    int lookAndFeelCount = installedLookAndFeels.length;
    LookAndFeel[] oldLookAndFeels = installedLookAndFeels;
    installedLookAndFeels = new LookAndFeel[lookAndFeelCount + 1];
    System.arraycopy(oldLookAndFeels, 0, installedLookAndFeels, 0, lookAndFeelCount);
    installedLookAndFeels[lookAndFeelCount] = theme;
  }

  /***********************************************************************************************
   * Whether to use anti-aliasing or not?
   * @return <b>true</b> to use anti-aliasing<br />
   *         <b>false</b> to not use any anti-aliasing
   ***********************************************************************************************/
  public static boolean isUseAntialias() {
    return lookAndFeel.isUseAntialias();
  }

  /***********************************************************************************************
   * Loads the properties of this <code>LookAndFeel</code> from the <code>Settings</code>
   ***********************************************************************************************/
  public static void loadProperties() {
    lookAndFeel.loadProperties();
  }

  /***********************************************************************************************
   * Sets the <code>backgroundColor</code>
   * @param backgroundColor the <code>backgroundColor</code>
   ***********************************************************************************************/
  public static void setBackgroundColor(Color backgroundColor) {
    lookAndFeel.setBackgroundColor(backgroundColor);
  }

  /***********************************************************************************************
   * Sets the <code>darkColor</code>
   * @param darkColor the <code>darkColor</code>
   ***********************************************************************************************/
  public static void setDarkColor(Color darkColor) {
    lookAndFeel.setDarkColor(darkColor);
  }

  /***********************************************************************************************
   * Sets the <code>font</code>
   * @param font the <code>font</code>
   ***********************************************************************************************/
  public static void setFont(Font font) {
    lookAndFeel.setFont(font);
  }

  /***********************************************************************************************
   * Sets the <code>fontSize</code>
   * @param fontSize the <code>fontSize</code>
   ***********************************************************************************************/
  public static void setFontSize(int fontSize) {
    lookAndFeel.setFontSize(fontSize);
  }

  /***********************************************************************************************
   * Sets the <code>lightColor</code>
   * @param lightColor the <code>lightColor</code>
   ***********************************************************************************************/
  public static void setLightColor(Color lightColor) {
    lookAndFeel.setLightColor(lightColor);
  }

  /***********************************************************************************************
   * Sets the current <code>LookAndFeel</code> used to render the GUI
   * @param theme the <code>LookAndFeel</code> to use
   ***********************************************************************************************/
  public static void setLookAndFeel(LookAndFeel theme) {
    try {
      lookAndFeel = theme;
      UIManager.setLookAndFeel(lookAndFeel.getPackage() + "." + lookAndFeel.getName());
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Sets the current <code>LookAndFeel</code> used to render the GUI
   * @param theme the <code>LookAndFeel</code> to use
   ***********************************************************************************************/
  public static void setLookAndFeel(String theme) {
    try {
      int lookAndFeelCount = installedLookAndFeels.length;
      for (int i = 0; i < lookAndFeelCount; i++) {
        if (installedLookAndFeels[i].getName().equals(theme)) {
          lookAndFeel = installedLookAndFeels[i];
          UIManager.setLookAndFeel(lookAndFeel.getPackage() + "." + lookAndFeel.getName());
          return;
        }
      }

      // not a LookAndFeel, so is a native look and feel like Metal
      LookAndFeelInfo[] lookAndFeels = UIManager.getInstalledLookAndFeels();
      lookAndFeelCount = lookAndFeels.length;
      for (int i = 0; i < lookAndFeelCount; i++) {
        if (lookAndFeels[i].getName().equals(theme)) {
          UIManager.setLookAndFeel(lookAndFeels[i].getClassName());
          return;
        }
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Sets the <code>midColor</code>
   * @param midColor the <code>midColor</code>
   ***********************************************************************************************/
  public static void setMidColor(Color midColor) {
    lookAndFeel.setMidColor(midColor);
  }

  /***********************************************************************************************
   * Sets the <code>name</code> of the <code>LookAndFeel</code>
   * @param name the <code>name</code>
   ***********************************************************************************************/
  public static void setName(String name) {
    lookAndFeel.setName(name);
  }

  /***********************************************************************************************
   * Sets the <code>packageName</code> of the <code>LookAndFeel</code>
   * @param packageName the <code>packageName</code>
   ***********************************************************************************************/
  public static void setPackage(String packageName) {
    lookAndFeel.setPackage(packageName);
  }

  /***********************************************************************************************
   * Sets the <code>textColor</code>
   * @param textColor the <code>textColor</code>
   ***********************************************************************************************/
  public static void setTextColor(Color textColor) {
    lookAndFeel.setTextColor(textColor);
  }

  /***********************************************************************************************
   * Sets the <code>textHeight</code>
   * @param textHeight the <code>textHeight</code>
   ***********************************************************************************************/
  public static void setTextHeight(int textHeight) {
    lookAndFeel.setTextHeight(textHeight);
  }

  /***********************************************************************************************
   * Whether to use anti-aliasing or not?
   * @param useAntialias <b>true</b> to use anti-aliasing<br />
   *        <b>false</b> to not use any anti-aliasing
   ***********************************************************************************************/
  public static void setUseAntialias(boolean useAntialias) {
    lookAndFeel.setUseAntialias(useAntialias);
  }

  /***********************************************************************************************
   * Constructor
   ***********************************************************************************************/
  public LookAndFeelManager() {

  }

}