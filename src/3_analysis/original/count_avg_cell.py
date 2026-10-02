#produces a comparison of the cells sizes, showing empirical data and simulation results

import sys, os
import matplotlib
#for running scripts silently:
#if 'matplotlib.backends' not in sys.modules: matplotlib.use('PS')
if 'matplotlib.backends' not in sys.modules: matplotlib.use('pdf')
from matplotlib import rc
#rc('font',**{'family':'sans-serif','sans-serif':['Helvetica']})
## for Palatino and other serif fonts use:
#rc('font',**{'family':'serif','serif':['Palatino']})
rc('font',**{'family':'serif','serif':['Times New Roman']})
#rc('font',**{'family':'serif','serif':['FreeSerif']})
rc('text', usetex=True)
#http://www.scipy.org/Cookbook/Matplotlib/UsingTex

#import matplotlib.pylab as pylab
import pylab

import numpy as np
import numpy.random as npr
import csv, time
import pdb
import scipy.stats as sps

timeNow = lambda : time.strftime('%Y_%m_%d__%H_%M_%S', time.localtime())


#the indices of this array are individuals, and the values are the cellnumbers
def cellCounts(cellIndivs):
    #counts cell sizes assuming the array lists the cell of each person, sorted by cell number
    if cellIndivs == []:
        return np.array([0])
    counts = []
    cellSize = 0
    lastCellNum = cellIndivs[0]
    for cellNum in cellIndivs:
        if cellNum == lastCellNum:
            cellSize += 1
        else:
            counts.append(cellSize)
            lastCellNum = cellNum
            cellSize = 1
    counts.append(cellSize)
    return np.array(counts)
assert cellCounts([]) == np.array([0])
assert (cellCounts([4,4,6,17,17,17]) == np.array([2,1,3])).all()

def hellinger(p,q):
#p and q should be binned as [0,0.5),[0.5,1.5),...
    d1 = np.array(p)
    d2 = np.array(q)
    if len(d1) < len(d2):
        d1.resize(len(d1) + (len(d2)-len(d1)))
    else:
        d2.resize(len(d2) + (len(d1)-len(d2)))

    hell = np.sqrt(d1) - np.sqrt(d2)
    hell = np.square(hell)
    hell.sort()

    return 0.5*reduce(lambda x,y: x+y, hell, 0.)  #underflow control
#assert hellinger([0.9,0.1,0,0],[0,0,0.5,0.5]) == 1.0


def loadCellCounts(empFname, simFname):
    cellIndivsEmp = {'all':[]}
    with open(empFname, 'rb') as f:
        dataDict = csv.DictReader(f, delimiter=',')
        for record in dataDict:
            cellID   = int(record['Cell_Name_ID'])
            movements= record['Movement'].strip()
            if cellID == -1: 
                continue
            cellIndivsEmp['all'].append(cellID)
            for movement in movements.split('/'):
                if movement not in cellIndivsEmp:
                    cellIndivsEmp[movement] = []
                cellIndivsEmp[movement].append(cellID)

    cellCountsSim = []
    with open(simFname, 'rb') as f:
        dataDict = csv.DictReader(f, delimiter=',')
        for record in dataDict:
            cellCountsSim.append(float(record['R-avgCellSize']))
    cellCountsSim = np.array(cellCountsSim)

    return cellIndivsEmp, cellCountsSim

def plotOutlinedBars(bins, dataSets, plotName):
#draws lines that outcome 
    #xFromBins = lambda bins: bins[:-1] + (bins[1]-bins[0])/2.
    dblMiddle = lambda ar: reduce(lambda x,y:x+[y,y], ar[1:-1], [ar[0]])+[ar[-1]]
    dblAll    = lambda ar: reduce(lambda x,y:x+[y,y], ar, [])
    xFromBins = lambda bins:  np.array(dblMiddle(bins))
    yFromHs   = lambda hvals: np.array(dblAll(hvals))

    pylab.figure()
    pylab.hold(True)
    for label in dataSets:
        pylab.plot(xFromBins(bins), yFromHs(dataSets[label]), 'r-', linewidth=1., label=label, alpha=1.0)
        pylab.plot(xFromBins(bins), yFromHs(dataSets[label]), 'b-', linewidth=2., label=label, alpha=1.0)

    pylab.legend(loc='best')
    pylab.xlabel('Cell Size')
    pylab.ylabel('Probability')
    pylab.savefig('output/'+plotName+timeNow())


def plotOverlayedBars(bins, dataSets, plotName):
    pylab.figure()
    pylab.hold(True)
    hatches = [r'\\', r'o', '/', '-', 'x', ]
    colors  = ['b', 'g', 'r', 'c', 'y']
    for i,label in enumerate(dataSets):
        pylab.hist(bins=bins, x=dataSets[label], color=colors[i], hatch=hatches[i], linewidth=1., label=label, alpha=1.0, normed=True)
    pylab.legend(loc='best')
    pylab.xlabel('Cell Size')
    pylab.ylabel('Probability')
    pylab.savefig('output/'+plotName+timeNow())

def plotParallelBars(bins, dataSets, plotName):
    pylab.figure()
    pylab.hold(True)
    colors  = ['b', 'r', 'g', 'c', 'y']
    hatches = [r'\\', r'/', 'o', '-', 'x', ]
    whiteSpace = 0.3
    widths = (1.0-whiteSpace)*(bins[1:]-bins[:-1])/len(dataSets)
    leftEndShift = widths/2.
    for i,label in enumerate(dataSets):
        pylab.bar(left=bins[:-1] + leftEndShift+ i*widths, width=widths, 
                  height=dataSets[label], color=colors[i], label=label, hatch=hatches[i], alpha=1.0)
    pylab.xlim(bins[0],bins[-1])
    pylab.xticks(range(1,int(np.ceil(bins[-1]))))
    pylab.legend(loc='best')
    pylab.xlabel('Cell Size', fontsize=20)
    pylab.ylabel('Probability', fontsize=20)
    fname='output/'+plotName+timeNow()+'.pdf'
    pylab.savefig(fname)
    #subprocess.call('dir')
    os.system('convert -density 300 '+fname+' '+fname[:-5]+'.png')
    #http://www.imagemagick.org/script/command-line-options.php
    pylab.hold(False)



cellIndivsEmp, cellCountsSim = loadCellCounts(empFname='terroristcelllist4.19.csv', simFname='../batchDossier_t=2011_02_16__transitivity.csv')

thresHs  = lambda hVals, th: np.select(condlist=[np.array(hVals)<th], choicelist=[hVals], default=th)  

printForR = lambda lst: reduce(lambda x,y:str(x)+','+str(y), lst)

topLimit = 14.5
cellCountsSim.sort()
cellCountsSim = thresHs(cellCountsSim, topLimit)
#binsForSims = np.array([0.5]+range(2,int(topLimit+0.5)))-0.5 #0,1.5,2.5,....,toplimit
binsForSims = np.array(range(1,int(topLimit+0.5)))-0.5 #0,1.5,2.5,....,toplimit

hSimAll,binsSimAll = np.histogram(cellCountsSim,                    bins=binsForSims, normed=True)
hEmpAll,binsEmpAll = np.histogram(cellCounts(cellIndivsEmp['all']), bins=binsForSims, normed=True)

print 'Hellinger sim-vs-emp all groups: %.2f'%hellinger(hSimAll, hEmpAll)
print 'Kolmogorov-Smirnov sim-vs-emp all groups: p=%.2f'%sps.ks_2samp(hSimAll, hEmpAll)[1]
print 'Mann-Whitney sim-vs-emp all groups: p=%.2f'%sps.mannwhitneyu(hSimAll, hEmpAll)[1]
#print printForR(cellCountsSim)
#print printForR(cellCounts(cellIndivsEmp['all']))

dataSets = {'Simulated': hSimAll, 'Empirical': hEmpAll}
plotParallelBars(bins=binsForSims, dataSets=dataSets, plotName='sim_vs_emp')

binsForEmps = np.array(range(1,int(topLimit+0.5)))-0.5 #0.5,1.5,....,toplimit
moveHs   = {}
movements = ['Environmentalist', 'Right-Wing', 'Islamist']
for movement in movements:
    moveHs[movement],dummy = np.histogram(cellCounts(cellIndivsEmp[movement]), bins=binsForEmps, normed=True)

plotParallelBars(bins=binsForEmps, dataSets=moveHs, plotName='movements')

for i,movement1 in enumerate(movements):
    cells1 = cellCounts(cellIndivsEmp[movement1])
    print 'Average cell size for %s: %.2f'%(movement1,np.average(cells1))
    for j,movement2 in enumerate(movements):
        if j <= i: continue
        cells2 = cellCounts(cellIndivsEmp[movement2])
        print 'Kolmogorov-Smirnov for %s,%s groups: p=%.3f'%(movement1,movement2,sps.ks_2samp(cells1,cells2)[1])
        print 'Mann-Whitney for %s,%s groups: p=%.3f'%(movement1,movement2,sps.mannwhitneyu(cells1,cells2)[1])
        print
        #print 'c(%s)'%printForR(cells1)
        #print 'c(%s)'%printForR(cells2)
pylab.show()

'''
binning of the cell sizes can effect both the Heilinger and the KS results;  therefore
TODO:
    1. sps.ks_2samp  should be applied to raw counts.  the function CAN be applied to arrays of different sizes 
    2. use kernel density estimate to estimate the distributions, then compute Heilinger

    see: scipy.stats.gaussian_kdescipy.stats.gaussian_kde


R code:
RightW <- c(1,1,1,1,2,1,1,2,1,3,2,1,1,1,1,4,1,3,2,2,1,3,1,2,2,1,1,1,2,1,1,1,1,1,1,1,2,1,1,1,1)
Env <- c(3,2,3,4,1,13,2,4,4,3,1,3,1,1,3)
Isl <- c(5,8,1,2,3,1,1,2,1,1,1,1,1,1,1,4,7,1,1,1,2,1,1,1,1,1,1,7,1,2,1,7,4,1,6,4,1,3,3,2,4,7,2,8,4,1,2,1,1,1,2)
library(adk);

> adk.test(list(RightW,Isl))
Anderson-Darling k-sample test.

Number of samples:  2
Sample sizes: 41 51
Total number of values: 92
Number of unique values: 8

Mean of Anderson Darling Criterion: 1
Standard deviation of Anderson Darling Criterion: 0.74609

T = (Anderson Darling Criterion - mean)/sigma

Null Hypothesis: All samples come from a common population.

                    t.obs P-value extrapolation
not adj. for ties 3.68447 0.01066             0
adj. for ties     3.61101 0.01132             0



> adk.test(list(RightW,Env))
Anderson-Darling k-sample test.

Number of samples:  2
Sample sizes: 41 15
Total number of values: 56
Number of unique values: 5

Mean of Anderson Darling Criterion: 1
Standard deviation of Anderson Darling Criterion: 0.73752

T = (Anderson Darling Criterion - mean)/sigma

Null Hypothesis: All samples come from a common population.

                     t.obs P-value extrapolation
not adj. for ties 10.61190   4e-05             1
adj. for ties     11.82475   2e-05             1



> adk.test(list(Isl,Env))
Anderson-Darling k-sample test.

Number of samples:  2
Sample sizes: 51 15
Total number of values: 66
Number of unique values: 9

Mean of Anderson Darling Criterion: 1
Standard deviation of Anderson Darling Criterion: 0.74148

T = (Anderson Darling Criterion - mean)/sigma

Null Hypothesis: All samples come from a common population.

                    t.obs P-value extrapolation
not adj. for ties 1.81863 0.05721             0
adj. for ties     1.87880 0.05394             0

'''
