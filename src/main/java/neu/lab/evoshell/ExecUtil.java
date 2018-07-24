package neu.lab.evoshell;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;

public class ExecUtil {

	public static void exeCmd(String mvnCmd) throws ExecuteException, IOException {
		System.out.println("----execute cmd:" + mvnCmd);
		 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 System.out.println("start time：" + sdf.format(new Date()));
		 CommandLine cmdLine = CommandLine.parse(mvnCmd);
		 DefaultExecutor executor = new DefaultExecutor();
		 executor.execute(cmdLine);
	}

}
