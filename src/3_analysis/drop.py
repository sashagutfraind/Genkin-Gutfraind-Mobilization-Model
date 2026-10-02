''' 
#    Copyright (C) 2010 by  
#    Sasha Gutfraind 
#    Distributed under the terms of the GNU Lesser General Public License 
#    http://www.gnu.org/copyleft/lesser.html 
# 
'''

import networkx as nx
import numpy as np
import pylab
import numpy.random as npr
import csv  #http://docs.python.org/library/csv.html
import sys
import re

def loadData():
    ret = {'ticNames':[], 'snames':('medCell','assort','clust'), 'series':[]}
    ret['series'] = {'medCell':[],'assort':[],'clust':[]}

    #badList = (,)
    with open('data.csv', 'r', newline='') as f:
        dataDict = csv.DictReader(f)
        for record in dataDict:       
            xname = record['xname']
            medCell  = float(record['medCell'])
            assort  = float(record['assort'])
            clust = float(record['clust'])
        
            ret['ticNames'].append(xname)
            ret['series']['medCell'].append(medCell)
            ret['series']['assort'].append(assort)
            ret['series']['clust'].append(clust)

    return ret

def plotDropline(data):
    #from matplotlib.ticker import MultipleLocator, FormatStrFormatter
    #nullForm = FormatStrFormatter('')

    ticNames=data['ticNames']
    tics    = np.arange(len(ticNames))
    sNames  = data['snames']
    series  = data['series']

    groupsep = 0.05
    colors = ['b', 'g', 'r']
    offsets = np.arange(3)*groupsep
    markers = ['s', 'o', 'D']
    labels  = list(series.keys())

    ax=pylab.figure()
    barwidth=0.03
    for i,sname in enumerate(series):
        sdata = np.array(series[sname])
        pylab.bar(x=tics+offsets[i], height=sdata, width=barwidth, linewidth=0, color=colors[i])
        pylab.plot(tics+offsets[i]+barwidth/2.,sdata,markers[i],color=colors[i], markersize=10, label=sname)
    #ax.get_axes()[0].xaxis.set_marker('')
    #ax.get_axes()[0].xaxis.set_major_formatter(nullForm)
    #ax.axis["xzero"].set_visible(True)
    #TODO: add axis at y=0
    
    pylab.legend(loc='best')
    pylab.xticks(tics+np.average(offsets), ticNames, rotation=90)
    pylab.ylabel('Standardized Effect')
    pylab.savefig('fig.pdf')

if __name__ == '__main__':
    data = loadData()

    plotDropline(data)

