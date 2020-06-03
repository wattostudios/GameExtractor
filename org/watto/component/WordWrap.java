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

import java.awt.FontMetrics;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.watto.array.ArrayResizer;
import org.watto.io.FileManipulator;


/***********************************************************************************************
Splits <code>String</code>s up into lines that fit a certain width.
***********************************************************************************************/
public class WordWrap {

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public WordWrap(){}


  /***********************************************************************************************
  Looks for special characters in the <code>text</code> (such as <b>\n</b> and <b>\t</b>) and
  converts them into the actual byte representation of the character
  @param text the <code>String</code> to convert
  @return the converted <code>String</code>
  ***********************************************************************************************/
  public static String convertSpecialCharacters(String text){
    String outText = "";
    FileManipulator manipulator = new FileManipulator(new org.watto.io.buffer.StringBuffer(text));
    while (manipulator.getOffset() < manipulator.getLength()) {
      char letter = (char)manipulator.readByte();

      if (letter == '\\' && manipulator.getOffset() < manipulator.getLength()) {
        char nextLetter = (char)manipulator.readByte();
        if (nextLetter == 'n') {
          // found the newline character
          outText += '\n';
        }
        else if (nextLetter == 'r') {
          // found the carriage return character
          outText += '\r';
        }
        else if (nextLetter == 't') {
          // found the tab character
          outText += '\t';
        }
      }
      else {
        outText += letter;
      }
    }
    manipulator.close();
    return outText;
  }


  /***********************************************************************************************
  Splits the <code>text</code> so that each line fits in the <code>width</code>
  @param text the <code>String</code> to wrap
  @param width the maximum width of each line
  @return the <code>text</code> split up into separate lines
  ***********************************************************************************************/
  public static String[] wrap(String text,int width){
    return wrap(text,new JLabel(),width);
  }


  /***********************************************************************************************
  Splits the <code>text</code> so that each line fits in the width of the <code>component</code>
  @param text the <code>String</code> to wrap
  @param component the <code>JComponent</code> that will contain the <code>text</code>
  @return the <code>text</code> split up into separate lines
  ***********************************************************************************************/
  public static String[] wrap(String text,JComponent component){
    int width = component.getWidth();
    Insets insets = component.getInsets();
    width -= (insets.left + insets.right);
    return wrap(text,component,width);
  }


  /***********************************************************************************************
  Splits the <code>text</code> so that each line fits in the <code>width</code> of the
  <code>component</code>
  @param text the <code>String</code> to wrap
  @param component the <code>JComponent</code> that will contain the <code>text</code>
  @param width the maximum width of each line
  @return the <code>text</code> split up into separate lines
  ***********************************************************************************************/
  public static String[] wrap(String text,JComponent component,int width){
    try {

      // Allows the last word to be added to the last line.
      // Without this, we would need to add in a lot of repeating code
      // at the end to add in this extra word, whereas adding this space
      // will do it all automatically.
      text = convertSpecialCharacters(text);
      text += " ";

      // get the metrics for calculating the string widths
      FontMetrics metric;
      try {
        metric = component.getGraphics().getFontMetrics();
      }
      catch (Throwable t2) {
        return new String[]{text};
      }

      // set up a buffer for reading over the text
      FileManipulator manipulator = new FileManipulator(new org.watto.io.buffer.StringBuffer(text));

      String[] lines = new String[0];

      // read over the string
      String line = "";
      String word = "";
      while (manipulator.getOffset() < manipulator.getLength()) {
        char letter = (char)manipulator.readByte();

        if (letter == '\n' || letter == '\r') {
          // a new line

          // see if there is a word already
          // if so, need to analyse it, to see whether it will fit on the previous line or not
          if (!word.equals("")) {

            // see if the word can be added to the line, and still be under the maxLength
            if (line.equals("") && metric.stringWidth(word) <= width) {
              // the first word in the line
              line = word;
              word = "";
            }
            else if (metric.stringWidth(line + " " + word) <= width) {
              // the line already has words in it
              // add the word, and continue
              line += " " + word;
              word = "";
            }
            else {
              // adding the word will make the line too long.

              // if there is no current line (ie this word will be the only word on this line),
              // then we want to just use the full word for this line
              if (line.equals("")) {
                line = word;
                word = "";
              }

              // else, add the current line, and the word becomes the start of the next line
              int insertPos = lines.length;
              lines = ArrayResizer.resize(lines,insertPos + 1);
              lines[insertPos] = line;

              line = word;
              word = "";
            }
          }

          // add the new line
          int insertPos = lines.length;
          lines = ArrayResizer.resize(lines,insertPos + 1);
          lines[insertPos] = line;

          line = "";
          word = "";

          // check for \r\n
          if (letter == '\r' && manipulator.getOffset() < manipulator.getLength()) {
            letter = (char)manipulator.readByte();
            if (letter != '\n') {
              // wasn't a \n, so was an actual character - add it to the next word
              word += letter;
            }
          }
        }
        else if (letter == ' ') {
          // a separator between words

          // see if the word can be added to the line, and still be under the maxLength
          if (line.equals("") && metric.stringWidth(word) <= width) {
            // the first word in the line
            line = word;
            word = "";
          }
          else if (metric.stringWidth(line + " " + word) <= width) {
            // the line already has words in it
            // add the word, and continue
            line += " " + word;
            word = "";
          }
          else {
            // adding the word will make the line too long.

            // if there is no current line (ie this word will be the only word on this line),
            // then we want to just use the full word for this line
            if (line.equals("")) {
              line = word;
              word = "";
            }

            // else, add the current line, and the word becomes the start of the next line
            int insertPos = lines.length;
            lines = ArrayResizer.resize(lines,insertPos + 1);
            lines[insertPos] = line;

            line = word;
            word = "";
          }
        }
        else {
          // a normal letter

          // add the letter to the current word
          word += letter;
        }
      }

      if (!line.equals("")) {
        // add any leftover text
        int insertPos = lines.length;
        lines = ArrayResizer.resize(lines,insertPos + 1);
        lines[insertPos] = line;
      }

      return lines;
    }
    catch (Throwable t) {
      return new String[]{text};
    }
  }

}