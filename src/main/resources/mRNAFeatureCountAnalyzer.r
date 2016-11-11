source("http://bioconductor.org/biocLite.R")
biocLite("DESeq2")
library(DESeq2)

currentLoc <- commandArgs(trailingOnly = TRUE)
contList <- read.table(file.path(as.character(currentLoc[1]), "src/main/resources/temp", as.character(currentLoc[2]), "con.temp"))
expList <- read.table(file.path(as.character(currentLoc[1]), "src/main/resources/temp", as.character(currentLoc[2]), "exp.temp"))


for (i in 1:ncol(contList)) {
	contList[,i] <- as.character(contList[,i])
}
for (i in 1:ncol(expList)) {
	expList[,i] <- as.character(expList[,i])
}

temp <- read.delim(contList[1,1], header = F)
countData = c(nrow=nrow(temp), ncol=0)
condition = c()
type = c()

for (i in expList[,1]) {
	temp <- read.delim(i, header=F, row.names=1)
	countData = cbind(countData, temp)
	condition = cbind(condition, "experiment")
	type = cbind(type, "single")
}

for (i in contList[,1]) {
	temp <- read.delim(i, header=F, row.names=1)
	countData = cbind(countData, temp)
	condition = cbind(condition, "control")
	type = cbind(type, "single")
}
anal = countData[-(1), -(1)]
colData <- data.frame(row.names=colnames(anal), condition[1,], type[1,])
for (i in 1:ncol(anal)) {
	anal[,i] <- as.numeric(anal[,i])
}
colData$condition <- relevel(colData$condition, ref="control")
dds <- DESeqDataSetFromMatrix(countData=anal, colData=colData, design=~condition)


dds <- DESeq(dds)
res <- results(dds)


## Order by smallest adjusted p-value
resOrdered <- res[order(res$pvalue),]

## Write to .csv
write.csv(as.data.frame(resOrdered), file=file.path(as.character(currentLoc[1]), "src/main/resources/temp", as.character(currentLoc[2]), "anal.csv"))