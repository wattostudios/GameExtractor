package net.sourceforge.lhadecompressor;

import java.io.*;

/**
 * 
 * @author Nobuyasu SUEHIRO <nosue@users.sourceforge.net>
 */
public interface LhaDecoder {
	public int read(byte[] b, int off, int len) throws LhaException, IOException;
	public void close();
}
