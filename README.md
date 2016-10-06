# modularity

A (slightly modified) implementation of Dcut network clustering algorithm. Modifications where made to produce the clusters in the desired size range.
Reference: Graph Clustering with Density-Cut; Junming Shao, Qinli Yang, Jinhu Liu, Stefan Kramer; 2016; https://arxiv.org/abs/1606.00950

Arguments by position:
 (1)Network file (no header, tab-separated, a pair of node ids followed by weight on each line.)
 (2)Number of CPU threads to use.
 (3)Maximum module size (hard limit --  all modules produced will be below this size)
 (4)Minimum module size (soft limit -- some modules produced may be below this size as maximum module size takes priority).
 (5)[optional] put in 'experimenatl' word as argument in the last position for density tree to be cut at the highest density edges rather than the lowest ones.
Outputs: a .tree_file containing the generated density tree and .pascal file with moudles in PASCAL tool format (each line starts with cluster id followed by cluster confidence number (not used, so always set to 1.0) then followed by ids of cluster members).
