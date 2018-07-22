package neu.lab.evoshell;

import java.io.File;
import java.io.IOException;

public class ExeDir {
	public static void main(String[] args) throws IOException {
		FileSyn fileSyn = new FileSyn("D:\\ws_testcase\\image\\syn_modifyEvo.txt");
		File reachedResult = new File(ShellConfig.cmpResult);
		if(reachedResult.exists()) {
			reachedResult.delete();
		}
		File pathDir = new File("D:\\ws_testcase\\distance_path\\path");
		for(File child:pathDir.listFiles()) {
			try {
				new CallPaths(child.getAbsolutePath()).validatePaths();
			} catch (Exception e) {
				e.printStackTrace();
			}
			fileSyn.add(child.getAbsolutePath());
		}
	}
}
