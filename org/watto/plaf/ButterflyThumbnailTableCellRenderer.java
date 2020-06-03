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

import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Image;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import org.watto.component.WSLabel;
import org.watto.component.WSPanel;
import org.watto.datatype.BlankImageResource;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Resource;
import org.watto.xml.XMLReader;

/***********************************************************************************************
Used to paint cells for <code>WSTable</code>s
***********************************************************************************************/

public class ButterflyThumbnailTableCellRenderer extends DefaultTableCellRenderer {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /***********************************************************************************************
  Constructor
  ***********************************************************************************************/
  public ButterflyThumbnailTableCellRenderer() {
  }

  /***********************************************************************************************
  Gets the renderer for the <code>table</code> cell <code>value</code>
  @param table the <code>JTable</code> being painted
  @param value the value of the cell being painted
  @param isSelected whether the cell is selected or not
  @param hasFocus whether the cell has focus or not
  @param row the row in the <code>table</code> where this cell appears
  @param column the column in the <code>table</code> where this cell appears
  @return the renderer for this cell
  ***********************************************************************************************/
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    //JComponent rend = (JComponent) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

    WSLabel rend = new WSLabel();

    //WSPanel innerPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"CenteredLayout\" paintBackground=\"false\" opaque=\"true\" />"));
    //innerPanel.add(rend);

    WSPanel outerPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"CenteredLayout\" paintBackground=\"false\" />"));
    outerPanel.add(rend);

    if (value != null) {
      outerPanel.setShowBorder(true);
      if (isSelected) {
        outerPanel.setPaintBackground(true); // selected items have full background painted, not just the border
      }
    }

    /*
    rend.setForeground(LookAndFeelManager.getTextColor());
    
    // determine whether this is the last column or row of the table.
    // this is so we don't paint double-thickness borders in the grid.
    boolean firstRow = (row == 0);
    boolean firstColumn = (column == 0);
    
    Color borderColor;
    Color backgroundColor;
    
    if (isSelected) {
      borderColor = LookAndFeelManager.getDarkColor();
    
      backgroundColor = LookAndFeelManager.getLightColor();
      if (row % 2 == 0) {
        // leave color as is
      }
      else {
        // darker row
        int r = backgroundColor.getRed() - 25;
        if (r < 0) {
          r = 0;
        }
        int g = backgroundColor.getGreen() - 25;
        if (g < 0) {
          g = 0;
        }
        int b = backgroundColor.getBlue() - 25;
        if (b < 0) {
          b = 0;
        }
        backgroundColor = new Color(r, g, b);
      }
    
      if (table.getSelectionModel().getMinSelectionIndex() == row) {
        firstRow = true;
      }
    }
    else {
      borderColor = LookAndFeelManager.getLightColor();
      if (row % 2 == 0) {
        // leave color as is
        backgroundColor = Color.WHITE;
      }
      else {
        // darker row
        backgroundColor = new Color(230, 230, 230);
      }
    }
    
    rend.setBackground(backgroundColor);
    
    Border coloredBorder;
    Border emptyBorder;
    
    if (firstColumn && firstRow) {
      coloredBorder = new MatteBorder(1, 1, 1, 1, borderColor);
      emptyBorder = null;
    }
    else if (firstColumn) {
      coloredBorder = new MatteBorder(0, 1, 1, 1, borderColor);
      emptyBorder = new EmptyBorder(1, 0, 0, 0);
    }
    else if (firstRow) {
      coloredBorder = new MatteBorder(1, 0, 1, 1, borderColor);
      emptyBorder = new EmptyBorder(0, 1, 0, 0);
    }
    else {
      coloredBorder = new MatteBorder(0, 0, 1, 1, borderColor);
      emptyBorder = new EmptyBorder(1, 1, 0, 0);
    }
    
    rend.setBorder(new CompoundBorder(coloredBorder, emptyBorder));
    */

    if (rend instanceof JLabel) {
      JLabel label = rend;
      if (value instanceof Resource) {

        ImageResource imageResource = ((Resource) value).getImageResource();
        if (imageResource instanceof BlankImageResource) {
          // want to show the filename under the thumbnail icon

          /*
          WSPanel innerPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"BorderLayout\" />"));
          
          //WSLabel textLabel = new WSLabel();
          //textLabel.setText_Super(((Resource) value).getFilename());
          JLabel textLabel = new JLabel(((Resource) value).getFilename());
          
          innerPanel.add(rend, BorderLayout.CENTER);
          innerPanel.add(textLabel, BorderLayout.SOUTH);
          
          outerPanel.removeAll();
          outerPanel.add(innerPanel);
          */
          String originalFilename = ((Resource) value).getFilenameWithExtension();
          String filename = originalFilename;

          // Shrink the text if it's too wide
          int width = table.getColumnModel().getColumn(column).getWidth() - 20;
          FontMetrics metrics = label.getFontMetrics(LookAndFeelManager.getFont());
          int textWidth = metrics.stringWidth(filename);
          while (textWidth > width) {
            filename = filename.substring(0, filename.length() - 1);
            textWidth = metrics.stringWidth(filename);
          }

          if (filename.length() < originalFilename.length()) {
            // filename is longer than a single line, make it wrap to a second line
            String filename2 = originalFilename.substring(filename.length());

            textWidth = metrics.stringWidth(filename2);
            while (textWidth > width) {
              filename2 = filename2.substring(0, filename2.length() - 1);
              textWidth = metrics.stringWidth(filename2);
            }

            // append the second line to the filename that will be rendered
            filename += "<br>" + filename2;
          }
          else {
            filename += "<br>&nbsp;"; // to force it to be rendered in the same position as a 2-line output
          }

          filename = "<html>" + filename + "</html>";  // to force it to read/paint the <br> as a second line

          rend.setText_Super(filename);
          //rend.setShortenLongText(true);
          rend.setHorizontalTextPosition(JLabel.CENTER);
          rend.setVerticalTextPosition(JLabel.BOTTOM);
        }
        label.setIcon(new ImageIcon(imageResource.getThumbnail()));
        //label.setText("");
        //label.setBorder(new EmptyBorder(0, 0, 0, 0));
      }
      else if (value instanceof Icon) {
        label.setIcon((Icon) value);
        //label.setText("");
        //label.setBorder(new EmptyBorder(0, 0, 0, 0));
      }
      else if (value instanceof Image) {
        label.setIcon(new ImageIcon((Image) value));
        //label.setText("");
        //label.setBorder(new EmptyBorder(0, 0, 0, 0));
      }
      else {
        label.setIcon(null);
      }
    }

    //rend.setSize(new Dimension((int)rend.getMinimumSize().getWidth(),table.getRowHeight()));

    //return rend;
    return outerPanel;
  }

}