package neu.lab.evoshell.modify;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.util.TraceClassVisitor;

import neu.lab.evoshell.FileUtil;
import neu.lab.evoshell.MthdFormatUtil;
import neu.lab.evoshell.ShellConfig;

public class MethodModifier {

	private String erM;// soot-format
	private String erJarPath;
	private String eeM;// soot-format
	// private String eeJarPath;

	public MethodModifier(String erM, String erJarPath, String eeM) {
		super();
		this.erM = erM;
		this.erJarPath = erJarPath;
		this.eeM = eeM;
	}

	public void modifyMthd() throws Exception {
		// TODO writeHalfByte before modify.
//		writeModifiedByteCode(getErClassStream(), "d:\\cWs\\notepad++\\ClassBefore.txt");

		ClassReader cr = new ClassReader(getErClassStream());
		ClassNode classNode = new ClassNode();
		cr.accept(classNode, 0);

		String evoErM = MthdFormatUtil.soot2evo(erM);
		boolean findMthd2modify = false;
		for (MethodNode mn : classNode.methods) {
			String evoMthd = classNode.name.replace("/", ".") + "." + mn.name + mn.desc;
			if (evoErM.equals(evoMthd)) {
				findMthd2modify = true;

				// filter node
				deleteBranch(mn);
				// mn.tryCatchBlocks = null;
			}
		}
		if (!findMthd2modify) {
			throw new Exception("can't find " + erM);
		}

		// TODO write result
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(cw);
		byte[] b = cw.toByteArray();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getOutFilePath()));
		out.write(b);
		out.close();
		// TODO writeHalfByte after modify.
		// writeModifiedByteCode(new FileInputStream(getOutFilePath()),
		// "d:\\cWs\\notepad++\\ClassAfter.txt");
	}

	private void writeModifiedByteCode(InputStream inClass, String outPath) {
		try {
			ClassReader cr = new ClassReader(inClass);

			PrintWriter p1 = new PrintWriter(new FileWriter(outPath, false));
			cr.accept(new TraceClassVisitor(p1), ClassReader.SKIP_DEBUG);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private String getOutFilePath() {
		String path = ShellConfig.modifyCp + MthdFormatUtil.sootMthd2cls(erM).replace(".", File.separator) + ".class";
		File outFile = new File(path);
		if (!outFile.getParentFile().exists()) {
			outFile.getParentFile().mkdirs();
		}
		return path;
	}

	private void deleteBranch(MethodNode mn) throws Exception {
		// TODO remaining path
		// ExeLabelPaths allPath = getAllExePath(mn);
		// System.out.println(allPath.getPathsStr());
		// List<LabelNode> callLabels = getCallLabels(mn, MthdFormatUtil.soot2evo(eeM));
		// for (LabelNode label : callLabels) {
		// System.out.println("callLabel:" + label);
		// }
		// ExeLabelPath remianPath = allPath.getRemainPath(callLabels);

		List<LabelNode> catchEndLabels = new ArrayList<LabelNode>();
		for (TryCatchBlockNode tryCatch : mn.tryCatchBlocks) {
			catchEndLabels.add(tryCatch.end);

		}

		ListIterator<AbstractInsnNode> ite = mn.instructions.iterator();
		LabelNode currentLabel = null;
		while (ite.hasNext()) {
			AbstractInsnNode insNode = ite.next();
			// System.out.println(insNode);
			if (insNode instanceof LabelNode) {
				currentLabel = (LabelNode) insNode;
			}
			if (insNode instanceof JumpInsnNode && !catchEndLabels.contains(currentLabel)) {
				JumpInsnNode jumpNode = ((JumpInsnNode) insNode);
				if (jumpNode.getOpcode() == Opcodes.GOTO) {
					ite.remove();
				}
					
			}
			if (insNode instanceof LineNumberNode) {
				// System.out.println(((LineNumberNode)insNode).line);
			}
			// //TODO remove else-body
			// else if (!remianPath.contains(currentLabel)) {
			// ite.remove();
			// }

		}
		System.out.println("======");
	}

	/**
	 * LabelNodes whose statements call evoMthd.
	 * 
	 * @param mn
	 * @param evoMthd
	 * @return
	 * @throws Exception
	 */
	private List<LabelNode> getCallLabels(MethodNode mn, String evoMthd) throws Exception {
		List<LabelNode> callLabels = new ArrayList<LabelNode>();
		ListIterator<AbstractInsnNode> ite = mn.instructions.iterator();
		LabelNode currentLabel = null;
		while (ite.hasNext()) {
			AbstractInsnNode insNode = ite.next();
			if (insNode instanceof LabelNode) {
				currentLabel = (LabelNode) insNode;
			}
			if (insNode instanceof MethodInsnNode) {
				MethodInsnNode mthdIns = (MethodInsnNode) insNode;
				String calledMthd = mthdIns.owner.replace("/", ".") + "." + mthdIns.name + mthdIns.desc;
				if (evoMthd.equals(calledMthd)) {
					callLabels.add(currentLabel);
				}
			}
		}
		ite = mn.instructions.iterator();
		currentLabel = null;
		if (callLabels.size() == 0) {// don't have accurate method,find same name method.
			while (ite.hasNext()) {
				AbstractInsnNode insNode = ite.next();
				if (insNode instanceof LabelNode) {
					currentLabel = (LabelNode) insNode;
				}
				if (insNode instanceof MethodInsnNode) {
					MethodInsnNode mthdIns = (MethodInsnNode) insNode;
					String calledMthd = mthdIns.name + mthdIns.desc;
					String evoMthdName = evoMthd.substring(evoMthd.lastIndexOf(".") + 1);
					if (evoMthdName.equals(calledMthd)) {
						callLabels.add(currentLabel);
					}
				}
			}
		}
		if (callLabels.size() == 0) {
			throw new Exception("don't have method " + evoMthd);
		}
		return callLabels;
	}

	private ExeLabelPaths getAllExePath(MethodNode mn) {
		ExeLabelPaths paths = new ExeLabelPaths();
		ListIterator<AbstractInsnNode> ite = mn.instructions.iterator();
		boolean hasAddFirst = false;
		LabelNode lastLabel = null;
		while (ite.hasNext()) {
			AbstractInsnNode insNode = ite.next();
			// System.out.println(insNode);
			if (insNode instanceof LabelNode) {// add sequence node.
				if (!hasAddFirst) {// first labelNode
					paths.addFirstNode((LabelNode) insNode);
					hasAddFirst = true;
				} else {
					if (lastLabel != null)
						paths.addNewNode(lastLabel, (LabelNode) insNode);
				}
				lastLabel = (LabelNode) insNode;
			}
			if (insNode instanceof JumpInsnNode) {
				JumpInsnNode jumpNode = ((JumpInsnNode) insNode);
				if (jumpNode.getOpcode() == Opcodes.GOTO) {
					paths.addNewNode(lastLabel, jumpNode.label);
					lastLabel = null;
				} else {
					paths.addNewBranchNode(lastLabel, jumpNode.label);
				}
			}
		}
		return paths;
	}

	private InputStream getErClassStream() throws ZipException, IOException {
		InputStream classInStream;
		String erCls = MthdFormatUtil.sootMthd2cls(erM);
		ZipFile zipFile = null;
		File modifiedClass = new File(ShellConfig.modifyCp + erCls.replace(".", File.separator) + ".class");
		if (modifiedClass.exists()) {// Other method in class was modified.
			classInStream = new FileInputStream(modifiedClass);
		} else if (erJarPath.endsWith(".jar")) {
			zipFile = new ZipFile(new File(erJarPath));
			ZipEntry entry = zipFile.getEntry(erCls.replace(".", "/") + ".class");
			classInStream = zipFile.getInputStream(entry);
		} else {
			classInStream = new FileInputStream(
					erJarPath + File.separator + erCls.replace(".", File.separator) + ".class");
		}
		return classInStream;
	}

	public static void main(String[] args) throws Exception {

		 System.out.println("erM:" + args[0]);
		 System.out.println("eeM:" + args[2]);
		 System.out.println("jarPath:" + args[1]);
		 new MethodModifier(args[0], args[1], args[2]).modifyMthd();

		// TODO test
//		asmTest();
	}

	private static void asmTest() throws Exception {
		FileUtil.delFolder(ShellConfig.modifyCp);
		new MethodModifier("<neu.lab.plug.testcase.asm.App: void <init>()>",
				"D:\\cWS\\eclipse1\\plug.testcase.asm\\target\\classes",
				"<java.lang.Object: java.lang.String toString()>").modifyMthd();
	}
}
