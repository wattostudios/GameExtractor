
package com.github.barubary.dsdecmp;

import java.io.IOException;

public class InvalidFileException extends IOException {

  private static final long serialVersionUID = -8354901572139075536L;

  public InvalidFileException(String message) {
    super(message);
  }

  public InvalidFileException(String message, Exception innerException) {
    super(message, innerException);
  }

}
