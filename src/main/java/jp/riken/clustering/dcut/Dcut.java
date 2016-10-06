package jp.riken.clustering.dcut;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jp.riken.clustering.misc.DefaultSuppliers;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

/**
 * 
 * @author Artem Lysenko
 * 
 *         A (slightly modified) implementation of Dcut network clustering
 *         algorithm. Modifications where made to produce clusters in the
 *         desired size range.
 * 
 *         Reference: Graph Clustering with Density-Cut; Junming Shao, Qinli
 *         Yang, Jinhu Liu, Stefan Kramer; 2016;
 *         https://arxiv.org/abs/1606.00950
 * 
 * 
 */
public class Dcut {

	public static void runDcut(String file, int threads, int maxModuleSize, int minModuleSize, boolean experimenatal) throws IOException {

		// Load the data
		SetMultimap<String, String> map = Multimaps.newSetMultimap(Maps.<String, Collection<String>> newHashMap(), DefaultSuppliers.<String> set());
		String base = file.replace(".tab", "");
		base = base.replace(".txt", "");
		Set<String> set = new HashSet<String>();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String line;
		double max = 0d;
		double min = 0d;
		while ((line = br.readLine()) != null) {
			String[] data = line.split("\t");
			set.add(data[0]);
			set.add(data[1]);
			map.put(data[0], data[1]);
			map.put(data[1], data[0]);
			map.put(data[0], data[0]);
			map.put(data[1], data[1]);

			if (!data[2].contains("Inf") && !data[2].contains("NaN")) {
				try {
					double v = Double.valueOf(data[2]);
					if (v > max) {
						max = v;
					}
					if (v < min) {
						min = v;
					}
				} catch (java.lang.NumberFormatException e) {
					System.out.println(data[2].contains("Inf") + " " + data[2]);
				}
			}
		}
		br.close();

		final double[][] array = new double[set.size()][set.size()];

		List<String> list = new ArrayList<String>(set);
		System.out.println("Nubmer of nodes: " + set.size() + " Maximum: " + max + " Minimum: " + min);
		Map<String, Integer> mapper = new HashMap<String, Integer>();
		Map<Integer, String> unmapper = new HashMap<Integer, String>();
		for (int i = 0; i < list.size(); i++) {
			mapper.put(list.get(i), i);
			unmapper.put(i, list.get(i));
		}
		list = null;

		br = new BufferedReader(new FileReader(file));
		while ((line = br.readLine()) != null) {
			String[] data = line.split("\t");
			double value = 1d;
			boolean isSet = false;
			if (data[2].contains("Inf")) {
				value = max;
				isSet = true;
			}
			if (data[2].contains("NaN") || data[2].contains("-Inf")) {
				value = min;
				isSet = true;
			}
			if (!isSet) {
				value = Double.valueOf(data[2]);
				if (value < 0.0) {
					value = Math.abs(value);
				}
			}
			double weight = getJaccard(map.get(data[0]), map.get(data[1])) * (value / max);
			int i = mapper.get(data[0]);
			int j = mapper.get(data[1]);
			array[i][j] = weight;
			array[j][i] = weight;
		}
		br.close();

		System.out.println(map.size());
		System.out.println("Finished loading the file");

		ArrayBlockingQueue<Pick> out = new ArrayBlockingQueue<Pick>(array.length);

		TIntArrayList processed = new TIntArrayList();
		TIntArrayList unprocessed = new TIntArrayList();

		for (int i = 1; i < unmapper.size(); i++) {
			unprocessed.add(i);
		}

		Map<Integer, TreeNode> id2node = new HashMap<Integer, TreeNode>();
		TreeNode root = new TreeNode(0, (double) minModuleSize);

		id2node.put(0, root);
		int a = 0;

		TIntArrayList toUpdate = new TIntArrayList();
		TDoubleArrayList processed_maxScore = new TDoubleArrayList();
		TIntArrayList processed_bestChild = new TIntArrayList();
		TIntIntHashMap processedPositions = new TIntIntHashMap();

		processedPositions.put(0, 0);
		processed.add(0);
		processed_maxScore.add(-1);
		processed_bestChild.add(-1);
		toUpdate.add(0);

		// Building the density tree
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		Stopwatch stopwatch = Stopwatch.createStarted();
		while (unprocessed.size() > 0) {
			final int N = toUpdate.size();
			int L = 1;
			if (N % threads == 0) {
				L = N / threads;
			} else {
				L = N / (threads - 1);
			}
			if (L == 0)
				L = 1;

			for (int i = 0; i < N; i += L) {
				executor.submit(new Picker(i, Math.min(N, i + L), array, unprocessed, toUpdate, out));
			}

			double best = -1d;
			int bestChild = -1;
			int bestParent = -1;

			for (int k = 0; k < processed_maxScore.size(); k++) {
				if (processed_maxScore.get(k) > best && !toUpdate.contains(processed.get(k))) {
					best = processed_maxScore.get(k);
					bestChild = processed_bestChild.get(k);
					bestParent = processed.get(k);
				}
			}

			int toCheck = toUpdate.size();

			while (toCheck > 0) {
				while (out.isEmpty()) {
					continue;
				}
				Pick p = out.poll();
				toCheck -= 1;
				if (p.score > best) {
					best = p.score;
					bestChild = p.child;
					bestParent = p.parent;
				}
				int index = processedPositions.get(p.parent);
				processed_maxScore.set(index, p.score);
				processed_bestChild.set(index, p.child);
			}
			toUpdate.clear();

			for (int k = 0; k < processed_bestChild.size(); k++) {
				if (processed_bestChild.get(k) == bestChild) {
					toUpdate.add(processed.get(k));
				}
			}

			TreeNode node = id2node.get(bestParent).grow(bestChild, best);
			unprocessed.remove(bestChild);

			processedPositions.put(bestChild, processed.size());
			processed.add(bestChild);
			processed_maxScore.add(-1);
			processed_bestChild.add(-1);
			toUpdate.add(bestChild);

			id2node.put(bestChild, node);

			a++;
			if (a > 999) {
				a = 0;
				System.out.println(unprocessed.size() + " nodes remaining to process");
				stopwatch = Stopwatch.createStarted();
			}
		}

		System.out.println("Tree construction  complete -- time taken: " + stopwatch.stop());
		executor.shutdown();

		// Save the tree graph
		BufferedWriter bw = new BufferedWriter(new FileWriter(base + ".tree_file"));
		for (TreeNode node : id2node.values()) {
			if (node.getParent() != null) {
				bw.write(unmapper.get(node.getId()) + "\t" + unmapper.get(node.getParent().getId()) + "\t" + node.getScore() + "\n");
			}
		}
		bw.flush();
		bw.close();

		for (Entry<Integer, TreeNode> ent : id2node.entrySet()) {
			ent.getValue().setExtId(unmapper.get(ent.getKey()));
		}

		TreeNode treeRoot = id2node.get(0);
		treeRoot.setFragment_size(treeRoot.getSize());
		List<List<String>> clusters = new ArrayList<List<String>>();
		Queue<TreeNode> q = new ArrayDeque<TreeNode>();
		q.add(treeRoot);
		while (!q.isEmpty()) {
			TreeNode n1 = q.poll();
			if (n1.fragment_size <= maxModuleSize) {
				clusters.add(n1.collectMemberIds());
				continue;
			} else {
				TreeNode n2;
				if (experimenatal) {
					n2 = n1._findCutPoint();
				} else {
					n2 = n1.findCutPoint();
				}
				n2.cut();
				q.add(n1);
				q.add(n2);
			}
		}

		// Save the clustering result
		bw = new BufferedWriter(new FileWriter(base + ".pascal"));
		for (int i = 0; i < clusters.size(); i++) {
			bw.write(String.valueOf(i + 1) + "\t1.0");
			for (String id : clusters.get(i)) {
				bw.write("\t");
				bw.write(id);
			}
			bw.write("\n");
		}
		System.out.println("Done.");
		bw.flush();
		bw.close();
	}

	private static final class TreeNode {
		private final int id;
		private TreeNode parent = null;
		private final Set<TreeNode> children = new HashSet<TreeNode>();
		private double size = 0d;
		private double d = Double.MAX_VALUE;
		private double fragment_size = 0d;
		private String extId;
		private final double minModuleSize;

		public TreeNode(int id, double minSize) {
			this.id = id;
			this.size = 1d;
			this.minModuleSize = minSize;
		}

		public TreeNode(int id, TreeNode parent, double d, int size, double minSize) {
			this.id = id;
			this.parent = parent;
			this.d = d;
			this.size = size;
			this.minModuleSize = minSize;
		}

		public List<String> collectMemberIds() {
			List<String> result = new ArrayList<String>();
			Queue<TreeNode> q = new ArrayDeque<TreeNode>();
			q.add(this);
			while (!q.isEmpty()) {
				TreeNode n = q.poll();
				result.add(n.extId);
				q.addAll(n.children);
			}
			return result;
		}

		public double getSize() {
			return size;
		}

		public double getD() {
			return d;
		}

		public double getFragment_size() {
			return fragment_size;
		}

		public void setFragment_size(double fragment_size) {
			this.fragment_size = fragment_size;
		}

		public String getExtId() {
			return extId;
		}

		public void setExtId(String extId) {
			this.extId = extId;
		}

		public TreeNode getParent() {
			return parent;
		}

		public double getScore() {
			return d;
		}

		public int getId() {
			return this.id;
		}

		public TreeNode grow(int newNodeId, double d) {
			this.size += 1d;
			if (parent != null) {
				parent.updateSize(1);
			}
			TreeNode c = new TreeNode(newNodeId, this, d, 1, this.minModuleSize);
			children.add(c);
			return c;
		}

		public double updateSize(double change) {
			this.size += change;
			if (parent != null) {
				return parent.updateSize(change);
			} else {
				fragment_size += change;
				return this.size;
			}
		}

		public TreeNode findCutPoint() {
			List<TreeNode> _children = new ArrayList<TreeNode>();
			TDoubleArrayList scores = new TDoubleArrayList();
			Queue<TreeNode> q = new ArrayDeque<TreeNode>();
			q.add(this);
			while (!q.isEmpty()) {
				TreeNode c = q.poll();
				q.addAll(c.children);
				TreeNode p = c.getParent();
				if (p == null) {
					continue;
				}
				double aboveSize = fragment_size - c.size;
				if (aboveSize >= 3d || c.size >= 3d) {
					_children.add(c);
					scores.add(c.d / Math.min(aboveSize, c.size));
				}
			}
			TreeNode minc = null;
			double minScore = Double.MAX_VALUE;
			for (int i = 0; i < scores.size(); i++) {
				if (scores.get(i) < minScore) {
					minScore = scores.get(i);
					minc = _children.get(i);
				}
			}
			return minc;
		}

		public TreeNode _findCutPoint() {
			List<TreeNode> _children = new ArrayList<TreeNode>();
			TDoubleArrayList scores = new TDoubleArrayList();
			Queue<TreeNode> q = new ArrayDeque<TreeNode>();
			q.add(this);
			while (!q.isEmpty()) {
				TreeNode c = q.poll();
				q.addAll(c.children);
				TreeNode p = c.getParent();
				if (p == null) {
					continue;
				}
				double aboveSize = fragment_size - c.size;
				if (aboveSize >= minModuleSize || c.size >= minModuleSize) {
					_children.add(c);
					scores.add(c.d / Math.max(aboveSize, c.size));
				}
			}
			TreeNode maxc = null;
			double maxScore = Double.MIN_VALUE;
			for (int i = 0; i < scores.size(); i++) {
				if (scores.get(i) > maxScore) {
					maxScore = scores.get(i);
					maxc = _children.get(i);
				}
			}
			return maxc;
		}

		public void cut() {
			this.fragment_size = size;
			this.parent.updateSize(-this.size);
			this.parent.children.remove(this);
			this.parent = null;
			this.d = Double.MAX_VALUE;
		}
	}

	private static final class Pick {
		private final double score;
		private final int child;
		private final int parent;

		public Pick(double score, int child, int parent) {
			this.score = score;
			this.child = child;
			this.parent = parent;
		}
	}

	private static final class Picker implements Runnable {
		private final int from;
		private final int to;
		private final double[][] array;
		private final TIntArrayList unprocessed;
		private final TIntArrayList processed;
		private final Queue<Pick> out;

		public Picker(int from, int to, double[][] array, TIntArrayList unprocessed, TIntArrayList processed, Queue<Pick> out) {
			super();
			this.from = from;
			this.to = to;
			this.array = array;
			this.unprocessed = unprocessed;
			this.processed = processed;
			this.out = out;
		}

		public void run() {
			for (int i = from; i < to; i++) {
				double max = -1d;
				int childe = -1;
				int parent = -1;
				int val = processed.get(i);
				for (int j = 0; j < unprocessed.size(); j++) {
					int child = unprocessed.get(j);
					if (array[val][child] > max) {
						max = array[val][child];
						childe = child;
						parent = val;
					}
				}
				out.add(new Pick(max, childe, parent));
			}
		}
	}

	private static double getJaccard(Collection<String> as, Collection<String> bs) {
		double intersection = 0d;
		for (String a : as) {
			if (bs.contains(a)) {
				intersection += 1d;
			}
		}
		double total = (double) (as.size() + bs.size());
		return (intersection) / (total - intersection);
	}
}
