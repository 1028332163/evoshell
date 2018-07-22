package neu.lab.evoshell;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;

import neu.lab.evoshell.modify.MethodModifier;

/**
 * @author asus validate this path whether executes to end.
 */
public class CallPathValidator {
	private static final int NO_ERROR = 0;
	private static final int MODIFY_METHOD_ERROR = 1;
	private static final int EVOSUITE_ERROR = 2;

	private static final String shellPath = "D:\\cEnvironment\\repository\\org\\ow2\\asm\\asm\\6.0\\asm-6.0.jar;"
			+ "D:\\cEnvironment\\repository\\org\\ow2\\asm\\asm-util\\6.0\\asm-util-6.0.jar;"
			+ "D:\\cEnvironment\\repository\\org\\ow2\\asm\\asm-tree\\6.0\\asm-tree-6.0.jar;"
			+ "D:\\cWS\\eclipse1\\evoshell\\target\\classes";
	String classPath;

	String pomPath;// D:\\cWS\\eclipse1\\testcase.top
	String entryClass;// neu.lab.testcase.top.MthdTop
	String riskMthd;// <neu.lab.testcase.bottom.MthdBottom: void m2()>
	String distanceFile;// "D:\ws_testcase\image\distance_mthdBranch\neu.lab+testcase.top+1.0@neu.lab+testcase.bottom@1.0.txt"

	String[] mthds;
	String[] jarPaths;

	List<String> modifyMthdCmds;
	String evoCmd;

	public CallPathValidator(String classPath, String pomPath, String distanceFile, String[] mthds, String[] jarPaths) {
		super();
		this.classPath = shellPath + File.pathSeparator + classPath;
		this.pomPath = pomPath;
		this.entryClass = MthdFormatUtil.sootMthd2cls(mthds[0]);
		this.riskMthd = mthds[mthds.length - 1];
		this.distanceFile = distanceFile;
		this.mthds = mthds;
		this.jarPaths = jarPaths;
		this.modifyMthdCmds = new ArrayList<String>();
	}

	private void exeEvosuite() throws ExecuteException, IOException {
		evoCmd = "D:\\cTool\\apache-maven-3.2.5\\bin\\mvn.bat org.evosuite.plugins:evosuite-maven-plugin:8.15:generate -f=" + pomPath
				+ " -Dmodify_cp=D:\\ws_testcase\\modifyCp -Dclass=" + entryClass
				+ " -Dcriterion=MTHD_PROB_RISK -Drisk_method=\"" + riskMthd + "\" -Dmthd_prob_distance_file="
				+ distanceFile + " -Dmaven.test.skip=true -e";
		exeCmd(evoCmd);
	}

	private void modifyMthdOnPath() throws Exception {
		FileUtil.delFolder(ShellConfig.modifyCp);
		for (int i = 0; i < mthds.length - 1; i++) {
			String cmd = "java -cp " + classPath + " neu.lab.evoshell.modify.MethodModifier \"" + mthds[i] + "\" \""
					+ jarPaths[i] + "\" \"" + mthds[i + 1] + "\"";
			modifyMthdCmds.add(cmd);
			exeCmd(cmd);
		}
	}

	private void cmpResult(int result, Exception resultException) {
		// TODO Auto-generated method stub
		try {
			PrintWriter printer = new PrintWriter(new BufferedWriter(new FileWriter(ShellConfig.cmpResult, true)));
			printer.println(evoCmd);
			for (String modifyMthdCmd : modifyMthdCmds) {
				printer.println(modifyMthdCmd);
			}
			
			printer.println("--ideal call:");
			for(String mthd:mthds) {
				printer.println(mthd);
			}
			
			printer.println("--covered method:");
			List<String> results = new ArrayList<String>();
			BufferedReader reader = new BufferedReader(new FileReader(ShellConfig.reachedResult));
			String line = reader.readLine();
			while (line != null) {
				if (!line.equals("")) {
					results.add(line);
				}
				line = reader.readLine();
			}
			reader.close();
			for(String mthd:results) {
				printer.println(mthd);
			}
			
			printer.println("result:" + result);
			
			if(results.contains(MthdFormatUtil.soot2evo(riskMthd))) {
				printer.println("success!");
			}
			if (resultException != null) {
				resultException.printStackTrace(printer);
				printer.println("\n");
			}
			printer.println();
			printer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void validateCallPath() {
		
		System.out.println("validate for ");
		System.out.println("pom:"+pomPath);
		for(String mthd:this.mthds) {
			System.out.println(mthd);
		}
		
		int resultFlag = CallPathValidator.NO_ERROR;
		Exception resultException = null;

		try {
			modifyMthdOnPath();// make class don't have branch.
		} catch (Exception e) {
			resultFlag = CallPathValidator.MODIFY_METHOD_ERROR;
			resultException = e;
			e.printStackTrace();
		}

		try {
			exeEvosuite();
		} catch (Exception e) {
			resultFlag = CallPathValidator.EVOSUITE_ERROR;
			resultException = e;
			e.printStackTrace();
		}

		cmpResult(resultFlag, resultException);
		System.out.println();
	}

	public static void exeCmd(String mvnCmd) throws ExecuteException, IOException {
		System.out.println("----execute cmd:" + mvnCmd);
		 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 System.out.println("start time：" + sdf.format(new Date()));
		 CommandLine cmdLine = CommandLine.parse(mvnCmd);
		 DefaultExecutor executor = new DefaultExecutor();
		 executor.execute(cmdLine);
	}

	public static void main(String[] args) throws Exception {
		// BufferedReader reader = new BufferedReader(new FileReader(
		// "D:\\ws_testcase\\image\\path\\neu.lab+testcase.top+1.0@neu.lab+testcase.bottom@1.0.txt"));
	
	

	}
	private static void test() {
//		BufferedReader reader = new BufferedReader(new FileReader(
//		));
//String line = reader.readLine();
//List<String> mthds = new ArrayList<String>();
//List<String> jarPaths = new ArrayList<String>();
//while (line != null) {
//	if (!line.equals("")) {
//		if (line.startsWith("pathLen")) {
//
//		} else {
//			System.out.println(line);
//			String[] mthd_path = line.split("> ");
//			if (mthd_path.length == 2) {
//				mthds.add(mthd_path[0] + ">");
//				jarPaths.add(mthd_path[1]);
//			} else {
//				mthds.add(mthd_path[0]);
//				// new CallPathValidator("", mthds.toArray(new String[0]), jarPaths.toArray(new
//				// String[0]))
//				// .modifyMthdOnPath();
//				break;
//			}
//		}
//	}
//	line = reader.readLine();
//}
//reader.close();
	}
}
