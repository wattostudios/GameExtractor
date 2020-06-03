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
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.watto.Language;

/***********************************************************************************************
 * A dialog splash screen that displays an image and a changable message <br />
 * <br />
 * <i>This is a special class - it doesn't use <code>WSComponent</code>s because this dialog will
 * be shown before the <code>WSComponent</code>s are loaded.</i>
 ***********************************************************************************************/

public class WSSplashDialog extends JDialog {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  /** The singleton instance of the dialog **/
  static WSSplashDialog instance = new WSSplashDialog();

  /** the message to show on the dialog **/
  static JLabel message;
  /** the image **/
  static JLabel image;

  /** the main panel **/
  static JPanel panel = null;

  /***********************************************************************************************
   * Gets the singleton <code>instance</code> of this dialog
   * @return the singleton <code>instance</code>
   ***********************************************************************************************/
  public static WSSplashDialog getInstance() {
    return instance;
  }

  /***********************************************************************************************
   * Sets the message shown on the dialog
   * @param messageCode the message <code>Language</code> code
   ***********************************************************************************************/
  public static void setMessage(String messageCode) {
    message.setText(Language.get("WSSplashDialog_" + messageCode));
  }

  /***********************************************************************************************
   * Constructor
   ***********************************************************************************************/
  public WSSplashDialog() {
    super();
    setModal(false);
    constructInterface();
  }

  /***********************************************************************************************
   * Constructs and shows the dialog
   ***********************************************************************************************/
  public void constructInterface() {
    if (panel == null) {

      setResizable(false);
      setUndecorated(true);
      setTitle("Loading...");

      getContentPane().setLayout(new BorderLayout(0, 0));

      try {
        //image = new JLabel(new ImageIcon(WSHelper.getResource("images/WSSplashDialog/logo.gif")));
        image = new JLabel(new ImageIcon("images/WSSplashDialog/logo.gif"));
      }
      catch (Throwable t) {
        // image does not exist
        image = new JLabel();
      }

      panel = new JPanel(new BorderLayout(0, 0));
      panel.setOpaque(true);

      panel.add(image, BorderLayout.NORTH);

      message = new JLabel("Loading Languages And Initial Settings");
      message.setHorizontalAlignment(JLabel.CENTER);
      panel.add(message, BorderLayout.CENTER);

      getContentPane().add(panel, BorderLayout.CENTER);

      pack();

      setLocationRelativeTo(getParent());
    }

  }

}