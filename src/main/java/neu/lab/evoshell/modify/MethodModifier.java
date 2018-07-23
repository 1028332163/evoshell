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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

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

		ClassNode classNode = readErClassNode();
		String evoErM = MthdFormatUtil.soot2evo(erM);
		boolean findMthd2modify = false;
		for (MethodNode mn : classNode.methods) {
			String evoMthd = classNode.name.replace("/", ".") + "." + mn.name + mn.desc;
			if (evoErM.equals(evoMthd)) {
				findMthd2modify = true;
				
				// filter node
				deleteBranch(mn);
				mn.tryCatchBlocks = null;
			}
		}
		if (!findMthd2modify) {
			throw new Exception("can't find " + erM);
		}

		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		classNode.accept(cw);
		byte[] b = cw.toByteArray();
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(getOutFilePath()));
		out.write(b);
		out.close();
		//TODO writeHalfByte
//		writeModifiedByteCode();
	}
	
	private void writeModifiedByteCode() {
		try {
			ClassReader cr = new ClassReader(new FileInputStream(getOutFilePath()));

			PrintWriter p1 = new PrintWriter(new FileWriter("d:\\cWs\\notepad++\\out.txt", false));
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
//		ExeLabelPath remianPath = getAllExePath(mn).getRemainPath(getCallLabels(mn, MthdFormatUtil.soot2evo(eeM)));
		
		ListIterator<AbstractInsnNode> ite = mn.instructions.iterator();
		LabelNode currentLabel = null;
		Set<AbstractInsnNode> remainLabels = new HashSet<AbstractInsnNode>();
		while (ite.hasNext()) {
			AbstractInsnNode insNode = ite.next();
			System.out.println(insNode+"->"+insNode.getNext());
			if (insNode instanceof LabelNode) {
				currentLabel = (LabelNode) insNode;
			}
			if (insNode instanceof JumpInsnNode) {
				ite.remove();
			} 
//			//TODO remove else-body
//			else if (!remianPath.contains(currentLabel)) {
//				ite.remove();
//			}
//			//TODO for debug
//			else {
//				remainLabels.add(insNode);
//			}
		}
		System.out.println("======");
//		//reset next.
//		 ite = mn.instructions.iterator();
//		 while (ite.hasNext()) {
//				AbstractInsnNode insNode = ite.next();
//				if(!remainLabels.contains(insNode.getNext())) {
//					System.out.println(insNode+"->"+insNode.getNext());
//				}
//			}
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

	private ClassNode readErClassNode() throws ZipException, IOException {
		InputStream classInStream;
		String erCls = MthdFormatUtil.sootMthd2cls(erM);
		ZipFile zipFile = null;
		File modifiedClass = new File(ShellConfig.modifyCp+erCls.replace(".", File.separator) + ".class");
		if(modifiedClass.exists()) {//Other method in class was modified.
			classInStream = new FileInputStream(modifiedClass);
		}else
		if (erJarPath.endsWith(".jar")) {
			zipFile = new ZipFile(new File(erJarPath));
			ZipEntry entry = zipFile.getEntry(erCls.replace(".", "/") + ".class");
			classInStream = zipFile.getInputStream(entry);
		} else {
			classInStream = new FileInputStream(
					erJarPath + File.separator + erCls.replace(".", File.separator) + ".class");
		}
		ClassReader cr = new ClassReader(classInStream);
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		if (zipFile != null) {
			zipFile.close();
		}
		return cn;
	}

	public static void main(String[] args) throws Exception {
//		args = new String[3];
//		args[0] = "<io.swagger.jaxrs.Reader: void readSwaggerConfig(java.lang.Class,io.swagger.annotations.SwaggerDefinition)>";
//		args[1] = 
		// String erM = "<neu.lab.testcase.top.MthdTop: void
		// m1(java.lang.String,java.lang.Integer)>";
		// String erJarPath ="D:\\cWS\\eclipse1\\testcase.top\\target\\classes";
		// String eeM = "<neu.lab.testcase.middle.MthdMiddle: void m1(int)>";
		System.out.println("erM:" + args[0]);
		System.out.println("eeM:" + args[2]);
		System.out.println("jarPath:" + args[1]);
		new MethodModifier(args[0], args[1], args[2]).modifyMthd();
	}
}
