"""
Plot the data in the .pkl files


Usage help
----------

python plot_elasticity.py -h

Release Notes
-------------


""" 
#    Copyright (C) 2007 by  
#    Sasha Gutfraind 
#    Distributed under the terms of the GNU Lesser General Public License 
#    http://www.gnu.org/copyleft/lesser.html 
# 
import sys

import numpy as np
import matplotlib
#for running scripts silently:
#matplotlib.use('PS')
#import matplotlib.pylab as pylab
import pylab
import scipy
import re, os, sys, pickle, time

def usage():
    print("Script for running the ABM and analyzing the data")
    print("Allowed options are:")
    print("[-d  <Dir>]         Perform analysis on Dir in which subdirectories contain") 
    print("                    pickled simulation reports.")
    print("-h                  Displays this")
    print("eg.")
    print(r"To run and analyze a batch file")
    print(r"python abmdriver.py -b analysis1.pf")
    print()
    print(r"To analyze without running the batch file(data generated in the past)")
    print(r"python abmdriver.py -b analysis1.pf -m a")


def initialize():
    import getopt
    opts, args = getopt.getopt(sys.argv[1:], "d:h", [""])

    batchFile = ''
    dataDir   = ''
    lhsMode   = False
    
    for o, a in opts:
       if o in ("-h"):
          usage()
          sys.exit(0)
       if o in ("-d"):
          dataDir = a
    
    if dataDir == '':
       raise IOError('Cannot run without output directory. Use -d option')
    if not os.path.isabs(dataDir):
       dataDir = os.getcwd()+os.sep+dataDir

    return dataDir 


def loadDossiers(dataDir):
#loads all .pkl from the dataDir and its direct subdirectories
    def getAllPickles(dir, dossiers):
        names = os.listdir(dir)
        for name in names:
            if not name[-4:] == '.pkl':
                continue
            #potentially multiple pickled data files in the same dir
            dossier = pickle.load(open(dir+os.sep+name, 'rb'))
            dossiers.append(dossier)
        return dossiers

    dossiers = getAllPickles(dataDir, [])
    
    dirList = os.listdir(dataDir)
    dirs    = []
    for path in dirList:
        pathAbs = dataDir+os.sep+path
        if os.path.isdir(pathAbs): 
            dossiers = getAllPickles(pathAbs, dossiers)

    return dossiers


def displayStats(dataDir, dossiers):
    numDossiers   = len(dossiers)
    #valuableStats = ['Density', 'Isolation', 'radicalRawDyads', 'Clustering', 'radicalRawTriads']
    valuableStats = ['Density', 'Isolation', 'Clustering']
    statColors    = dict(list(zip(valuableStats, ['r', 'g', 'b'])))
    numValStats   = len(valuableStats)
    colWidth      = 1./1.1/len(valuableStats) 

    xlocs         = np.arange(numDossiers, dtype=np.double) 
    #modify this to customize: dict[paramname] = loc
    xlocs_dict    = {}
    for loc, dossier in enumerate(dossiers):
        paramName = dossier['paramName']
        xlocs_dict[paramName] = loc
    yvals   = {}
    prtexts = {}
    for statName in valuableStats:
        vals  = np.repeat(0., numDossiers)
        texts = ['']*numDossiers
        for dossier in dossiers:
            paramName    = dossier['paramName']
            elasticities = dossier['elasticities'] #dict (val,pr_f) keyed by statName 
            report       = dossier['data']
            el, pr_f     = elasticities[statName]
            vals[xlocs_dict[paramName]] = el

            if pr_f < 0.001:
                pr_txt = '***'
            elif pr_f < 0.01:
                pr_txt = '**'
            elif pr_f < 0.05:
                pr_txt = '*'
            else:
                pr_txt = ''
            texts[xlocs_dict[paramName]] = pr_txt
        yvals[statName]   = vals
        prtexts[statName] = texts

    barPlots = []
    for i, statName in enumerate(valuableStats):
        #if pr_f < 0.05:
        #    linestyle = 'solid'
        #else:
        #    linestyle = 'dashed'
        offset = colWidth*(i - ((numValStats-1)/2 + (numValStats%2)/2))
        barPlot = pylab.bar(xlocs+offset, yvals[statName], colWidth, color=statColors[statName], alpha=0.5)
        for j,xloc in enumerate(xlocs):
            yloc = yvals[statName][j]
            pylab.text(xloc+offset+colWidth*0.5, yloc + 0.01*np.sign(yloc), prtexts[statName][j], rotation=90)

        barPlots.append(barPlot)

    sortedTicks = list(xlocs_dict.items())
    sortedTicks.sort(key=lambda x:x[1])
    pylab.xticks(xlocs, [x[0] for x in sortedTicks], rotation=10)
    pylab.xlim(-2*colWidth, numDossiers-1+3*colWidth)
    pylab.ylim(-1.05,1.05)
    pylab.ylabel('Elasticity')

    pylab.legend( [x[0] for x in barPlots], valuableStats, shadow=True)
    
    pylab.savefig(dataDir + os.sep + 'bar ' + str(time.localtime()[3:-1]) + '.eps')
    #pylab.savefig(dataDir + os.sep + 'bar ' + str(time.localtime()[3:-1]) + '.png')
    pylab.show()
        
    print('Done')

if __name__ == "__main__":
    dataDir = initialize()

    dossiers = loadDossiers(dataDir)

    displayStats(dataDir, dossiers)

