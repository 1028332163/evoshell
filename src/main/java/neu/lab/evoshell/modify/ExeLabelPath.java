package neu.lab.evoshell.modify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.LabelNode;

public class ExeLabelPath {
	private List<LabelNode> nodes;
	private LabelNode lastNode;

	public ExeLabelPath() {
		nodes = new ArrayList<LabelNode>();
	}

	public ExeLabelPath(LabelNode firstNode) {
		nodes = new ArrayList<LabelNode>();
		this.addNode(firstNode);
	}

	public LabelNode getLast() {
		return lastNode;
	}

	public boolean contains(LabelNode node) {
		return nodes.contains(node);
	}

	public void addNode(LabelNode newNode) {
		nodes.add(newNode);
		lastNode = newNode;
	}

	@Override
	public ExeLabelPath clone() {
		ExeLabelPath clone = new ExeLabelPath();
		for (LabelNode node : nodes) {
			clone.addNode(node);
		}
		return clone;
	}

	public String getPathStr(Map<LabelNode, Integer> label2num) {
		StringBuilder sb = new StringBuilder();
		for (LabelNode node : this.nodes) {
			sb.append(label2num.get(node) + "->");
		}
		return sb.toString();
	}

	private int getLabelPos(LabelNode labelNode) {
		int pos = -1;
		for (int i = 0; i < this.nodes.size(); i++) {
			if (labelNode == this.nodes.get(i)) {
				pos = i;
				break;
			}
		}
		return pos;
	}

	public int getLabelPos(List<LabelNode> labels) {
		int pos = -1;
		for (LabelNode label : labels) {
			int newPos = getLabelPos(label);
			if (newPos != -1) {
				if (pos == -1 || newPos < pos) {
					pos = newPos;
				}
			}
		}
		return pos;
	}
}
