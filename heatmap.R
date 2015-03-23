#!/usr/bin/env Rscript
library(gplots)
args <- commandArgs(trailingOnly = T)
ESS <- data.matrix(read.csv(args[1]))
print(max(ESS))
heatmap.2(ESS, col = cm.colors(1024), Rowv = NA, Colv = NA)
