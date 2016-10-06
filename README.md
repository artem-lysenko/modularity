A (slightly modified) implementation of Dcut network clustering algorithm. The changes made aim to produce clusters in the desired size range. Build with Maven to produce the executable jar.

Reference: Graph Clustering with Density-Cut; Junming Shao, Qinli Yang, Jinhu Liu, Stefan Kramer; 2016; https://arxiv.org/abs/1606.00950

Arguments by position:
- Network file (no header, tab-separated, a pair of node ids followed by weight on each line.)
- Number of CPU threads to use.
- Maximum module size (hard limit --  all modules produced will be below this size)
- Minimum module size (soft limit -- some modules produced may be below this size as maximum module size takes priority).
- [optional] put in 'experimenal' word as argument in the last position for density tree to be cut at the highest density edges rather than the lowest ones.

Outputs: a .tree_file containing the generated density tree and .pascal file with modules in PASCAL tool format (each line starts with cluster id followed by cluster confidence (not used, so always set to 1.0) then followed by ids of cluster members).
