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

def loadCellCounts(simFname):
    cellCounts  = []
    NumMagnetsN  = []
    NumMagnetsR = []
    with open(simFname, 'rb') as f:
        dataDict = csv.DictReader(f, delimiter=',')
        for record in dataDict:
            cellCounts.append(float(record['R-avgCellSize']))
            NumMagnetsN.append(float(record['NumMagnetsN']))
            NumMagnetsR.append(float(record['NumMagnetsR']))
    cellCounts = np.log(np.array(cellCounts))
    NumMagnetsN = np.log(1.+np.array(NumMagnetsN))
    NumMagnetsR = np.log(1.+np.array(NumMagnetsR))

    return {'cellCounts':cellCounts, 'NumMagnetsN':NumMagnetsN, 'NumMagnetsR':NumMagnetsR}

sim_data = loadCellCounts(simFname='../batchDossier_t=2011_02_16__transitivity.csv')

pylab.figure()
pylab.scatter(sim_data['NumMagnetsN'], sim_data['cellCounts'])

cc = np.corrcoef(sim_data['NumMagnetsN'], sim_data['cellCounts'])[0,1]
print 'correlation: '+str(cc)

#pylab.legend(loc='best')
pylab.xlabel('Log (Radical Magnets + 1)')
pylab.ylabel('Log Cell Size')
pylab.text(0.1, -2., 'Correlation: %.2f'%cc, fontsize=20)
pylab.savefig('output/cellsSize_on_magnets_'+timeNow()+'.png')
