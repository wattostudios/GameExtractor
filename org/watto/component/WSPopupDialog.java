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
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.TemporarySettings;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.listener.WSKeyableListener;
import org.watto.xml.XMLReader;

/***********************************************************************************************
 * The actual popup dialog that is shown to the user. Users should create popups using the class
 * <code>WSPopup</code> or <code>WSPopupPanel</code> rather than using this class directly.
 * @see org.watto.component.WSPopup
 ***********************************************************************************************/

public class WSPopupDialog extends JDialog implements WSPopupDialogInterface, WSClickableInterface, WSKeyableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** the popup that this dialog belongs to **/
  WSPopup popup;

  /***********************************************************************************************
   * Creates the <code>WSPopupDialog</code> for the parent <code>popup</code>
   * @param popup the popup owner
   ***********************************************************************************************/
  public WSPopupDialog(WSPopup popup) {
    super();
    this.popup = popup;
    setModal(true);
    getContentPane().setLayout(new BorderLayout());
  }

  /**
   **********************************************************************************************
   * Builds a <code>WSButton</code> specifically for the dialog
   * @param code the <code>Language</code> code of the <code>WSButton</code>
   * @return the <code>WSButton</code>
   **********************************************************************************************
   **/
  public WSButton constructButton(String code) {
    WSButton button = new WSButton(XMLReader.read("<WSButton code=\"" + code + "\" />"));
    button.addKeyListener(new WSKeyableListener(this));
    return button;
  }

  /***********************************************************************************************
   * Builds and shows a popup
   * @param type the type of popup to show
   * @param code the text code of the popup
   * @param hidable whether this popup can be disabled from appearing or not
   ***********************************************************************************************/
  @SuppressWarnings({ "rawtypes", "unchecked" })
  @Override
  public void constructInterface(String type, String code, boolean hidable) {
    try {

      getContentPane().removeAll();

      String settingCode = "Popup_" + code + "_Show";

      setTitle(Language.get("WSPopupDialog_" + type + "_Title"));

      // Setting up the panel for the buttons
      int numButtons = 1;
      if (type.equals(WSPopup.TYPE_CONFIRM)) {
        numButtons++;
      }

      JPanel buttonsPanel = new JPanel(new GridLayout(1, numButtons, 5, 5));
      buttonsPanel.setOpaque(false);

      // Constructing the buttons
      WSButton buttonWithFocus = null;

      if (type.equals(WSPopup.TYPE_MESSAGE) || type.equals(WSPopup.TYPE_OPTION)) {
        buttonWithFocus = constructButton(WSPopup.BUTTON_OK);
        buttonsPanel.add(buttonWithFocus);
      }
      else if (type.equals(WSPopup.TYPE_ERROR)) {
        buttonWithFocus = constructButton(WSPopup.BUTTON_OK);
        buttonsPanel.add(buttonWithFocus);
      }
      else if (type.equals(WSPopup.TYPE_CONFIRM)) {
        buttonWithFocus = constructButton(WSPopup.BUTTON_YES);
        buttonsPanel.add(buttonWithFocus);

        buttonsPanel.add(constructButton(WSPopup.BUTTON_NO));
      }

      WSPopup.setButtonWithFocus(buttonWithFocus);

      // Setting up the panel for the message
      JPanel messagePanel = new JPanel(new BorderLayout(5, 5));
      messagePanel.setOpaque(false);

      // Constructing the message
      WSLabel messageLabel = new WSLabel(XMLReader.read("<WSLabel code=\"" + code + "\" wrap=\"true\" />"));
      messageLabel.setOpaque(false);
      messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
      messagePanel.add(messageLabel, BorderLayout.CENTER);

      // Construct the options (for an Options dialog)
      if (type.equals(WSPopup.TYPE_OPTION)) {
        // Get the list of options
        try {
          String[] optionsList = (String[]) SingletonManager.get("Options_" + code);
          String selectedOption = TemporarySettings.getString("SelectedOption_" + code);

          WSComboBox optionsCombo = new WSComboBox(XMLReader.read("<WSComboBox code=\"WSPopup_OptionsCombo\" />"));
          optionsCombo.setInRepository(true);
          optionsCombo.setModel(new DefaultComboBoxModel(optionsList));
          optionsCombo.setSelectedItem(selectedOption);
          messagePanel.add(optionsCombo, BorderLayout.SOUTH);

        }
        catch (Throwable t) {
        }
      }

      // Setting up the panel for the checkbox
      JPanel checkboxPanel = new JPanel(new BorderLayout(5, 5));
      checkboxPanel.setOpaque(false);

      // Constructing the checkbox
      if (hidable) {
        JCheckBox hidableCheckbox = new JCheckBox(Language.get("PopupDialog_HidableCheckBox_Message"));
        hidableCheckbox.setOpaque(false);
        hidableCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
        hidableCheckbox.setSelected(Settings.getBoolean(settingCode));
        checkboxPanel.add(hidableCheckbox, BorderLayout.CENTER);
        WSPopup.setHidableCheckbox(hidableCheckbox);
      }

      // Constructing the center panel
      JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
      centerPanel.setOpaque(false);

      centerPanel.add(checkboxPanel, BorderLayout.SOUTH);
      centerPanel.add(messagePanel, BorderLayout.CENTER);

      WSPanel overallPanel = new WSPanel(XMLReader.read("<WSPanel showBorder=\"true\" />"));

      try {
        //JLabel iconLabel = new JLabel(new ImageIcon(WSHelper.getResource("images/WSLabel/" + type + ".png")));
        JLabel iconLabel = new JLabel(new ImageIcon("images/WSLabel/" + type + ".png"));
        overallPanel.add(iconLabel, BorderLayout.WEST);
      }
      catch (Throwable t) {
        // image doesn't exist
      }

      // Constructing the dialog
      overallPanel.add(centerPanel, BorderLayout.CENTER);
      overallPanel.add(buttonsPanel, BorderLayout.SOUTH);

      getContentPane().add(overallPanel, BorderLayout.CENTER);

      pack();
      setSize(400, getHeight() + 10);
      setLocationRelativeTo(null);
      buttonWithFocus.requestFocus();
      setVisible(true);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Closes the popup when a button was pressed, and sets the <code>pressedEvent</code>.
   * @param component the <code>Component</code> that triggered the <code>event</code>
   * @param event the <code>MouseEvent</code> that was triggered
   * @return true
   ***********************************************************************************************/
  @Override
  public boolean onClick(JComponent component, MouseEvent event) {
    if (component instanceof JButton) {
      popup.onClick(component, event);
    }
    return true;
  }

  /***********************************************************************************************
   * Pressed the button when the Enter key is pressed
   * @param component the <code>Component</code> that triggered the <code>event</code>
   * @param event the <code>KeyEvent</code> that was triggered
   * @return true
   ***********************************************************************************************/
  @Override
  public boolean onKeyPress(JComponent component, KeyEvent event) {
    popup.onKeyPress(component, event);
    return true;
  }

  /***********************************************************************************************
   * Waits for the user to click something before continuing with the Thread
   ***********************************************************************************************/
  @Override
  public void waitForClick() {
    // don't need to do anything - it's a modal dialog already!
  }

}