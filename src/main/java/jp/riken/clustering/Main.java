package jp.riken.clustering;

import java.io.IOException;

import jp.riken.clustering.dcut.Dcut;

/**
 * 
 * @author Artem Lysenko
 * 
 */
public class Main {

	public static final void main(String[] args) {
		boolean displayUsage = false;

		int threads = 0;
		int maxModuleSize = 0;
		int minModuleSize = 0;
		String file = "";
		boolean experimenatal = false;
		
		if (args.length < 4) {
			displayUsage = true;
		}
		else{
			file = args[0];
			try {
				threads = Integer.valueOf(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("Error -- second argument should be a number.");
			}
			try {
				maxModuleSize = Integer.valueOf(args[2]);
			} catch (NumberFormatException e) {
				System.err.println("Error -- third argument should be a number.");
			}
			try {
				minModuleSize = Integer.valueOf(args[3]);
			} catch (NumberFormatException e) {
				System.err.println("Error -- fourth argument should be a number.");
			}

			
			if (args.length > 4 && args[4].equalsIgnoreCase("experimental")) {
				experimenatal = true;
			}	
		}

		if (displayUsage) {
			System.out.println("A (slightly modified) implementation of Dcut network clustering algorithm. Modifications where made to produce the clusters in the desired size range.");
			System.out.println("Reference: Graph Clustering with Density-Cut; Junming Shao, Qinli Yang, Jinhu Liu, Stefan Kramer; 2016; https://arxiv.org/abs/1606.00950");
			System.out.println("Arguments by position:");
			System.out.println("\t(1)Network file (no header, tab-separated, a pair of node ids followed by weight on each line.)");
			System.out.println("\t(2)Number of CPU threads to use.");
			System.out.println("\t(3)Maximum module size (hard limit --  all modules produced will be below this size)");
			System.out.println("\t(4)Minimum module size (soft limit -- some modules produced may be below this size as maximum module size takes priority).");
			System.out.println("\t(5)[optional] put in 'experimenatl' word as argument in the last position for density tree to be cut at the highest density edges rather than the lowest ones.");
			System.out.println("Outputs: a .tree_file containing the generated density tree and .pascal file with moudles in PASCAL tool format (each line starts with cluster id followed by cluster confidence number (not used, so always set to 1.0) then followed by ids of cluster members).");
			return;
		} else {
			try {
				Dcut.runDcut(file, threads, maxModuleSize, minModuleSize, experimenatal);
			} catch (IOException e) {
				System.err.println("Error -- could not load file: " + file);
			}
		}
	}
}
