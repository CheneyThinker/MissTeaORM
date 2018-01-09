package com.tea.orm.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class CloneUtils {

	@SuppressWarnings("unchecked")
	public static <T> T deepCloneObject(T t) {
		ByteArrayOutputStream bos = null;
		ObjectOutputStream oos = null;
		ByteArrayInputStream bis = null;
		ObjectInputStream ois = null;
		try {
			bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			oos.writeObject(t);
			bis = new ByteArrayInputStream(bos.toByteArray());
			ois = new ObjectInputStream(bis);
			return (T) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			try {
				ois.close(); ois = null;
				bis.close(); bis = null;
				oos.close(); oos = null;
				bos.close(); bos = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
}
