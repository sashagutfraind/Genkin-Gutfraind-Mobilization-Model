'''
Run the agent-based model with the specified batch file

#     Copyright (c) 2007-2010, Michael Genkin and Alexander Gutfraind and Cornell University
#    Distributed under the terms of Creative Commons Attribution License

Usage help
----------

python abmdriver.py -h

Release Notes
-------------
TODO: 
1. replace complex open.fdopen() to open() 
2. LHS should report which samples were generated
3. order the functions alphabetically

''' 
import sys
sys.path.append('rpy/')

import numpy as np
import matplotlib
#for running scripts silently:
matplotlib.use('PS')
import matplotlib.pylab as pylab
#import pylab
import scipy
import re, os, sys, time, pickle

try:
    import rpy
except ImportError:
    print 'Couldn\'t import RPy. Statistical analysis functionality is deactivated.'

def usage():
    print 'Script for running the ABM and analyzing the data'
    print 'Allowed options are:'
    print '[-b  <Batch file>]  Batch file to execute. In LHS, the batch file describes the sample space. '
    print '[-c  <Command>]     The command to execute'
    print '[-d  <Dir>]         Perform analysis on Dir in which subdirectories contain' 
    print '                    simulation output files. Available only with \'-m a\''
    print '                    Default parsed from batch file \'outputDirectory\' parameter'
    print '                    Each subdirectory is assumed to contain runs with' 
    print '                    the same control parameters, but different seeds.'
    print '[-m  <b|r|a>]       Modes: b=perform (run batch and then analyze (default)), '
    print '                           r=just run and not analyze, a=analyze Only.'
    print '-h                  Displays this'
    print 'eg.'
    print r'To run and analyze a batch file'
    print r'python abmdriver.py -b analysis1.pf'
    print
    print r'To analyze without running the batch file(data generated in the past)'
    print r'python abmdriver.py -b analysis1.pf -m a'


def initialize():
    import getopt
    opts, args = getopt.getopt(sys.argv[1:], 'b:d:m:lh', [''])

    cmd       = 'java -Xms500m -Xmx2G -jar radicalization.jar -b '
    batchFile = ''
    dataDir   = ''
    analysisMode = 'runANDanalyze'
    
    for o, a in opts:
       if o in ('-h'):
          usage()
          sys.exit(0)
       if o in ('-b'):
          batchFile = a
       if o in ('-c'):
          cmd = a
       if o in ('-d'):
          dataDir = a
          analysisMode = 'analyzeOnly'
       if o in ('-m'):
          if (a == 'b') or (a == 'Y'):
              analysisMode = 'runANDanalyze'
          elif (a == 'r') or (a == 'N'):
              analysisMode = 'runANDstop'
          elif (a == 'a') or (a == 'O'):
              analysisMode = 'analyzeOnly'
          else:
              raise ValueError, 'Couldn\'t parse analysis mode'

    if not os.path.exists(batchFile) and not dataDir:
       raise ValueError, 'Batch file \'' + batchFile + '\' doesn\'t exist!  Did you specify a batch file correctly (name after -b option, path has no \\)?'
    if dataDir == '' and analysisMode != 'runANDstop':
       try:
           bFile = os.fdopen(os.open(batchFile, 0))
       except:
           raise IOError, 'Couldn\'t parse batch file for output directory. Make sure it is explicitly given in the batch file.'

       searchStarted = False
       for idx, line in enumerate(bFile):
           if 'outputDirectory' in line:
                searchStarted = True
           if searchStarted:
               m=re.search('.*set_string:\s*', line)
               if not isinstance(m, type(None)):
                  dataDir = line[m.end():len(line)-1]
                  dataDir = dataDir.strip()
                  break
       if dataDir == '':
           raise IOError, 'Couldn\'t parse batch file for output directory. Make sure it is on next line after parameter name'
    if not os.path.isabs(dataDir):
       dataDir = os.getcwd()+os.sep+dataDir
    
    if os.path.exists(dataDir) and analysisMode != 'analyzeOnly':
        print '!!!!!!!!!!!!!!!Warning!!!!!!!!!!!!!!'
        print ''
        print '    outputDirectory already exists'
        print ''
        print '1) If you are repeating a run, make sure that the seeds are allowed to vary'
        print '2) If it\'s a new simulation, you probably want to change the outputDirectory'
        print ''
        userIn = raw_input('Do you want to continue [Y]/N?: ')
        if userIn == 'N' or userIn == 'n' or userIn == 'No' or userIn == 'NO':
            sys.exit(0)
        
    if not os.path.exists(dataDir) and analysisMode == 'analyzeOnly':
        print 'outputDirectory %s doesn\'t exist' %dataDir
        sys.exit(1)
    
    params = {}
    params['batchFile']     = batchFile
    params['dataDir']       = dataDir
    params['analysisMode']  = analysisMode
    params['cmd']           = cmd
    return params

def parseDataFile(path):
    radicalData = {}

    try:
        f = os.fdopen(os.open(path, 0))
    except:
        raise IOError, 'Can\'t read run report: ' + path

    headerIdx = 4

    for idx, line in enumerate(f):
        if idx < headerIdx:
            continue
        elif idx == headerIdx:
            header = re.split(',', line)
            header.pop()
            break

    rawData = []
    numSeries = len(header)
    for i in range(numSeries):
        rawData.append([])

    for idx, line in enumerate(f):
        ar = re.split(',', line)
        if 'NaN' in ar:
            print 'Warning: NaN in %s at line %d'%(path,idx)
            continue
        for i in xrange(numSeries):
            rawData[i].append(float(ar[i]))


    for i in range(numSeries):
        radicalData[header[i]] = rawData[i]

    return radicalData

def summarizeRadicalization(paramValStats):
    #paramValMeans = dict.fromkeys(paramValStats[0][0].keys(), 0) 
    headers = paramValStats[0][0].keys();
    tmp = []
    for i in range(len(headers)):
        tmp.append([])
    paramValMeans = dict(zip(headers, tmp)) 

    for means, vars in paramValStats:
        #vars is not analyzed
        for k in paramValMeans: 
            #paramValMeans[k] += means[k]
            paramValMeans[k].append(means[k])
            
    #for k in paramValMeans: 
          #paramValMeans[k] /= len(paramValStats)

    return paramValMeans

    
def analyzeRunData(radicalData):
    means, vars = {}, {}

    for k in radicalData:
        #means[k] = radicalData[k][-1]
        means[k] = np.average(radicalData[k])
        #vars[k]  = np.std(radicalData[k])

    return means, vars

def reportStats(dataDir):
    dirList  = os.listdir(dataDir)
    dirs = []
    report = {}

    for path in dirList:
        path = dataDir+os.sep+path
        if os.path.isdir(path): 
            dirs.append(path)

    numFiles = -1;
    garbageFound = False 
    for dir in dirs:
        paramValFiles = os.listdir(dir)
        if numFiles == -1:
            numFiles = len(paramValFiles)
        elif len(paramValFiles) != numFiles:
            garbageFound = True

        paramValStats = []
        for f in paramValFiles:
            if not 'raw' in f:
                continue
            radicalData = parseDataFile(dir+os.sep+f)
            means, vars = analyzeRunData(radicalData)
            paramValStats.append([means, vars])

        report[os.path.split(dir)[1]] = summarizeRadicalization(paramValStats)

    if garbageFound:
        raise ValueError, 'At least one of the data directories includes an extra run.  It could be garbage..'

    return report

def sortPropernumerically(str1, str2):
    ar1 = re.split('=', str1)
    ar2 = re.split('=', str2)
    for i in range(len(ar1)):
        s1 = ar1[i]
        s2 = ar2[i]
        if not (s1[0].isdigit() or s1[0] in ('-', '+')) or not (s2[0].isdigit() or s2[0] in ('-', '+')):
            if s1 < s2:
                return -1
            elif s1 > s2:
                return 1
            else:
                continue
        else:
            try:
                f1 = float(s1)
                f2 = float(s2)
            except ValueError:
                if s1 < s2:
                    return -1
                elif s1 > s2:
                    return 1
                else:
                    continue 
            if f1 < f2:
                return -1
            elif f1 > f2:
                return 1
            else:
                continue
                
    return 0

def runRegression(statName, statRange, dataOverParamValues, seriesNames, f):
    f.write('\nRegression for ' + statName + '\n')

    xvals = np.array([], dtype=np.double)
    yvals = np.array([], dtype=np.double)
    f.write(statName + '\t' + 'Mean\n')
    for i, name in enumerate(seriesNames):
        Ys = np.array(dataOverParamValues[i], dtype=np.double)
        try:
            Xs = float(seriesNames[i]) * np.ones(Ys.size, dtype=np.double)
        except:
            Xs = float(i)              * np.ones(Ys.size, dtype=np.double)
        xvals = np.concatenate((xvals, Xs))
        yvals = np.concatenate((yvals, Ys))

        f.write(seriesNames[i] + '\t' + str(Ys.mean()) + '\n')

    data={'x':xvals, 'y':yvals}
    lm_res = rpy.r.lm(rpy.r('y~x'), data)
    lm_sum = rpy.r.summary_lm(lm_res)
    lm_aov = rpy.r.summary_aov(lm_res)

    coeffs = lm_res['coefficients'].values()
    CI_m   = rpy.r.confint(lm_res['call'], 'x',           level=0.95)[0]
    CI_int = rpy.r.confint(lm_res['call'], '(Intercept)', level=0.95)[0]
    f.write('slope (95%% CI)\n%.4G'%(coeffs[0]) + '\n')
    f.write('[%.4G,%.4G]'%(CI_m[0],CI_m[1]) + '\n')

    f.write('intercept (95%% CI)\n%.4G'%(coeffs[1]) + '\n')
    f.write('[%.4G,%.4G]'%(CI_int[0],CI_int[1]) + '\n')

    #f.write('R-sq\n%.4G'%(lm_sum['r.squared']) + '\n')
    f.write('R-sq-adj\n%.4G'%(lm_sum['adj.r.squared']) + '\n')
    f.write('f-stat\n%.4G'%(lm_aov['F value'][0]) + '\n')
    pr_f = lm_aov['Pr(>F)'][0]
    f.write('Pr(>F)\n%.4G'%pr_f + '\n')

    xSpace = xvals[-1]-xvals[0]
    elasticity = coeffs[0]*xSpace/statRange #assume that x-s represent the entire range
    f.write('Elasticity\n%.4G'%(elasticity) + '\n')
    f.write('(x-range: %.4G to %.4G)'%(xvals[0],xvals[-1]) + '\n')

#TODO: make it report the exponent compactly: not 1.57E+004 but 1.57E+4? 
    return elasticity, pr_f

def displayStats(dataDir, report):

    paramSettings = report.keys()
    paramSettings.sort(sortPropernumerically)

    #report is organized as a nested dictionary:
    #report[parameterSetting][statValue] is an array: [mean of run 1, mean of run 2, ..., mean of run n]

    try:
        if rpy.r:
            do_tests = True
    except:
        print 'Couldn\'t import RPy... No statistical analysis will be done.'
        do_tests = False

    #obsolete names
    #valuableStats = ['fracRadicals', 'radicalNeighbZeal', 'radicalClustering']
    #valuableStats = ['Fraction Radicals', 'Radical Dyads', 'Radical Triads']
    valuableStats = ['Density', 'Isolation', 'radicalRawDyads', 'radicalExcessDyads', 'Clustering', 'radicalRawTriads', 
                     'radicalExcessTriads', 'MedianCellSize', 'meanRadicalComponentSize']

    ################################################
    #   update these if the metric is updated
    ################################################
    valuableStatsRanges = {'Density':1., 'Isolation':1., 'Clustering':1.}
    paramName = paramSettings[0][:paramSettings[0].find('=')]
    if do_tests:
        f = open(dataDir + os.sep + paramName + '_' + str(time.localtime()[3:-1]) + '.txt', 'w')
        f.write('Effect of parameter: %s\n'%paramName)

    elasticities = {}
    for statName in valuableStats:
        dataOverParamValues = []
        try:
            for k in paramSettings:
                dataOverParamValues.append(report[k][statName])
        except:
            print 'Unable to read metric: ' + statName + '. Data might be damaged or the metric might be obsolete.'
            continue

        pylab.figure()
        try:
            pylab.boxplot(np.array(dataOverParamValues).transpose())
        except:
            print 'Couldn\'t produce box plot.  Make sure all parameter test groups have the same size.'
            raise

        paramSettingsAbr = []
        paramSettingsAbr = [val[val.find('=')+1:] for val in paramSettings]
        pylab.xlabel(paramName)
        pylab.xticks(range(1,len(report)+1), paramSettingsAbr)
        pylab.ylabel(statName)
        pylab.savefig(dataDir + os.sep + statName + str(time.localtime()[3:-1]) + '.eps')

        try:
           if do_tests:
               elasticity,pr_f = runRegression(statName, valuableStatsRanges.get(statName, 1.), dataOverParamValues, paramSettingsAbr, f)
               elasticities[statName] = (elasticity,pr_f)
        except Exception, inst:
            print 'Could not perform regression for: ' + statName
            print type(inst)     # the exception instance
            print inst.args      # arguments stored in .args
            
    
    if do_tests:
        f.close()

    dossier    = {}
    dossier['paramName']    = paramName
    dossier['elasticities'] = elasticities
    dossier['data']         = report
    filename   = dataDir + os.sep + paramName + str(time.localtime()[3:-1]) + '.pkl'
    outputFile = open(filename, 'wb')
    pickle.dump(dossier, outputFile)
    outputFile.close()
    print 'Pickle: ' + filename + ' written!'

    print 'Done'

def runBatch(batchFile, dataDir, analysisMode, cmd):
    if analysisMode == 'runANDanalyze' or analysisMode == 'runANDstop':
       #see java -X | more for documentation
       runCmd = cmd + batchFile
       #runCmd = 'java -Xms500m -Xmx2G -jar radicalization.jar -b ' + batchFile
       print 'Running: ' + runCmd
       if os.system(runCmd):
           raise Exception, 'Batch execution failed.'

    if analysisMode == 'runANDanalyze' or analysisMode == 'analyzeOnly':
       print 'Starting data analysis on directory:'
       print dataDir
       report = reportStats(dataDir)
       displayStats(dataDir, report)

   #TODO: copy the batch file to the dataDir for reference

if __name__ == '__main__':
    params = initialize()

    runBatch(params['batchFile'], params['dataDir'], params['analysisMode'], params['cmd'])

