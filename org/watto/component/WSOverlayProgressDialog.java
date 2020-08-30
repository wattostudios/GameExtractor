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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import org.watto.Language;
import org.watto.timer.RepaintWhileVisibleThread;
import org.watto.xml.XMLReader;

/***********************************************************************************************
A Progress Dialog GUI <code>Component</code>
***********************************************************************************************/

public class WSOverlayProgressDialog extends JPanel implements WSProgressDialogInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** the singleton instance of the dialog **/
  static WSOverlayProgressDialog instance = null;

  /** the progress bars **/
  static JProgressBar[] bars = new JProgressBar[1];

  /** the number of progress bars **/
  static int barCount = 1;

  /** the status message label **/
  static JLabel message = new JLabel();

  /** If the maximum is too large, the maximum and actual values need to be scaled down **/
  static int scalingFactor = 0;

  /***********************************************************************************************
  Gets the singleton <code>instance</code> of this <code>WSProgressDialog</code>
  @return the singleton <code>instance</code>
  ***********************************************************************************************/
  public static WSOverlayProgressDialog getInstance() {
    if (instance == null) {
      instance = new WSOverlayProgressDialog();
    }
    return instance;
  }

  /***********************************************************************************************
  Constructor for extended classes only
  ***********************************************************************************************/
  public WSOverlayProgressDialog() {
    super();
    constructInterface();
  }

  /***********************************************************************************************
  Creates the interface for the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  public void constructInterface() {
    //instance.setDefaultLookAndFeelDecorated(false); // don't want to show the Windows-style border around the popup

    removeAll();
    setLayout(new BorderLayout(0, 0));
    setOpaque(false);

    WSPanel overallPanel = new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" showBorder=\"true\" border-width=\"6\" />"));

    WSPanel mainPanel = new WSPanel(XMLReader.read("<WSPanel opaque=\"false\" vertical-gap=\"6\" />"));
    mainPanel.add(message, BorderLayout.CENTER);

    WSPanel barPanel = new WSPanel(XMLReader.read("<WSPanel layout=\"GridLayout\" rows=\"" + barCount + "\" columns=\"1\" />"));
    barPanel.setOpaque(false);

    bars = new JProgressBar[barCount];

    for (int i = 0; i < barCount; i++) {
      JProgressBar bar = new JProgressBar();

      bar.setOpaque(false);
      bar.setPreferredSize(new Dimension(200, 20));
      bar.setStringPainted(true);

      barPanel.add(bar);
      bars[i] = bar;
    }

    mainPanel.add(barPanel, BorderLayout.SOUTH);

    overallPanel.add(mainPanel, BorderLayout.CENTER);

    message.setHorizontalAlignment(JLabel.CENTER);

    add(overallPanel, BorderLayout.CENTER);

    //setMinimumSize(new Dimension(200, 100));
    //progress.setSize(new Dimension(200,20));
  }

  /***********************************************************************************************
  Sets this <code>WSProgressDialog</code> to show the main <code>JProgressBar</code> as indeterminate
  @param indeterminate <b>true</b> if the main <code>JProgressBar</code> is indeterminate<br />
                       <b>false</b> if it shows real values
  ***********************************************************************************************/
  @Override
  public void setIndeterminate(boolean indeterminate) {
    setIndeterminate(indeterminate, 0);
  }

  /***********************************************************************************************
  Sets this <code>WSProgressDialog</code> to show indeterminate <code>JProgressBar</code>s
  @param indeterminate <b>true</b> if the <code>JProgressBar</code>s are indeterminate<br />
                       <b>false</b> if they show real values
  @param barNumber the <code>JProgressBar</code> to set as indeterminate
  ***********************************************************************************************/
  @Override
  public void setIndeterminate(boolean indeterminate, int barNumber) {
    try {
      bars[barNumber].setIndeterminate(indeterminate);
      if (indeterminate) {
        // if true, need to repaint the indeterminate progress
        //new RepaintThread_WhileVisible(this,50).start();
        new RepaintWhileVisibleThread(bars[barNumber], 50).start();
      }
    }
    catch (Throwable t) {
    }
  }

  /***********************************************************************************************
  Sets the maximum value of the main <code>JProgressBar</code>
  @param newMaximum the new maximum value
  ***********************************************************************************************/
  @Override
  public void setMaximum(long newMaximum) {
    setMaximum(newMaximum, 0);
  }

  /***********************************************************************************************
  Sets the maximum value of the given <code>JProgressBar</code>
  @param newMaximum the new maximum value
  @param barNumber the <code>JProgressBar</code> to set the maximum value of
  ***********************************************************************************************/
  @Override
  public void setMaximum(long newMaximum, int barNumber) {
    try {
      if (barCount > 1) {
        // if the barNumber is indeterminate, looks for the next determinate bar and changes that instead
        for (; barNumber < barCount; barNumber++) {
          if (!bars[barNumber].isIndeterminate()) {
            bars[barNumber].setMinimum(0);
            bars[barNumber].setValue(0);

            scalingFactor = 0;
            while (newMaximum > Integer.MAX_VALUE) {
              // need to apply a scaling factor
              newMaximum >>= 1;
              scalingFactor++;
            }

            bars[barNumber].setMaximum((int) newMaximum);
            return;
          }
        }
      }
      else {
        bars[barNumber].setMinimum(0);
        bars[barNumber].setValue(0);

        scalingFactor = 0;
        while (newMaximum > Integer.MAX_VALUE) {
          // need to apply a scaling factor
          newMaximum >>= 1;
          scalingFactor++;
        }

        bars[barNumber].setMaximum((int) newMaximum);
      }
    }
    catch (Throwable t) {
    }
  }

  /***********************************************************************************************
  Sets the message shown on the <code>WSProgressDialog</code>
  @param newMessage the message to show
  ***********************************************************************************************/
  @Override
  public void setMessage(String newMessage) {
    message.setText(newMessage);
  }

  /***********************************************************************************************
  Sets the number of <code>JProgressBar</code>s to show on the <code>WSProgressDialog</code>
  @param newNumbars the number of <code>JProgressBar</code>s to show
  ***********************************************************************************************/
  @Override
  public void setNumberOfBars(int newNumBars) {
    barCount = newNumBars;
    constructInterface();
  }

  /***********************************************************************************************
  Sets the current value of the main <code>JProgressBar</code>
  @param newValue the new current value
  ***********************************************************************************************/
  @Override
  public void setValue(long newValue) {
    setValue(newValue, 0);
  }

  /***********************************************************************************************
  Sets the current value of the given <code>JProgressBar</code>
  @param newValue the new current value
  @param barNumber the <code>JProgressBar</code> to set the current value of
  ***********************************************************************************************/
  @Override
  public void setValue(long newValue, int barNumber) {
    try {
      if (barCount > 1) {
        // if the barNumber is indeterminate, looks for the next determinate bar and changes that instead
        for (; barNumber < barCount; barNumber++) {
          if (!bars[barNumber].isIndeterminate()) {
            if (scalingFactor != 0) {
              newValue >>= scalingFactor;
            }
            bars[barNumber].setValue((int) newValue);
            return;
          }
        }
      }
      else {
        if (barNumber < bars.length) {
          if (scalingFactor != 0) {
            newValue >>= scalingFactor;
          }
          bars[barNumber].setValue((int) newValue);
        }
      }
    }
    catch (Throwable t) {
    }
  }

  /***********************************************************************************************
  Shows or hides the <code>WSProgressDialog</code>
  @param visible <b>true</b> to show the <code>WSProgressDialog</code><br />
                 <b>false</b> to hide the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  @Override
  public void setVisible(boolean visible) {
    for (int i = 0; i < barCount; i++) {
      if (bars[i] != null) {
        bars[i].setVisible(visible);
      }
    }

    Dimension size = getSize();
    size.height += 10;
    size.width += 10;
    setSize(size);

    super.setVisible(visible);

    if (!visible) {
      // Remove *this* from the overlayPanel
      WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
      if (overlayPanel != null) {
        overlayPanel.removeAll();
        overlayPanel.setVisible(visible);
      }
    }
  }

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with a single <code>JProgressBar</code> and a <i>Please
  Wait</i> message
  @param newMaximum the maximum value of the <code>JProgressBar</code>
  ***********************************************************************************************/
  @Override
  public void show(int newMaximum) {
    show(newMaximum, Language.get("Progress_PleaseWait"));
  }

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with multiple <code>JProgressBar</code>s
  @param numBars the number of <code>JProgressBar</code>s to show
  @param newMaximum the maximum value of the <code>JProgressBar</code>s
  @param newMessage the message to show on the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  @Override
  public void show(int numBars, int newMaximum, String newMessage) {
    setNumberOfBars(numBars);
    show(newMaximum, newMessage);
  }

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with a single <code>JProgressBar</code>
  @param newMaximum the maximum value of the <code>JProgressBar</code>
  @param newMessage the message to show on the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  @Override
  public void show(int newMaximum, String newMessage) {
    // Build the panel
    setMaximum(newMaximum);
    setMessage(newMessage);

    // Add *this* to the overlay panel
    WSPanel overlayPanel = (WSPanel) ComponentRepository.get("PopupOverlay");
    if (overlayPanel != null) {
      // Add the panel to the overlay
      overlayPanel.removeAll();
      overlayPanel.add(this);

      // set the background color around the panel
      overlayPanel.setObeyBackgroundColor(true);
      overlayPanel.setBackground(new Color(255, 255, 255, 110));

      // Validate and show the display
      overlayPanel.validate();
      overlayPanel.setVisible(true);
    }

    // Now show it
    setVisible(true);
  }

  /***********************************************************************************************
  Shows the <code>WSProgressDialog</code> with a single <code>JProgressBar</code>
  @param newMessage the message to show on the <code>WSProgressDialog</code>
  ***********************************************************************************************/
  @Override
  public void show(String newMessage) {
    show(0, newMessage);
  }
}