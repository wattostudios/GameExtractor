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

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.TemporarySettings;
import org.watto.component.task.Task_ShowPopupInNewThread;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.task.Task;

/***********************************************************************************************
A popup that can either show an error, message, or confirmation. The user needs to click a
<code>WSButton</code> to close the popup, which passes the pressed button back to the caller.
***********************************************************************************************/

public class WSPopup extends JComponent implements WSClickableInterface, WSKeyableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** The code for the language and settings **/
  static String code = null;

  /** so only 1 popup will ever be displayed to the user at a time! **/
  static WSPopupDialogInterface instance = null;

  /** A message popup **/
  public static String TYPE_MESSAGE = "Message";

  /** An error popup **/
  public static String TYPE_ERROR = "Error";

  /** A confirmation popup **/
  public static String TYPE_CONFIRM = "Confirm";

  /** A popup asking the user to choose a value from a list of options (a ComboBox) **/
  public static String TYPE_OPTION = "Option";

  /** The OK Button **/
  public static String BUTTON_OK = "OK";

  /** The Yes Button **/
  public static String BUTTON_YES = "Yes";

  /** The No Button **/
  public static String BUTTON_NO = "No";

  /** Whether the panel can be disabled from appearing **/
  static boolean hidable = false;

  /** The type of the popup **/
  static String type = TYPE_MESSAGE;

  /** The value of the button that was pressed **/
  static String pressedEvent = null;

  /** The button with focus **/
  static WSButton buttonWithFocus = null;

  /** The checkbox that asks to hide the popup **/
  static JCheckBox hidableCheckbox = null;

  /***********************************************************************************************
  Shows a message <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @return the button that was pressed
  ***********************************************************************************************/
  public static void generateNewThread(String typeIn, String codeIn, boolean hidableIn) {
    Task_ShowPopupInNewThread task = new Task_ShowPopupInNewThread(typeIn, codeIn, hidableIn);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

  /***********************************************************************************************
  Gets the singleton <code>instance</code> of the <code>WSPopupDialog</code>
  @return the <code>instance</code> <code>WSPopupDialog</code>
  ***********************************************************************************************/
  public static WSPopupDialogInterface getInstance() {
    return instance;
  }

  /***********************************************************************************************
  Sets the <code>WSButton</code> that has focus
  @param button the <code>WSButton</code> that has focus
  ***********************************************************************************************/
  public static void setButtonWithFocus(WSButton button) {
    buttonWithFocus = button;
  }

  /***********************************************************************************************
  Sets the <code>hidableCheckbox</code>
  @param checkbox the hidable <code>JCheckBox</code>
  ***********************************************************************************************/
  public static void setHidableCheckbox(JCheckBox checkbox) {
    hidableCheckbox = checkbox;
  }

  /***********************************************************************************************
  Shows the <code>WSPopupDialog</code> popup of the given <code>type</code>
  @param typeIn the type of popup to show
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @return the button that was pressed
  ***********************************************************************************************/
  public static String show(String typeIn, String codeIn, boolean hidableIn) {
    code = codeIn;
    hidable = hidableIn;
    type = typeIn;
    pressedEvent = null;
    hidableCheckbox = null;

    String settingCode = "Popup_" + code + "_Show";

    if (hidable && !Settings.getBoolean(settingCode)) {
      return BUTTON_OK;
    }

    instance.constructInterface(type, code, hidable);
    instance.waitForClick();

    if (hidable) {
      Settings.set(settingCode, hidableCheckbox.isSelected());
    }

    return pressedEvent;

  }

  /***********************************************************************************************
  Sets the options and selected value that will appear in an Options ComboBox popup
  @param codeIn the text code of the popup
  @param options the options to display in the ComboBox
  @param selectedValue the default selected value of the ComboBox (or null)
  ***********************************************************************************************/
  public static void setOptionsList(String codeIn, String[] options, String selectedValue) {
    SingletonManager.set("Options_" + codeIn, options);
    TemporarySettings.set("SelectedOption_" + codeIn, selectedValue);
  }

  /***********************************************************************************************
  Shows a non-hidable <code>WSPopupDialog</code> popup where the user chooses a value from a ComboBox
  @param codeIn the text code of the popup
  @param options the options to display in the ComboBox
  @param selectedValue the default selected value of the ComboBox (or null)
  @return the value that was chosen from the ComboBox
  ***********************************************************************************************/
  public static String showOption(String codeIn, String[] options, String selectedValue) {
    setOptionsList(codeIn, options, selectedValue);
    return show(TYPE_OPTION, codeIn, false);
  }

  /***********************************************************************************************
  Shows a <code>WSPopupDialog</code> popup where the user chooses a value from a ComboBox
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @param options the options to display in the ComboBox
  @param selectedValue the default selected value of the ComboBox (or null)
  @return the value that was chosen from the ComboBox
  ***********************************************************************************************/
  public static String showOption(String codeIn, boolean hidableIn, String[] options, String selectedValue) {
    setOptionsList(codeIn, options, selectedValue);
    return show(TYPE_OPTION, codeIn, hidableIn);
  }

  /***********************************************************************************************
  Shows a non-hidable <code>WSPopupDialog</code> popup where the user chooses a value from a ComboBox
  @param codeIn the text code of the popup
  @param options the options to display in the ComboBox
  @param selectedValue the default selected value of the ComboBox (or null)
  @return the value that was chosen from the ComboBox
  ***********************************************************************************************/
  public static void showOptionInNewThread(String codeIn, String[] options, String selectedValue) {
    setOptionsList(codeIn, options, selectedValue);
    generateNewThread(TYPE_OPTION, codeIn, false);
  }

  /***********************************************************************************************
  Shows a <code>WSPopupDialog</code> popup where the user chooses a value from a ComboBox
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @param options the options to display in the ComboBox
  @param selectedValue the default selected value of the ComboBox (or null)
  @return the value that was chosen from the ComboBox
  ***********************************************************************************************/
  public static void showOptionInNewThread(String codeIn, boolean hidableIn, String[] options, String selectedValue) {
    setOptionsList(codeIn, options, selectedValue);
    generateNewThread(TYPE_OPTION, codeIn, hidableIn);
  }

  /***********************************************************************************************
  Shows a non-hidable confirmation <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @return the button that was pressed
  ***********************************************************************************************/
  public static String showConfirm(String codeIn) {
    return show(TYPE_CONFIRM, codeIn, false);
  }

  /***********************************************************************************************
  Shows a confirmation <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @return the button that was pressed
  ***********************************************************************************************/
  public static String showConfirm(String codeIn, boolean hidableIn) {
    return show(TYPE_CONFIRM, codeIn, hidableIn);
  }

  /***********************************************************************************************
  Shows a non-hidable confirmation <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @return the button that was pressed
  ***********************************************************************************************/
  public static void showConfirmInNewThread(String codeIn) {
    generateNewThread(TYPE_CONFIRM, codeIn, false);
  }

  /***********************************************************************************************
  Shows a confirmation <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @return the button that was pressed
  ***********************************************************************************************/
  public static void showConfirmInNewThread(String codeIn, boolean hidableIn) {
    generateNewThread(TYPE_CONFIRM, codeIn, hidableIn);
  }

  /***********************************************************************************************
  Shows a non-hidable error <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @return the button that was pressed
  ***********************************************************************************************/
  public static String showError(String codeIn) {
    return show(TYPE_ERROR, codeIn, false);
  }

  /***********************************************************************************************
  Shows an error <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @return the button that was pressed
  ***********************************************************************************************/
  public static String showError(String codeIn, boolean hidableIn) {
    return show(TYPE_ERROR, codeIn, hidableIn);
  }

  /***********************************************************************************************
  Shows a non-hidable error <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @return the button that was pressed
  ***********************************************************************************************/
  public static void showErrorInNewThread(String codeIn) {
    generateNewThread(TYPE_ERROR, codeIn, false);
  }

  /***********************************************************************************************
  Shows an error <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @return the button that was pressed
  ***********************************************************************************************/
  public static void showErrorInNewThread(String codeIn, boolean hidableIn) {
    generateNewThread(TYPE_ERROR, codeIn, hidableIn);
  }

  /***********************************************************************************************
  Shows a non-hidable message <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @return the button that was pressed
  ***********************************************************************************************/
  public static String showMessage(String codeIn) {
    return show(TYPE_MESSAGE, codeIn, false);
  }

  /***********************************************************************************************
  Shows a message <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @return the button that was pressed
  ***********************************************************************************************/
  public static String showMessage(String codeIn, boolean hidableIn) {
    return show(TYPE_MESSAGE, codeIn, hidableIn);
  }

  /***********************************************************************************************
  Shows a non-hidable message <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @return the button that was pressed
  ***********************************************************************************************/
  public static void showMessageInNewThread(String codeIn) {
    generateNewThread(TYPE_MESSAGE, codeIn, false);
  }

  /***********************************************************************************************
  Shows a message <code>WSPopupDialog</code> popup
  @param codeIn the text code of the popup
  @param hidableIn whether the popup can be disabled from appearing or not
  @return the button that was pressed
  ***********************************************************************************************/
  public static void showMessageInNewThread(String codeIn, boolean hidableIn) {
    generateNewThread(TYPE_MESSAGE, codeIn, hidableIn);
  }

  /***********************************************************************************************
  Creates the <code>instance</code> <code>WSPopupDialog</code>
  ***********************************************************************************************/
  public WSPopup() {
    super();
    if (instance == null) {

      if (ComponentRepository.has("PopupOverlay")) {
        // use the OverlayPopupDialog
        instance = new WSOverlayPopupDialog(this);
      }
      else {
        // use the Popup PopupDialog
        instance = new WSPopupDialog(this);
      }

    }
  }

  /***********************************************************************************************
  Closes the popup when a button was pressed, and sets the <code>pressedEvent</code>.
  @param component the <code>Component</code> that triggered the <code>event</code>
  @param event the <code>MouseEvent</code> that was triggered
  @return true
  ***********************************************************************************************/
  @Override
  public boolean onClick(JComponent component, MouseEvent event) {
    if (component instanceof JButton) {
      pressedEvent = ((WSComponent) component).getCode();

      if (type.equals(TYPE_OPTION)) {
        // We want to return the selected value, not the button that was clicked.
        try {
          WSComponent comboComponent = ComponentRepository.get("WSPopup_OptionsCombo");
          if (comboComponent != null && comboComponent instanceof WSComboBox) {
            WSComboBox comboBox = (WSComboBox) comboComponent;
            String selectedValue = (String) comboBox.getSelectedItem();
            pressedEvent = selectedValue;
          }
        }
        catch (Throwable t) {
        }
      }

      instance.dispose();
    }
    return true;
  }

  /***********************************************************************************************
  Pressed the button when the Enter key is pressed
  @param component the <code>Component</code> that triggered the <code>event</code>
  @param event the <code>KeyEvent</code> that was triggered
  @return true
  ***********************************************************************************************/
  @Override
  public boolean onKeyPress(JComponent component, KeyEvent event) {
    if (event.getKeyCode() == KeyEvent.VK_ENTER) {
      if (component instanceof WSButton) {
        WSButton button = (WSButton) component;
        instance.onClick(button, new MouseEvent(button, MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, 1, false));
      }
    }

    return true;

  }

}