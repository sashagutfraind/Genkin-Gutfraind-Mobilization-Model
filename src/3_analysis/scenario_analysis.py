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
rc('text', usetex=True)

import matplotlib.pylab as pylab
#import pylab

import numpy as np
import numpy.random as npr
import csv, time, os
import pdb
import scipy.stats as sps

timeNow = lambda : time.strftime('%Y_%m_%d__%H_%M_%S', time.localtime())

params = ['NumMagnetsN', 'DegreeSD', 'NumMagnetsP', 'randomSeed', 'NumMagnetsR', 'AvgInitTieStrength', 'Diversity', 'NumVarAttributes', 'AttritionRate', 'ZealSalience', 'AvgPressurability', 'Population', 'SwitchRandomness', 'MaxLinkAgeBonus', 'FixedAttribSalience', 'TransitiveFriendship', 'PressurabilitySD', 'LinkAgeBonusFactor', 'StrengthUpdateRate', 'SwitchPressFactor', 'InitTieStrengthSD', 'FractionInitPacifists', 'FractionInitRadicals', 'AvgDegree', 'RadicalsAttritionIncrement', 'FractionExposedToMagnets', 'MagneticEncounterRate', 'NumFixedAttributes',]
#params exclude strings and parameters not changed during simulations

metrics = ['energy', 'energy_std', 'R-clusteringWattsGlobal', 'R-clusteringWattsGlobal_std', 'R-excessTriads', 'R-excessTriads_std', 'R-clusteringWatts', 'R-clusteringWatts_std', 'R-rawDyads', 'R-rawDyads_std', 'R-homophilyPin', 'R-homophilyPin_std', 'R-avgIndCellSize', 'R-avgIndCellSize_std', 'R-fraction', 'R-fraction_std', 'R-clusteringSoffer', 'R-clusteringSoffer_std', 'All-clusteringWatts', 'All-clusteringWatts_std', 'R-numCells', 'R-numCells_std', 'R-rawTriads', 'R-rawTriads_std', 'All-clusteringSoffer', 'All-clusteringSoffer_std', 'R-numIsolatedCells', 'R-numIsolatedCells_std', 'R-dyadRatio', 'R-dyadRatio_std', 'R-assortativity', 'R-assortativity_std', 'R-avgTieStrength', 'R-avgTieStrength_std', 'R-E-I_index', 'R-E-I_index_std', 'R-avgCellSize', 'R-avgCellSize_std', 'medianNumRadRadNeighbors', 'medianNumRadRadNeighbors_std', 'zealCorr', 'zealCorr_std', 'R-medianCellSize', 'R-medianCellSize_std', 'avgTieStrength', 'avgTieStrength_std',]

#ordered for plotting:
paramNames = [
#magnets
        'Num Pacifist Magnets',
        'Num Neutral Magnets',
        'Num Radical Magnets',
        'Fraction Exposed to Magnets',
        'Magnetic Encounter Rate',
#structural
        'Population Size',
        'Attrition Rate',
#       'RadicalAttritionIncrement',
        'Fraction Initially Radicals',
        'Fraction Initially Pacifists',
#network
        'Degree',
#       'Degree SD',
        'Init Tie Strength',
#       'InitTieStrength SD',
        'Transitive Friendship',
#individual
        'Num Fixed Attributes',
        'Num Variable Attributes',
        'Fixed Attribute Salience',
        'Diversity',
        'Zeal Salience',
        'Pressurability',
#       'Pressurability SD',
#controls
#removed from analysis for lack of theoretical interest
#        'SwitchRandomness',
#        'SwitchPressFactor',
#        'LinkAgeBonusFactor',
#        'MaxLinkAgeBonus',
#        'StrengthUpdateRate',
        ]
paramRemap = {
        'AvgDegree':'Degree',
        'AttritionR~e':'Attrition Rate',
        'AttritionRate':'Attrition Rate',
        'AvgPressur~y':'Pressurability',
        'AvgPressurability':'Pressurability',
        'FractionEx~s':'Fraction Exposed to Magnets',
        'FractionExposedToMagnets':'Fraction Exposed to Magnets',
        'ZealSalience':'Zeal Salience',
        'NumMagnetsN':'Num Neutral Magnets',
        'DegreeSD':'Degree SD',
        'FractionI~ls':'Fraction Initially Radicals',
        'FractionInitRadicals':'Fraction Initially Radicals',
        'StrengthUp~e':'Strength Update Rate',
        'StrengthUpdateRate':'Strength Update Rate',
        'FixedAttri~e':'Fixed Attribute Salience',
        'FixedAttribSalience':'Fixed Attribute Salience',
        'MagneticEncounterRate':'Magnetic Encounter Rate',
        'MagneticEn~e':'Magnetic Encounter Rate',
        'Population':'Population Size',
        'NumVarAttr~s':'Num Variable Attributes',
        'NumVarAttributes':'Num Variable Attributes',
        'NumFixedAt~s':'Num Fixed Attributes',
        'NumFixedAttributes':'Num Fixed Attributes',
        'NumMagnetsR':'Num Radical Magnets',
        'Pressurabi~D':'Pressurability SD',
        'PressurabilitySD':'Pressurability SD',
        'RadicalsAt~t':'Radical Attrition Increment', #'RadicalsAttritionIncrement',
        'RadicalsAttritionIncrement':'Radical Attrition Increment',
        'AvgInitTie~h':'Init Tie Strength',
        'AvgInitTieStrength':'Init Tie Strength',
        'Diversity':'Diversity',
        'Transitive~p':'Transitive Friendship',
        'TransitiveFriendship':'Transitive Friendship',
        'SwitchRand~s':'Switch Randomness',
        'SwitchPres~r':'Switch Press Factor',
        'InitTieStr~D':'Init Tie Strength SD',
        'InitTieStrengthSD':'Init TieS trength SD',
        'LinkAgeBon~r':'Link Age Bonus Factor',
        'FractionI~ts':'Fraction Initially Pacifists',
        'FractionInitPacifists':'Fraction Initially Pacifists',
        'NumMagnetsP':'Num Pacifist Magnets',
        'MaxLinkAge~s':'Max Link Age Bonus',}

timeNow = lambda : time.strftime('%Y_%m_%d__%H_%M_%S', time.localtime())

class reportFile:
    f = None
    def __init__(self,fname=None):
        if fname == None:
            fname = 'output/radicalizationReport_'+timeNow()+'.csv'
        self.f = open(fname, 'w')
    def close(self):
        if self.f!= None:
            self.f.close()
    def writeln(self,s=''):
        self.f.write(str(s) + os.linesep)
        print(s)

reportF = reportFile()

def indicesHighRFraction(metricsData,percentile=10,top=True,absolute=True):
    metricDat = metricsData[:,metrics.index('R-fraction')]
    if absolute:
        if top:
            threshold = -1 + 2 * (1.-percentile/100.)
        else:
            threshold = -1 + 2 * (percentile/100.)
    else:
        if top: 
            threshold = np.percentile(metricDat,100-percentile)
        else:
            threshold = np.percentile(metricDat,percentile)
    if top:
        return np.where(metricDat>threshold)[0]
    else:
        return np.where(metricDat<threshold)[0]

def indicesHighIsolation(metricsData,percentile=10,top=True,absolute=True):
    metricDat = -1*metricsData[:,metrics.index('R-E-I_index')]
    if absolute:
        if top:
            threshold = -1 + 2 * (1.-percentile/100.)
        else:
            threshold = -1 + 2 * (percentile/100.)
    else:
        if top: 
            threshold = np.percentile(metricDat,100-percentile)
        else:
            threshold = np.percentile(metricDat,percentile)
    if top:
        return np.where(metricDat>threshold)[0]
    else:
        return np.where(metricDat<threshold)[0]

def indicesHighClustering(metricsData,percentile=10,top=True,absolute=True):
    metricDat = metricsData[:,metrics.index('R-clusteringSoffer')]
    if absolute:
        if top:
            threshold = 1.-percentile/100.
        else:
            threshold = percentile/100.
    else:
        if top: 
            threshold = np.percentile(metricDat,100-percentile)
        else:
            threshold = np.percentile(metricDat,percentile)
    if top:
        return np.where(metricDat>threshold)[0]
    else:
        return np.where(metricDat<threshold)[0]

def indicesHighCellSize(metricsData,percentile=10,top=True,absolute=False):
    #percentile is a misnomed in the absolute regime since there is no upped bound
    metricDat = metricsData[:,metrics.index('R-avgCellSize')]
    if bool(absolute):  #a positive threshold
        threshold = float(percentile)
    else:
        if top: 
            threshold = np.percentile(metricDat,100-percentile)
        else:
            threshold = np.percentile(metricDat,percentile)
    if top:
        return np.where(metricDat>threshold)[0]
    else:
        return np.where(metricDat<threshold)[0]


def indicesHighIsolationClustering(metricsData,percentile=10,top=True,absolute=True):
    indices0 = indicesHighIsolation(metricsData,percentile,top,absolute)
    indices1 = indicesHighClustering(metricsData,percentile,top,absolute)
    return joinIndices(indices0,indices1)

def joinIndices(set1, set2):
    return np.array(list(set(set1).intersection(set2)))

def loadData(fname):
    reportF.writeln('Loading: '+fname)
    #records = []
    paramsData    = []
    metricsData    = []
    with open(fname, 'r', newline='') as f:
        dataDict = csv.DictReader(f, delimiter=',')
        for record in dataDict:
            #floatRec = {'params':[], 'metrics':[]}
            ar = []
            for field in params:
                #floatRec['params'][field] = float(record[field])
                ar.append(float(record[field]))
            paramsData.append(ar)
            ar = []
            for field in metrics:
                #floatRec['metrics'][field] = float(record[field])
                ar.append(float(record[field]))
            #record.append(floatRec)
            metricsData.append(ar)

    paramsData  = np.array(paramsData)
    metricsData = np.array(metricsData)

    return paramsData,metricsData


def plotComparison(name,dossiers,paramNames):
    scenarNames   = [scenName for scenName,dat in dossiers]
    numParams     = len(paramNames)
    statColors    = dict(list(zip(scenarNames, ['y', 'r', 'g', 'b'])))  #this sequence matches 2 rad scenarios, 2 de-rad scenarios
    statHatches   = dict(list(zip(scenarNames, [r'o', r'', r'o/', r'/'])))
    numValStats   = len(scenarNames)

    yvals  = {}  #by scenName
    pvals  = {}  #by scenName
    ptexts = {}  #by scenName
    xlocs  = np.arange(numParams, dtype=np.double)
    #modify this to customize: dict[paramname] = loc
    xlocs_dict    = {}
    for loc, paramName in enumerate(paramNames):
        xlocs_dict[paramName] = loc

    for scenNum, scenName in enumerate(scenarNames):
        m_coeffs = np.repeat(0., numParams)
        m_pvals  = np.repeat(0., numParams)
        m_texts  = ['']*numParams
        paramData = dossiers[scenNum][1]
        for paramName,paramInfo in paramData:
            #if (paramName not in paramNames) or (paramName in ['randomSeed']):
            if (paramName in ['randomSeed']):
                continue
            paramName = paramRemap.get(paramName, paramName)
            coeff = paramInfo['stdCoeff']
            m_coeffs[xlocs_dict[paramName]] = coeff
            pval  = float(paramInfo['MW'])
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
        yvals[scenName]  = m_coeffs
        pvals[scenName]  = m_pvals
        ptexts[scenName] = m_texts

    pylab.figure() #would this work to prevent blocking?
    ax = pylab.axes([0.1, 0.29, .85, .65])  #[right_shift,vert_shift,right,top] 
    #http://matplotlib.sourceforge.net/examples/pylab_examples/axes_demo.html
    barPlots = []
    whitespace = 0.3 #fractional 
    colWidth   = (1.-whitespace)/numValStats 
    leftEndShift = colWidth*(.5 - (numValStats+1)/2 + (numValStats%2)/2)
    for scenNum, scenName in enumerate(scenarNames):
        barPlot = pylab.bar(x=xlocs+leftEndShift+colWidth*scenNum, height=yvals[scenName], width=colWidth, 
                            color=statColors[scenName], hatch=statHatches[scenName], alpha=1.0)
        for j,xloc in enumerate(xlocs):
            yloc = yvals[scenName][j]
            yloc = yloc + 0.10*np.sign(yloc)
            rotation = -90.
            pylab.text(xloc+leftEndShift+colWidth*(scenNum-0.5), yloc, ptexts[scenName][j], rotation=rotation, fontsize=10)

        barPlots.append(barPlot)

    #pylab.xlim(-(numValStats/2)*colWidth, numParams-1 +(numValStats/2)*colWidth)
    pylab.xlim(-0.49, numParams-1 + 0.49)
    pylab.ylabel('Effect Size (standardized change in mean value)', fontsize=15)
    #pylab.ylabel('Radicalization')
    #pylab.title(name)
    sortedTicks = list(xlocs_dict.items())
    sortedTicks.sort(key=lambda x:x[1])
    pylab.xticks(xlocs+colWidth/2, [x[0] for x in sortedTicks], rotation=90, fontsize=9)

    pylab.grid(True)
    #ax.grid(True, linestyle='-', linewidth=0.1, color=pylab.matplotlib.colors.cnames['grey'])
    xticklines = pylab.getp(pylab.gca(), 'xticklines')
    ygridlines = pylab.getp(pylab.gca(), 'ygridlines')
    pylab.setp(xticklines, 'linewidth', 3)
    pylab.setp(ygridlines, 'linestyle', '-')

    def add_bar(ax, start, end, txt, offset):
       lineStart = start - colWidth*offset
       lineEnd   = end   + colWidth*offset
       bot,top   = ax.get_ylim()
       ax.plot([lineStart,lineEnd], [bot*0.8, bot*0.8], 'k-')
       ax.text(lineStart+(lineEnd-lineStart)/2, bot*0.8 - 0.18, txt, horizontalalignment='center')
        #transform = ax.transAxes)
    
    bot,top   = ax.get_ylim()
    ax.set_ylim(bot*1.30, top) #for text
    curPos = 0
    #for txt,length in [('magnets',5),('structural',5),('network',5),('individual',7),]:#('controls',5)]:
    for txt,length in [('MAGNETS',5),('STRUCTURAL',4),('NETWORK',3),('INDIVIDUAL',6),]:
        add_bar(ax, start=curPos, end=curPos+length-1, txt=txt, offset=numValStats/2.)
        curPos += length

    if numValStats > 1:
        pylab.legend( [x[0] for x in barPlots], scenarNames, shadow=True, loc=(0.66,0.2))
        #pylab.legend( map(lambda x:x[0], barPlots), scenarNames, shadow=True, loc=(0.66,0.65))
        ###pylab.legend( map(lambda x:x[0], barPlots), scenarNames, shadow=True, loc='best')
    
    fname='output/'+name +'_'+ timeNow() + '.pdf'
    pylab.savefig(fname)  #there is also a dpi argument!
    os.system('convert -density 600 '+fname+' '+fname[:-5]+'.png')
    #http://www.imagemagick.org/script/command-line-options.php
        

def reportScenarioBasic(paramsData,metricsData):
    res = {'highIsolation':{'func':lambda data: indicesHighIsolation(data,percentile=66,top=True,absolute=False), 'paramSense':{}},
            'highClustering':{'func':lambda data: indicesHighClustering(data,percentile=66,top=True,absolute=False),  'paramSense':{}},
            'highCellSize':{'func':lambda data: indicesHighCellSize(data,percentile=66,top=True),  'paramSense':{}},
            'highIsolationClustering':{'func':lambda data: indicesHighIsolationClustering(data,percentile=66,top=True,absolute=False),  'paramSense':{}},
            }

    for scenario in res:
        reportF.writeln()
        reportF.writeln( 'Scenario:')
        reportF.writeln( scenario)
        filteredIndices = res[scenario]['func'](metricsData)
        reportF.writeln( '%d original, %d filtered (%.2f)'%(len(metricsData),len(filteredIndices),len(filteredIndices)/(1.0*len(metricsData))))
        res[scenario]['filteredSize'] = len(filteredIndices)
        if len(filteredIndices) == 0:
            reportF.writeln( 'Warning: no points fit the scenario!')
            continue
        for param in params:
            if paramRemap.get(param,None) not in paramNames:
                continue
            fullParamArray = paramsData[:,params.index(param)]

            filteredParamArray = fullParamArray.take(filteredIndices)

            #reportF.writeln( param)
            paramRec = {}
            KS = sps.ks_2samp(fullParamArray, filteredParamArray, method='asymp')[1]
            #reportF.writeln( 'Kolmogorov-Smirnov: p=%.4f'%KS)
            paramRec['KS'] = KS
            #MWW requires that the samples are independent, so we remove them
            fullParamArrayPruned = list(fullParamArray)
            for i in filteredParamArray:
                fullParamArrayPruned.remove(i) #just one occurance
            MW = sps.mannwhitneyu(fullParamArrayPruned, filteredParamArray, alternative='two-sided', method='asymptotic')[1]/2.
            #reportF.writeln( 'Mann-Whitney: p=%.4f'%MW)
            paramRec['MW'] = MW
           
            pmean         = np.average(fullParamArray)
            pmeanFiltered = np.average(filteredParamArray)
            psd           = np.std(fullParamArray,ddof=1)
            stdCoeff = (pmeanFiltered-pmean)/psd
            #reportF.writeln( 'pmean=%f, pmeanFiltered=%f, psd=%f, stdCoeff=%.2f'%(pmean, pmeanFiltered, psd, stdCoeff))
            paramRec['pmean'] = pmean
            paramRec['pmeanFiltered'] = pmeanFiltered
            paramRec['psd'] = psd
            paramRec['stdCoeff'] = stdCoeff

            res[scenario]['paramSense'][param] = paramRec
        
        paramData = list(res[scenario]['paramSense'].items())
        paramData.sort(key=lambda x:x[1]['MW'])
        reportF.writeln( 'param, stdCoeff, Mann-Whitney p')
        for param,info in paramData:
            reportF.writeln( '%s,%f,%f'%(param, info['stdCoeff'], info['MW']))

def reportScenarioFull(paramsData,metricsData,paramNames):
    '''
    conditions=\
     [
         ('Isolation',{'isolation':'high'}),
         ('Clustering',{'clustering':'high'}),
         ('CellSize',{'avgCellSize':'high'}),
         ('Fraction (Rad)',{'rFraction':'high'}),
     ]
    measures=\
       {
        '50rel_rawMetrics':{'rFraction':{'absolute':False, 'threshold':50,},
                            'isolation':{'absolute':False, 'threshold':50,},
                            'clustering':{'absolute':False, 'threshold':50,},
                            'avgCellSize':{'absolute':False, 'threshold':50,}},}

    filters = {'rFraction':indicesHighRFraction, 'isolation':indicesHighIsolation, 'clustering':indicesHighClustering, 'avgCellSize':indicesHighCellSize}
    
    '''
    conditions=\
    [
     ('LoneWolfs',{'isolation':'high', 'clustering':'low', 'avgCellSize':'low'}),
     ('WolfPacks',{'isolation':'high', 'clustering':'high', 'avgCellSize':'high'}),
     ('TrappedWolfs',{'isolation':'low', 'clustering':'low', 'avgCellSize':'low'}),
     ('TrappedWolfPacks',{'isolation':'low', 'clustering':'high', 'avgCellSize':'high'}),
     ]

    measures=\
       {
       # {'25abs':{'isolation':{'absolute':True, 'threshold':25,},    
       #     'clustering':{'absolute':True, 'threshold':25,},  
       #    'avgCellSize':{'absolute':True, 'threshold':4,}},

       # '33abs':{'isolation':{'absolute':True, 'threshold':33,}, 
       #    'clustering':{'absolute':True, 'threshold':33,}, 
       #    'avgCellSize':{'absolute':True, 'threshold':3,}},

       # '25rel':{'isolation':{'absolute':False, 'threshold':25,},
       #    'clustering':{'absolute':False, 'threshold':25,},
       #    'avgCellSize':{'absolute':False, 'threshold':25,}},

        '50rel':{
           'isolation':{'absolute':False, 'threshold':50,},
           'clustering':{'absolute':False, 'threshold':50,},
           'avgCellSize':{'absolute':False, 'threshold':50,}},}

    filters = {'isolation':indicesHighIsolation, 'clustering':indicesHighClustering, 'avgCellSize':indicesHighCellSize}

    scenarios = []
    for condName,condSets in conditions:
        for measureName in measures:
            measureInfo = measures[measureName]
            name = condName #+ '_' + str(measureNum+1)

            indices = []
            reportF.writeln(name)
            for metName in filters:
                if metName not in condSets:
                    indices.append(list(range(metricsData.shape[0])))
                    reportF.writeln('Skipping filter on: ' + metName)
                    continue
                pecentile = measureInfo[metName]['threshold']
                top=condSets[metName]=='high'
                absolute=measureInfo[metName]['absolute']

                indices.append(filters[metName](metricsData, percentile=pecentile, top=top, absolute=absolute))
                reportF.writeln(metName)
                reportF.writeln('numSims :'+str(len(indices[-1])))
            resIndices = indices[0]
            for indicesSet in indices[1:]:
                resIndices = joinIndices(resIndices, indicesSet)
            reportF.writeln('numSim in all :'+str(len(resIndices)))

            scenarios.append({'name':name, 'filteredIndices':resIndices, 'paramSense':{}, 'condition':condName, 'measure':measureName, } )
    
    #these are combinations of condition + measure
    for scenario in scenarios:
        reportF.writeln()
        reportF.writeln('Scenario:')
        reportF.writeln(scenario['name'])
        filteredIndices = scenario['filteredIndices']
        reportF.writeln('%d original, %d filtered (%.2f)'%(len(metricsData),len(filteredIndices),len(filteredIndices)/(1.0*len(metricsData))))
        scenario['filteredSize'] = len(filteredIndices)
        if len(filteredIndices) == 0:
            reportF.writeln('Warning: no points fit the scenario!')
            continue
        for param in params:
            if paramRemap.get(param,param) not in paramNames:
                continue
            fullParamArray = paramsData[:,params.index(param)]

            filteredParamArray = fullParamArray.take(filteredIndices)

            fullParamArrayPruned = list(fullParamArray)
            for i in filteredParamArray:
                fullParamArrayPruned.remove(i) #just one occurance is removed
            #note: using sets would be faster, but it would remove repeats from each sample;  yet, if x passes the filter, so would its repeat
            assert len(fullParamArrayPruned) > 20
            paramRec = {}
            KS = sps.ks_2samp(fullParamArrayPruned, filteredParamArray, method='asymp')[1]
            paramRec['KS'] = KS
            MW = sps.mannwhitneyu(fullParamArrayPruned, filteredParamArray, alternative='two-sided', method='asymptotic')[1]/2.
            paramRec['MW'] = MW
           
            pmeanPruned   = np.average(fullParamArrayPruned)
            pmeanFiltered = np.average(filteredParamArray)
            psd           = np.std(fullParamArray,ddof=1)
            stdCoeff = (pmeanFiltered-pmeanPruned)/psd
            #reportF.writeln( 'pmean=%f, pmeanFiltered=%f, psd=%f, stdCoeff=%.2f'%(pmean, pmeanFiltered, psd, stdCoeff))
            paramRec['pmean'] = pmeanPruned
            paramRec['pmeanFiltered'] = pmeanFiltered
            paramRec['psd'] = psd
            paramRec['stdCoeff'] = stdCoeff

            scenario['paramSense'][param] = paramRec
        
        '''
        paramData = scenario['paramSense'].items()
        paramData.sort(key=lambda x:x[1]['MW'])
        reportF.writeln('param, stdCoeff, Mann-Whitney p')
        for param,info in paramData:
            reportF.writeln('%s,%f,%f'%(param, info['stdCoeff'], info['MW']))

        plotComparison(scenario['name']+'_numSimsMeetScenario='+str(scenario['filteredSize'])+'__', dossiers=[('main',paramData)], paramNames=paramNames)
        '''

    #for targetCondition,dummy in conditions:
    #    scenarioNames = [scenario['name']               for scenario in scenarios if scenario['condition']==targetCondition]
    #    paramDatas    = [scenario['paramSense'].items() for scenario in scenarios if scenario['condition']==targetCondition]
    #    plotComparison('crossConditions_'+targetCondition, dossiers=zip(scenarioNames,paramDatas), paramNames=paramNames)
    
    for targetMeasure in measures:
        scenarioNames = [scenario['name']               for scenario in scenarios if scenario['measure']==targetMeasure]
        paramDatas    = [list(scenario['paramSense'].items()) for scenario in scenarios if scenario['measure']==targetMeasure]
        plotComparison('crossMeasures_'+targetMeasure, dossiers=list(zip(scenarioNames,paramDatas)), paramNames=paramNames)
    return scenarios

paramsData,metricsData = loadData(fname='../batchDossier_t=2011_02_16__transitivity.csv')
#reportScenarioBasic(paramsData,metricsData)
reportScenarioFull(paramsData,metricsData,paramNames)
pylab.show()

reportF.close()
