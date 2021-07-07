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
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicGraphicsUtils;

/***********************************************************************************************
 * Utilities for painting borders, backgrounds, and text.
 * @see org.watto.plaf.ButterflyLookAndFeel
 ***********************************************************************************************/
public class ButterflyPainter {

  /** Whether gradients should be painted up-down (false) or left-right (true) **/
  static boolean sidewaysGradient = false;

  /***********************************************************************************************
   * Enabled anti-aliasing on the <code>Graphics</code> if
   * <code>LookAndFeel.getUseAntialias()</code> is <i>true</i>
   * @param graphics the <code>Graphics</code> to set the anti-alias parameters on
   ***********************************************************************************************/
  public static void enableAntialias(Graphics graphics) {
    enableAntialias((Graphics2D) graphics);
  }

  /***********************************************************************************************
   * Enabled anti-aliasing on the <code>Graphics2D</code> if
   * <code>LookAndFeel.getUseAntialias()</code> is <i>true</i>
   * @param graphics the <code>Graphics2D</code> to set the anti-alias parameters on
   ***********************************************************************************************/
  public static void enableAntialias(Graphics2D graphics) {
    if (LookAndFeelManager.isUseAntialias()) {
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
  }

  /***********************************************************************************************
   * Paints a cross, as found in a checkbox
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintCross(Graphics2D graphics, int x, int y, int w, int h) {
    graphics.setColor(LookAndFeelManager.getTextColor());
    graphics.drawLine(x + 4, y + 4, w - 5, h - 5);
    graphics.drawLine(x + 4, h - 5, w - 5, y + 4);
  }

  /***********************************************************************************************
   * Paints a cross, as found in a checkbox
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintCross(Graphics2D graphics, int x, int y, int w, int h, Color color) {
    graphics.setColor(color);
    graphics.drawLine(x + 4, y + 4, w - 5, h - 5);
    graphics.drawLine(x + 4, h - 5, w - 5, y + 4);
  }

  /***********************************************************************************************
   * Paints a curved border
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintCurvedBorder(Graphics2D graphics, int x, int y, int w, int h) {
    paintCurvedBorder(graphics, x, y, w, h, LookAndFeelManager.getDarkColor(), LookAndFeelManager.getLightColor());
  }

  /***********************************************************************************************
   * Paints a curved border
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param border the border <code>Color</code>
   * @param gloss the gloss <code>Color</code>
   ***********************************************************************************************/
  public static void paintCurvedBorder(Graphics2D graphics, int x, int y, int w, int h, Color border, Color gloss) {
    int round = LookAndFeelManager.getPropertyInt("ROUNDNESS");
    paintCurvedBorder(graphics, x, y, w, h, border, gloss, round);
  }

  /***********************************************************************************************
   * Paints a curved border
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param border the border <code>Color</code>
   * @param gloss the gloss <code>Color</code>
   * @param round the roundness of the curve
   ***********************************************************************************************/
  public static void paintCurvedBorder(Graphics2D graphics, int x, int y, int w, int h, Color border, Color gloss, int round) {
    enableAntialias(graphics);

    // INNER BORDER
    //graphics.setColor(border);
    //graphics.setStroke(new BasicStroke(thickness,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
    //graphics.drawRoundRect(x + 3,y + 3,w - 7,h - 7,round,round);

    // GLOSS
    graphics.setColor(LookAndFeelManager.getLightColor());
    //graphics.setStroke(new BasicStroke(thickness,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
    graphics.drawRoundRect(x + 2, y + 2, w - 5, h - 5, round, round);

    // OUTER BORDER
    graphics.setColor(border);
    //graphics.setStroke(new BasicStroke(thickness,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
    graphics.drawRoundRect(x + 1, y + 1, w - 3, h - 3, round, round);
  }

  /***********************************************************************************************
   * Paints a curved border with a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintCurvedGradient(Graphics2D graphics, int x, int y, int w, int h) {
    paintCurvedGradient(graphics, x, y, w, h, LookAndFeelManager.getMidColor(), LookAndFeelManager.getLightColor());
  }

  /***********************************************************************************************
   * Paints a curved border with a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param startGradient the starting <code>Color</code> for the gradient background
   * @param finishGradient the finishing <code>Color</code> for the gradient background
   ***********************************************************************************************/
  public static void paintCurvedGradient(Graphics2D graphics, int x, int y, int w, int h, Color startGradient, Color finishGradient) {
    paintCurvedGradient(graphics, x, y, w, h, LookAndFeelManager.getDarkColor(), LookAndFeelManager.getLightColor(), startGradient, finishGradient);
  }

  /***********************************************************************************************
   * Paints a curved border with a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param border the border <code>Color</code>
   * @param gloss the gloss <code>Color</code>
   * @param startGradient the starting <code>Color</code> for the gradient background
   * @param finishGradient the finishing <code>Color</code> for the gradient background
   ***********************************************************************************************/
  public static void paintCurvedGradient(Graphics2D graphics, int x, int y, int w, int h, Color border, Color gloss, Color startGradient, Color finishGradient) {
    int round = LookAndFeelManager.getPropertyInt("ROUNDNESS");
    paintCurvedGradient(graphics, x, y, w, h, border, gloss, startGradient, finishGradient, round);
  }

  /***********************************************************************************************
   * Paints a curved border with a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param border the border <code>Color</code>
   * @param gloss the gloss <code>Color</code>
   * @param startGradient the starting <code>Color</code> for the gradient background
   * @param finishGradient the finishing <code>Color</code> for the gradient background
   * @param round the roundness of the curve
   ***********************************************************************************************/
  public static void paintCurvedGradient(Graphics2D graphics, int x, int y, int w, int h, Color border, Color gloss, Color startGradient, Color finishGradient, int round) {

    // BACKGROUND
    Shape clip = graphics.getClip();
    graphics.clip(new RoundRectangle2D.Float(x + 1, y + 1, w - 3, h - 3, round, round));
    paintGradientBackground(graphics, x, y, w, h, startGradient, finishGradient);
    graphics.setClip(clip);

    // BORDER
    paintCurvedBorder(graphics, x, y, w, h, border, gloss, round);
  }

  /***********************************************************************************************
   * Paints a curved border with a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param round the roundness of the curve
   ***********************************************************************************************/
  public static void paintCurvedGradient(Graphics2D graphics, int x, int y, int w, int h, int round) {
    paintCurvedGradient(graphics, x, y, w, h, LookAndFeelManager.getDarkColor(), LookAndFeelManager.getLightColor(), LookAndFeelManager.getMidColor(), LookAndFeelManager.getLightColor(), round);
  }

  /***********************************************************************************************
   * Paints a curved border with a solid background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintCurvedSolid(Graphics2D graphics, int x, int y, int w, int h) {
    paintCurvedSolid(graphics, x, y, w, h, LookAndFeelManager.getMidColor());
  }

  /***********************************************************************************************
   * Paints a curved border with a solid background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param background the background <code>Color</code>
   ***********************************************************************************************/
  public static void paintCurvedSolid(Graphics2D graphics, int x, int y, int w, int h, Color background) {
    paintCurvedSolid(graphics, x, y, w, h, LookAndFeelManager.getDarkColor(), LookAndFeelManager.getLightColor(), background);
  }

  /***********************************************************************************************
   * Paints a curved border with a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param border the border <code>Color</code>
   * @param gloss the gloss <code>Color</code>
   * @param background the background <code>Color</code>
   ***********************************************************************************************/
  public static void paintCurvedSolid(Graphics2D graphics, int x, int y, int w, int h, Color border, Color gloss, Color background) {
    int round = LookAndFeelManager.getPropertyInt("ROUNDNESS");

    // BACKGROUND
    Shape clip = graphics.getClip();
    graphics.clip(new RoundRectangle2D.Float(x + 1, y + 1, w - 3, h - 3, round, round));
    paintSolidBackground(graphics, x, y, w, h, background);
    graphics.setClip(clip);

    // BORDER
    paintCurvedBorder(graphics, x, y, w, h, border, gloss);
  }

  /***********************************************************************************************
   * Paints a cross, as found in a radio button
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintDot(Graphics2D graphics, int x, int y, int w, int h) {
    int round = LookAndFeelManager.getPropertyInt("ROUNDNESS") * 2;

    graphics.setColor(LookAndFeelManager.getTextColor());
    graphics.fillRoundRect(x + 4, y + 4, w - 8, h - 8, round, round);
  }

  /***********************************************************************************************
   * Paints a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintGradientBackground(Graphics2D graphics, int x, int y, int w, int h) {
    paintGradientBackground(graphics, x, y, w, h, LookAndFeelManager.getMidColor(), LookAndFeelManager.getLightColor());
  }

  /***********************************************************************************************
   * Paints a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param startGradient the starting <code>Color</code> for the gradient background
   * @param finishGradient the finishing <code>Color</code> for the gradient background
   ***********************************************************************************************/
  public static void paintGradientBackground(Graphics2D graphics, int x, int y, int w, int h, Color startGradient, Color finishGradient) {
    enableAntialias(graphics);

    if (sidewaysGradient) {
      graphics.setPaint(new GradientPaint(x, 0, startGradient, x + w, 0, finishGradient, false));
    }
    else {
      graphics.setPaint(new GradientPaint(0, y, startGradient, 0, h + y, finishGradient, false));
    }

    graphics.fillRect(x, y, w, h);
  }

  /***********************************************************************************************
   * If the <code>component</code> is opaque, it paints the <code>backgroundColor</code>
   * background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param component the <code>Component</code> to check for opacity
   ***********************************************************************************************/
  public static void paintOpaque(Graphics graphics, JComponent component) {
    if (component.isOpaque()) {
      graphics.setColor(LookAndFeelManager.getBackgroundColor());
      graphics.fillRect(0, 0, component.getWidth(), component.getHeight());
    }
  }

  /***********************************************************************************************
   * Paints a <code>String</code> with a shadow
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param text the text <code>String</code> to paint
   * @param x the x position to paint from
   * @param y the y position to paint from
   ***********************************************************************************************/
  public static void paintShadowText(Graphics2D graphics, String text, int x, int y) {
    //paintShadowText(graphics, text, x, y, LookAndFeelManager.getBackgroundColor(), LookAndFeelManager.getTextColor());
    paintShadowText(graphics, text, x, y, LookAndFeelManager.getTextColor(), LookAndFeelManager.getLightColor());
  }

  /***********************************************************************************************
   * Paints a <code>String</code> with a shadow
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param text the text <code>String</code> to paint
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param textColor the <code>text</code> <code>Color</code>
   * @param shadowColor the <code>Color</code> of the shadow
   ***********************************************************************************************/
  public static void paintShadowText(Graphics2D graphics, String text, int x, int y, Color textColor, Color shadowColor) {
    paintText(graphics, text, x + 1, y + 1, shadowColor);
    paintText(graphics, text, x, y, textColor);
  }

  /***********************************************************************************************
   * Paints a solid background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param background the background <code>Color</code>
   ***********************************************************************************************/
  public static void paintSolidBackground(Graphics2D graphics, int x, int y, int w, int h, Color background) {
    enableAntialias(graphics);

    graphics.setColor(background);
    graphics.fillRect(x, y, w, h);
  }

  /***********************************************************************************************
   * Paints a square border
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintSquareBorder(Graphics2D graphics, int x, int y, int w, int h) {
    paintSquareBorder(graphics, x, y, w, h, LookAndFeelManager.getDarkColor(), LookAndFeelManager.getLightColor());
  }

  /***********************************************************************************************
   * Paints a square border
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param border the border <code>Color</code>
   * @param gloss the gloss <code>Color</code>
   ***********************************************************************************************/
  public static void paintSquareBorder(Graphics2D graphics, int x, int y, int w, int h, Color border, Color gloss) {
    enableAntialias(graphics);

    // INNER BORDER
    //graphics.setColor(border);
    ////graphics.setStroke(new BasicStroke(thickness,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
    //graphics.drawRect(x + 3, y + 3, w - 7, h - 7);

    // GLOSS
    graphics.setColor(LookAndFeelManager.getLightColor());
    //graphics.setStroke(new BasicStroke(thickness,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
    graphics.drawRect(x + 2, y + 2, w - 5, h - 5);

    // OUTER BORDER
    graphics.setColor(border);
    //graphics.setStroke(new BasicStroke(thickness,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
    graphics.drawRect(x + 1, y + 1, w - 3, h - 3);
  }

  /***********************************************************************************************
   * Paints a square border with a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintSquareGradient(Graphics2D graphics, int x, int y, int w, int h) {
    paintSquareGradient(graphics, x, y, w, h, LookAndFeelManager.getMidColor(), LookAndFeelManager.getLightColor());
  }

  /***********************************************************************************************
   * Paints a square border with a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param startGradient the starting <code>Color</code> for the gradient background
   * @param finishGradient the finishing <code>Color</code> for the gradient background
   ***********************************************************************************************/
  public static void paintSquareGradient(Graphics2D graphics, int x, int y, int w, int h, Color startGradient, Color finishGradient) {
    paintSquareGradient(graphics, x, y, w, h, LookAndFeelManager.getDarkColor(), LookAndFeelManager.getLightColor(), startGradient, finishGradient);
  }

  /***********************************************************************************************
   * Paints a square border with a gradient background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param border the border <code>Color</code>
   * @param gloss the gloss <code>Color</code>
   * @param startGradient the starting <code>Color</code> for the gradient background
   * @param finishGradient the finishing <code>Color</code> for the gradient background
   ***********************************************************************************************/
  public static void paintSquareGradient(Graphics2D graphics, int x, int y, int w, int h, Color border, Color gloss, Color startGradient, Color finishGradient) {
    // BACKGROUND
    Shape clip = graphics.getClip();
    graphics.clip(new Rectangle2D.Float(x + 1, y + 1, w - 3, h - 3));
    paintGradientBackground(graphics, x, y, w, h, startGradient, finishGradient);
    graphics.setClip(clip);

    // BORDER
    paintSquareBorder(graphics, x, y, w, h, border, gloss);
  }

  /***********************************************************************************************
   * Paints a square border with a solid background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   ***********************************************************************************************/
  public static void paintSquareSolid(Graphics2D graphics, int x, int y, int w, int h) {
    paintSquareSolid(graphics, x, y, w, h, LookAndFeelManager.getMidColor());
  }

  /***********************************************************************************************
   * Paints a square border with a solid background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param background the background <code>Color</code>
   ***********************************************************************************************/
  public static void paintSquareSolid(Graphics2D graphics, int x, int y, int w, int h, Color background) {
    paintSquareSolid(graphics, x, y, w, h, LookAndFeelManager.getDarkColor(), LookAndFeelManager.getLightColor(), background);
  }

  /***********************************************************************************************
   * Paints a square border with a solid background
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param w the width of the painting window
   * @param h the height of the painting window
   * @param border the border <code>Color</code>
   * @param gloss the gloss <code>Color</code>
   * @param background the background <code>Color</code>
   ***********************************************************************************************/
  public static void paintSquareSolid(Graphics2D graphics, int x, int y, int w, int h, Color border, Color gloss, Color background) {
    // BACKGROUND
    Shape clip = graphics.getClip();
    graphics.clip(new Rectangle2D.Float(x + 1, y + 1, w - 3, h - 3));
    paintSolidBackground(graphics, x, y, w, h, background);
    graphics.setClip(clip);

    // BORDER
    paintSquareBorder(graphics, x, y, w, h);
  }

  /***********************************************************************************************
   * Paints a <code>String</code>
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param text the text <code>String</code> to paint
   * @param x the x position to paint from
   * @param y the y position to paint from
   ***********************************************************************************************/
  public static void paintText(Graphics2D graphics, String text, int x, int y) {
    paintText(graphics, text, x, y, LookAndFeelManager.getTextColor());
  }

  /***********************************************************************************************
   * Paints a <code>String</code>
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param text the text <code>String</code> to paint
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param textColor the <code>text</code> <code>Color</code>
   ***********************************************************************************************/
  public static void paintText(Graphics2D graphics, String text, int x, int y, Color textColor) {
    enableAntialias(graphics);

    graphics.setColor(textColor);
    graphics.drawString(text, x, y);
  }

  /***********************************************************************************************
   * Paints a <code>String</code> with a shadow, underlining a character in the <code>text</code>
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param text the text <code>String</code> to paint
   * @param underline the character to underline
   * @param x the x position to paint from
   * @param y the y position to paint from
   ***********************************************************************************************/
  public static void paintUnderlineShadowText(Graphics2D graphics, String text, int underline, int x, int y) {
    paintUnderlineShadowText(graphics, text, underline, x, y, LookAndFeelManager.getBackgroundColor(), LookAndFeelManager.getTextColor());
  }

  /***********************************************************************************************
   * Paints a <code>String</code> with a shadow, underlining a character in the <code>text</code>
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param text the text <code>String</code> to paint
   * @param underline the character to underline
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param textColor the <code>text</code> <code>Color</code>
   * @param shadowColor the <code>Color</code> of the shadow
   ***********************************************************************************************/
  public static void paintUnderlineShadowText(Graphics2D graphics, String text, int underline, int x, int y, Color textColor, Color shadowColor) {
    paintUnderlineText(graphics, text, underline, x + 1, y + 1, shadowColor);
    paintUnderlineText(graphics, text, underline, x, y, textColor);
  }

  /***********************************************************************************************
   * Paints a <code>String</code>, underlining a character in the <code>text</code>
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param text the text <code>String</code> to paint
   * @param underline the character to underline
   * @param x the x position to paint from
   * @param y the y position to paint from
   ***********************************************************************************************/
  public static void paintUnderlineText(Graphics2D graphics, String text, int underline, int x, int y) {
    paintUnderlineText(graphics, text, underline, x, y, LookAndFeelManager.getTextColor());
  }

  /***********************************************************************************************
   * Paints a <code>String</code>, underlining a character in the <code>text</code>
   * @param graphics the <code>Graphics2D</code> to paint on
   * @param text the text <code>String</code> to paint
   * @param underline the character to underline
   * @param x the x position to paint from
   * @param y the y position to paint from
   * @param textColor the <code>text</code> <code>Color</code>
   ***********************************************************************************************/
  public static void paintUnderlineText(Graphics2D graphics, String text, int underline, int x, int y, Color textColor) {
    enableAntialias(graphics);

    graphics.setColor(textColor);
    BasicGraphicsUtils.drawStringUnderlineCharAt(graphics, text, underline, x, y);
  }

  /***********************************************************************************************
   * Sets whether gradients should be painted up-down or left-right
   * @param sideways <b>true</b> to paint gradients left-right<br />
   *        <b>false</b> to paint gradients up-down
   ***********************************************************************************************/
  public static void setSidewaysGradient(boolean sideways) {
    sidewaysGradient = sideways;
  }
}