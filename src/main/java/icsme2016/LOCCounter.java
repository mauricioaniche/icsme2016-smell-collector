package icsme2016;

import java.io.BufferedReader;
import java.io.FileReader;

public class LOCCounter {

	public static int count(String path) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(path));
			int lines = 0;
			
			String line = null;
			do {
				line = reader.readLine();
				if(line!=null && !line.trim().isEmpty()) lines++;
			} while(line!=null);
			reader.close();
			return lines;
		} catch (Exception e) {
			System.out.println("erro");
			return 0;
		}
	}

}
