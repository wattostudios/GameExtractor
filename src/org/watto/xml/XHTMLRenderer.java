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

package org.watto.xml;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.ImageIcon;
import org.watto.ErrorLogger;

/***********************************************************************************************
 * Renders <code>XMLNode</code> representations of a XHTML document into an <code>Image</code>
 * @see org.watto.xml.XHTMLReader
 ***********************************************************************************************/
public class XHTMLRenderer {

  /***********************************************************************************************
   * Constructor
   ***********************************************************************************************/
  public XHTMLRenderer() {
  }

  /***********************************************************************************************
   * Determines whether the current component is centered, by looking for a &lt;center&gt; tag in
   * one of the parents of the <code>currentNode</code>
   * @param currentNode the node to verify for being centered
   * @return <b>true</b> if the <code>currentNode</code> is centered<br />
   *         <b>false</b> if it is not centered
   ***********************************************************************************************/
  public boolean isCentered(XMLNode currentNode) {
    try {
      XMLNode parent = (XMLNode) currentNode.getParent();
      while (parent != null) {
        String parentTag = parent.getTag();
        if (parentTag.equalsIgnoreCase("center")) {
          return true;
        }
        else {
          parent = (XMLNode) parent.getParent();
        }
      }

      return false;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return false;
    }
  }

  /***********************************************************************************************
   * Paints the <code>currentNode</code> on to the <code>graphics</code>, constrained by the
   * location.
   * @param currentNode the node to render
   * @param graphics the <code>Graphics</code> to paint on
   * @param x the x position to start painting
   * @param y the y position to start painting
   * @param width the maximum width that the <code>currentNode</code> can be painted on
   * @param height the maximum height that the <code>currentNode</code> can be painted on
   * @return the actual <code>Dimensions</code> used to render the <code>currentNode</code>
   ***********************************************************************************************/
  public Dimension paint(XMLNode currentNode, Graphics graphics, int x, int y, int width, int height) {
    String tag = currentNode.getTag();

    int textHeight = graphics.getFontMetrics().getHeight();

    // GETTING THE OLD VALUES (such as colors)
    Color oldColor = graphics.getColor();

    // APPLYING THE ATTRIBUTES FOR THIS TAG
    String newColor = currentNode.getAttribute("color");
    graphics.setColor(parseColor(newColor));

    int componentHeight = 0;
    int componentWidth = 0;

    // BEFORE RENDERING TASKS

    if (tag.equals("li")) {
      // paint bullet
      graphics.fillOval(x, y + (textHeight / 2) - 7, 6, 6);

      // adjust sizes/positions so the text doesn't overwrite the bullet
      width = width - 10;
      x = x + 10;
    }
    else if (tag.equals("span")) {
      Object borderWidthValue = currentNode.getAttribute("border-width");
      if (borderWidthValue != null) {
        int borderWidth = Integer.parseInt((String) borderWidthValue);

        // set up the children paint area
        width -= borderWidth * 4;
        height -= borderWidth * 4;
        x += borderWidth * 2;
        y += borderWidth * 2;

      }
    }
    else if (tag.equals("ul") || tag.equals("hr")) {
      componentHeight += textHeight;
    }

    // RENDERING SINGLE TAGS

    // single tags
    if (tag.equals("hr")) {
      int hrTop = y + componentHeight + 2;
      graphics.drawLine(x + 10, hrTop, width + x - 10, hrTop);
      graphics.drawLine(x + 10, hrTop + 2, width + x - 10, hrTop + 2);

      Color oldHRColor = graphics.getColor();
      graphics.setColor(oldHRColor.brighter());
      graphics.drawLine(x + 10, hrTop + 1, width + x - 10, hrTop + 1);
      graphics.setColor(oldHRColor);
    }
    else if (tag.equals("img")) {
      try {
        String src = currentNode.getAttribute("src");
        //ImageIcon icon = new ImageIcon(getClass().getResource(src));
        //ImageIcon icon = new ImageIcon(org.watto.component.WSHelper.getResource(src));
        ImageIcon icon = new ImageIcon(src);

        int iconWidth = icon.getIconWidth();
        int iconHeight = icon.getIconHeight();

        int imgTop = y + componentHeight + 2;
        int imgLeft = x + componentWidth + 2;

        if (isCentered(currentNode)) {
          imgLeft += (width / 2) - (iconWidth / 2);
        }

        graphics.drawImage(icon.getImage(), imgLeft, imgTop, iconWidth, iconHeight, null);

        componentHeight += iconHeight + 4;
        componentWidth += iconWidth + 4;
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }
    }

    // RENDERING THE TAG TEXT/CONTENT, IF ANY
    if (currentNode.hasContent()) {
      Dimension usedSpace = paintText(currentNode, graphics, x, y, width, height);
      if (usedSpace != null) {
        componentHeight += usedSpace.height;
        if (usedSpace.width > componentWidth) {
          componentWidth = usedSpace.width;
        }
      }
    }

    // RENDERING THE CHILDREN

    int numChildren = currentNode.getChildCount();

    for (int i = 0; i < numChildren; i++) {
      //Graphics t = g.create(0,componentHeight,width,height-componentHeight);

      Dimension childSize = paint(((XMLNode) currentNode.getChildAt(i)), graphics, x, y + componentHeight, width, height);
      componentHeight += (int) childSize.getHeight();

      int childWidth = (int) childSize.getWidth();
      if (childWidth > componentWidth) {
        componentWidth = childWidth;
      }
    }

    // AFTER RENDERING TASKS
    if (tag.equals("br") || tag.equals("li") || tag.equals("hr")) {
      componentHeight += textHeight;
    }
    else if (tag.equals("u")) {
      // paint underline
      int uTop = y + componentHeight + textHeight - 5;
      int uLeft = x + (width / 2) - componentWidth / 2 - 1;
      graphics.drawLine(uLeft, uTop, uLeft + componentWidth, uTop);
    }
    else if (tag.equals("span")) {
      Object borderWidthValue = currentNode.getAttribute("border-width");
      if (borderWidthValue != null) {
        int borderWidth = Integer.parseInt((String) borderWidthValue);

        // set up the border paint area
        width += borderWidth * 6;
        height = componentHeight + borderWidth * 4;
        x -= borderWidth * 4;
        y -= borderWidth * 4;

        // paint the new border
        for (int i = 0; i < borderWidth; i++) {
          graphics.drawRect(x + i, y + i, width - i - i - 1, height - i - i - 1);
        }

        componentHeight += borderWidth * 4;

      }
    }

    // SETTING BACK THE OLD VALUES
    graphics.setColor(oldColor);

    return new Dimension(componentWidth, componentHeight);
  }

  /***********************************************************************************************
   * Paints a <code>line</code> of text on to the <code>graphics</code>, constrained by the
   * location.
   * @param graphics the <code>Graphics</code> to paint on
   * @param x the x position to start painting
   * @param y the y position to start painting
   * @param line the text <code>String</code> to paint
   ***********************************************************************************************/
  public void paintLine(Graphics graphics, int x, int y, String line) {
    graphics.drawString(line, x, y);
  }

  /***********************************************************************************************
   * Paints the text of the <code>currentNode</code> on to the <code>graphics</code>, constrained
   * by the location.
   * @param currentNode the node to render
   * @param graphics the <code>Graphics</code> to paint on
   * @param x the x position to start painting
   * @param y the y position to start painting
   * @param width the maximum width that the <code>currentNode</code> can be painted on
   * @param height the maximum height that the <code>currentNode</code> can be painted on
   * @return the actual <code>Dimensions</code> used to render the <code>currentNode</code>
   ***********************************************************************************************/
  public Dimension paintText(XMLNode currentNode, Graphics graphics, int x, int y, int width, int height) {
    boolean centered = isCentered(currentNode);

    int componentHeight = 0;
    int componentWidth = 0;

    int textHeight = 0;
    int textWidth = 0;

    String text = currentNode.getContent();

    if (text.equals("")) {
      // keep it centered for the top and left, and don't change the width of height
    }
    else {
      // determine the size, the top position, and the left position of the text
      graphics.setFont(graphics.getFont().deriveFont(Font.BOLD));
      FontMetrics metrics = graphics.getFontMetrics();

      textHeight = metrics.getHeight();
      textWidth = metrics.stringWidth(text);

      String[] lines;

      if (textWidth > width) {
        lines = splitText(text, metrics, width);
      }
      else {
        lines = new String[] { text };
      }

      componentHeight = textHeight * (lines.length - 1);

      int top = y + textHeight / 2;
      for (int i = 0; i < lines.length; i++) {
        int lineWidth = metrics.stringWidth(lines[i]);

        int left = x;
        if (centered) {
          left += (width / 2) - (lineWidth / 2);
        }

        paintLine(graphics, left, top, lines[i]);

        top += textHeight;

        if (lineWidth > componentWidth) {
          componentWidth = lineWidth;
        }

      }

    }

    return new Dimension(componentWidth, componentHeight);
  }

  /***********************************************************************************************
   * Changes a <code>colorName</code> into an actual <code>Color</code> value
   * @param colorName the name of a color
   * @return the <code>Color</code> for the <code>colorName</code>
   ***********************************************************************************************/
  public Color parseColor(String colorName) {
    if (colorName == null) {
      return new Color(255, 255, 255);
    }
    else if (colorName.equals("red")) {
      return new Color(200, 50, 50);
    }
    else if (colorName.equals("blue")) {
      return new Color(50, 50, 200);
    }
    else if (colorName.equals("green")) {
      return new Color(50, 150, 50);
    }
    else if (colorName.equals("purple")) {
      return new Color(200, 50, 200);
    }
    else if (colorName.equals("orange")) {
      return new Color(200, 150, 50);
    }
    else if (colorName.equals("yellow")) {
      return new Color(200, 200, 50);
    }
    else {
      return new Color(255, 255, 255);
    }
  }

  /***********************************************************************************************
   * Splits the <code>text</code> into a number of lines. The <code>text</code> is split so that
   * it fits the words to a maximum of <code>width</code>, and to any height.
   * @param text the <code>String</code> to split into lines
   * @param metrics the <code>FontMetrics</code> for the current <code>Font</code>, to let us
   *        calculate the width of any character in the <code>text</code>
   * @param width the maximum width that a line of <code>text</code> can be
   * @return the <code>text</code>, split up in to lines of a maximum <code>width</code>
   ***********************************************************************************************/
  public String[] splitText(String text, FontMetrics metrics, int width) {
    String[] words = text.split(" ");
    String[] lines = new String[words.length];
    int numLines = 0;

    String line = words[0];
    for (int i = 1; i < words.length; i++) {
      if (metrics.stringWidth(line + " " + words[i]) < width) {
        line += " " + words[i];
      }
      else {
        lines[numLines] = line;
        numLines++;

        line = words[i];
      }
    }

    lines[numLines] = line;
    numLines++;

    String[] temp = lines;
    lines = new String[numLines];
    System.arraycopy(temp, 0, lines, 0, numLines);

    return lines;
  }
}