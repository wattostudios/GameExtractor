package com.github.junrar;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;


public class MVTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = "/home/rogiel/fs/home/ae721273-eade-45e7-8112-d14115ebae56/Village People - Y.M.C.A.mp3.part1.rar";
		File f = new File(filename);
		RarArchive a = null;
		try {
			a = new RarArchive(new FileVolumeManager(f));
		} catch (RarException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (a != null) {
			a.getMainHeader().print();
			FileHeader fh = a.nextFileHeader();
			while (fh != null) {
				try {
					File out = new File("/home/rogiel/fs/test/"
							+ fh.getFileNameString().trim());
					System.out.println(out.getAbsolutePath());
					FileOutputStream os = new FileOutputStream(out);
					a.extractFile(fh, os);
					os.close();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (RarException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fh = a.nextFileHeader();
			}
		}
	}
}
