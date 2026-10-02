"""


Usage help
----------

python plot_elasticity.py -h

Release Notes
-------------
"""

import numpy as np
import matplotlib
#for running scripts silently:
#matplotlib.use('PS')
#import matplotlib.pylab as pylab
import pylab
import scipy
import re, os, sys, pickle, time, csv

timeNow = lambda : time.strftime('%Y_%m_%d__%H_%M_%S', time.localtime())

#from mpl_toolkits.axes_grid1.anchored_artists import AnchoredSizeBar

def loadCoefficients():
    dossiers = {}
    metricNames = ['Isolation', 'Clustering', 'AvgCellSize']
    #dataFiles = {'Isolation':'../rankIsolationTest.csv', 
    #             'Clustering':'../rankClusteringTest.csv', 
    #             'AvgCellSize':'../rankAvgCellTest.csv'}
    dataFiles = {'Isolation':'../rankIsolation.csv', 
                 'Clustering':'../rankClustering.csv', 
                 'AvgCellSize':'../rankAvgCell.csv'}
    paramNames = [
#magnets
            'Num Pacifist Magnets',
            'Num Neutral Magnets',
            'Num Radical Magnets',
            'FractionExposedtoMagnets',
            'MagneticEncounterRate',
#structural
            'Population Size',
            'Attrition Rate',
            'RadicalAttritionIncrement',
            'FractionInitiallyRadicals',
            'FractionInitiallyPacifists',
#network
            'Degree AVG',
            'Degree SD',
            'InitTieStrength AVG',
            'InitTieStrength SD',
            'TransitiveFriendship',
#individual
            'Num Fixed Attributes',
            'Num Variable Attributes',
            'FixedAttributeSalience',
            'Diversity',
            'ZealSalience',
            'Pressurability AVG',
            'Pressurability SD',
#controls
            'SwitchRandomness',
            'SwitchPressFactor',
            'LinkAgeBonusFactor',
            'MaxLinkAgeBonus',
            'StrengthUpdateRate',
            ]
    paramRemap = {
            'AvgDegree':'Degree AVG',
            'AttritionR~e':'Attrition Rate',
            'AvgPressur~y':'Pressurability AVG',
            'AvgPressurability':'Pressurability AVG',
            'FractionEx~s':'FractionExposedtoMagnets',
            'ZealSalience':'ZealSalience',
            'NumMagnetsN':'Num Neutral Magnets',
            'DegreeSD':'Degree SD',
            'FractionI~ls':'FractionInitiallyRadicals',
            'FractionInitRadicals':'FractionInitiallyRadicals',
            'StrengthUp~e':'StrengthUpdateRate',
            'FixedAttri~e':'FixedAttributeSalience',
            'FixedAttribSalience':'FixedAttributeSalience',
            'MagneticEn~e':'MagneticEncounterRate',
            'Population':'Population Size',
            'NumVarAttr~s':'Num Variable Attributes',
            'NumVarAttributes':'Num Variable Attributes',
            'NumFixedAt~s':'Num Fixed Attributes',
            'NumFixedAttributes':'Num Fixed Attributes',
            'NumMagnetsR':'Num Radical Magnets',
            'Pressurabi~D':'Pressurability SD',
            'PressurabilitySD':'Pressurability SD',
            'RadicalsAt~t':'RadicalAttritionIncrement', #'RadicalsAttritionIncrement',
            'AvgInitTie~h':'InitTieStrength AVG',
            'AvgInitTieStrength':'InitTieStrength AVG',
            'Diversity':'Diversity',
            'Transitive~p':'TransitiveFriendship',
            'SwitchRand~s':'SwitchRandomness',
            'SwitchPres~r':'SwitchPressFactor',
            'InitTieStr~D':'InitTieStrength SD',
            'InitTieStrengthSD':'InitTieStrength SD',
            'LinkAgeBon~r':'LinkAgeBonusFactor',
            'FractionI~ts':'FractionInitiallyPacifists',
            'FractionInitPacifists':'FractionInitiallyPacifists',
            'NumMagnetsP':'Num Pacifist Magnets',
            'MaxLinkAge~s':'MaxLinkAgeBonus',}
    for metric in dataFiles:
        dossier = {}
        with open(dataFiles[metric], 'r', newline='') as f:
            dataDict = csv.DictReader(f, delimiter=',')
            for record in dataDict:
                paramName = paramRemap[record['parameter']]
                dossier[paramName] = record
        dossiers[metric] = dossier
    return dossiers, metricNames, paramNames


def displayStats(dossiers, metricNames, paramNames):
    numDossiers   = len(dossiers)
    numParams     = len(paramNames)
    statColors    = dict(list(zip(metricNames, ['r', 'g', 'b'])))
    statClasses   = dict(list(zip(metricNames, [r'o', r'\\', r'*'])))
    numValStats   = len(metricNames)

    xlocs         = np.arange(numParams, dtype=np.double) 
    #modify this to customize: dict[paramname] = loc
    xlocs_dict    = {}
    for loc, param in enumerate(paramNames):
        xlocs_dict[param] = loc

    yvals   = {}  #by metric
    pvals   = {}  #by metric
    ptexts = {}  #by metric
    for metric in metricNames:
        m_coeffs = np.repeat(0., numParams)
        m_pvals  = np.repeat(0., numParams)
        m_texts  = ['']*numParams
        paramData = dossiers[metric]
        for paramName in paramData:
            coeff = paramData[paramName]['coeff']
            m_coeffs[xlocs_dict[paramName]] = coeff

            pval  = float(paramData[paramName]['pval'])
            m_pvals[xlocs_dict[paramName]] = pval

            if pval < 0.001:
                pr_txt = '***'
            elif pval < 0.01:
                pr_txt = '**'
            elif pval < 0.05:
                pr_txt = '*'
            else:
                pr_txt = ''
            m_texts[xlocs_dict[paramName]] = pr_txt
        yvals[metric]  = m_coeffs
        pvals[metric]  = m_pvals
        ptexts[metric] = m_texts

    ax = pylab.axes([0.1, 0.27, .85, .70])  #[right_shift,vert_shift,right,top] 
    #http://matplotlib.sourceforge.net/examples/pylab_examples/axes_demo.html
    barPlots = []
    colWidth      = 1./1.5/len(metricNames) 
    for i, metric in enumerate(metricNames):
        #if pr_f < 0.05:
        #    linestyle = 'solid'
        #else:
        #    linestyle = 'dashed'
        offset = colWidth*(i - ((numValStats-1)/2 + (numValStats%2)/2))
        barPlot = pylab.bar(xlocs+offset, yvals[metric], colWidth, color=statColors[metric], hatch=statClasses[metric], alpha=1.0)
        for j,xloc in enumerate(xlocs):
            yloc = yvals[metric][j]
            pylab.text(xloc+offset+colWidth*0.5, yloc + 0.05*np.sign(yloc), ptexts[metric][j], rotation=90, fontsize=8)

        barPlots.append(barPlot)

    sortedTicks = list(xlocs_dict.items())
    sortedTicks.sort(key=lambda x:x[1])
    pylab.xticks(xlocs+colWidth/2, [x[0] for x in sortedTicks], rotation=90, fontsize=8)
    pylab.xlim(-2*colWidth, numParams-1+3*colWidth)
    #pylab.ylim(-1.05,1.05)
    #pylab.ylabel('Coefficient')
    pylab.ylabel('Radicalization')

    pylab.grid(True)
    #ax.grid(True, linestyle='-', linewidth=0.1, color=pylab.matplotlib.colors.cnames['grey'])
    xticklines = pylab.getp(pylab.gca(), 'xticklines')
    ygridlines = pylab.getp(pylab.gca(), 'ygridlines')
    pylab.setp(xticklines, 'linewidth', 3)
    pylab.setp(ygridlines, 'linestyle', '-')

    def add_bar(ax, start, end, txt):
       lineStart = start-colWidth
       lineEnd   = end + colWidth*1.5
       ax.plot([lineStart,lineEnd], [-0.5, -0.5], 'k-')
       ax.text(lineStart+(lineEnd-lineStart)/2, -0.55, txt, horizontalalignment='center')
       #asb =  AnchoredSizeBar(ax.transData,
       #                      size,
       #                      txt,
       #                      loc=8,
       #                      pad=0.1, borderpad=0.5, sep=5,
       #                      frameon=False)
       #ax.add_artist(asb)
    
    curPos = 0
    for txt,length in [('magnets',5),('structural',5),('network',5),('individual',7),('controls',5)]:
        add_bar(ax, start=curPos, end=curPos+length-1, txt=txt)
        curPos += length

    #pylab.legend( map(lambda x:x[0], barPlots), metricNames, shadow=True, loc=4)
    pylab.legend( [x[0] for x in barPlots], metricNames, shadow=True, loc='best')
    
    pylab.savefig('output/bar ' + timeNow() + '.pdf')
    pylab.show()
        
    print('Done')

if __name__ == "__main__":
    dossiers, metricNames, paramNames = loadCoefficients()
    displayStats(dossiers, metricNames, paramNames)

