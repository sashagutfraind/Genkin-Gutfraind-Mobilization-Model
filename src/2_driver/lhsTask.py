'''
Implementation of the problem used for Latin Hypercube Sampling

#     Copyright (c) 2007-2010, Michael Genkin and Alexander Gutfraind and Cornell University
#    Distributed under the terms of Creative Commons Attribution License
# 

Python 3 port (2026): minimal changes from the Python 2 version kept in original/.  See PORTING.md.

Usage help
----------

Required symbols:
writeBatchFiles(dataDir, samples)
reportLHS_BatchStats(dataDir)
displayLHS_BatchStats(dataDir, report)

Release Notes
-------------
TODO:

'''

import sys
from functools import reduce

import numpy #necessary for simulated job server
import numpy as np
import numpy.random as npr
import random
import re, os, sys, time, pickle, subprocess

#the released jar and its libraries, located relative to this file (<repo>/src/2_driver/lhsTask.py)
repoDir    = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
defaultCmd = 'java -cp "%s" edu.cornell.rad64.NewModel'%os.pathsep.join([os.path.join(repoDir, 'releases', 'radicalization-3.24.3.jar'), os.path.join(repoDir, 'lib', '*')]) + ' -b %s -NS'

def analyzeRunData(radicalData, burnin):
    means, stds = {}, {}
    #import numpy as np #doesn't work through pp

    if len(list(radicalData.values())[0]) <= burnin:
        print('Data is too short to use burnin pruning')
    for col in radicalData:
        data = radicalData[col]
        if len(data) > burnin:
            data = data[burnin:]
        means[col] = numpy.average(data)
        try:
            stds[col] = numpy.std(data,ddof=1)
        except:
            stds[col] = numpy.std(data)

    return means, stds


def displayLHS_BatchStats(dataDir, dossier):
    #dossier is just a list of samples where each sample is a tuple: sample,metrics
    timeNow = lambda : time.strftime('%Y_%m_%d__%H_%M_%S', time.localtime())
    #report is just dict of samples, keyed by filename, where each sample is a tuple: params,metrics

    print()
    print('Starting data analysis and display...')

    if len(dossier) == 0:
        print('Dossier is empty!')
        return

    try:
        dossierSpreadsheetFileName = os.path.join(dataDir, 'batchDossier_t=' + timeNow() + '.csv')
        dossierSpreadsheetFile     = open(dossierSpreadsheetFileName, 'w')
        flatten = lambda ar: reduce(lambda x,y: str(x) + ',' + str(y), ar) #produces a nice comma-seped string from pylists

        paramNames  = list(dossier[0]['sample'].keys())            #simulation parameter settings' names
        metricNames = list(dossier[0]['metrics']['means'].keys())  #metrics' names
        metricNamesExtended = []
        for metricName in metricNames:
            metricNamesExtended.append(metricName)
            metricNamesExtended.append(metricName+'_std')  #assumes that mean data implies std is also provided
        dossierSpreadsheetFile.write(flatten(paramNames))
        dossierSpreadsheetFile.write(', ,' + flatten(metricNamesExtended))
        dossierSpreadsheetFile.write('\n')

        for report in dossier:
            params = report['sample']
            means  = report['metrics']['means']
            stds   = report['metrics']['stds']

            paramVals   = [params.get(paramName, {'value':''})['value'] for paramName in paramNames]
            metricVals  = []
            for metricName in metricNames:
                metricVals.append(means[metricName])
                metricVals.append(stds[metricName])
            dossierSpreadsheetFile.write(flatten(paramVals))
            dossierSpreadsheetFile.write(', ,')
            dossierSpreadsheetFile.write(flatten(metricVals))
            dossierSpreadsheetFile.write('\n')

        print('Dossier written to: ' + dossierSpreadsheetFileName  + '!')
    except Exception as inst:
        print('Error writing dossier!')
        print(inst)
        raise
    finally: 
        dossierSpreadsheetFile.close()
    

def getJobAndDataFilePaths(dataDir):
#all .pf are jobs, .cvs files are report
    jobFilePaths = []
    dataFilePaths = []
    
    for name in os.listdir(dataDir):
        fileExt = os.path.splitext(name)[1]
        if fileExt == '.pf':
            jobFilePaths.append(dataDir + os.sep + name)
        elif fileExt == '.pkl':
            dataFilePaths.append(dataDir + os.sep + name)
        #elif fileExt == '.csv':
        #    dataFilePaths.append(dataDir + os.sep + name)

    print('Found %d job files and %d data files.'%(len(jobFilePaths),len(dataFilePaths)))
    return jobFilePaths, dataFilePaths

def getJobFileDataDir(jobFilePath):
#assumes that the output goes to the directory containing the job file
    if '-' in jobFilePath:
        print('Warning: job file path contains "-" - repast will fail!')
    return os.path.splitext(jobFilePath)[0]


def parseDataFile(path):
#gets the simulation params and data from a raw file

#examplefile
    #lhsSim												
    #MaxLinkAgeBonus,AttritionRate,Population,AvgInitTieStrength
    #0.156895441,0.115438986,801,0.352849321,1000
    #
    #Density,zealCorr,energy,Isolation,avgTieStrength	
    #0.007491,0.125306,-6215.150426,-0.007547
    #...
    #0.007491,0.125306,-6215.150426,-0.007547

    radicalParams = {}
    radicalData   = {}

    print('Reading data file:')
    print(path)
    try:
        f = open(path, 'r')
    except Exception as inst:
        print(inst)
        raise IOError('Can\'t read run report: ' + path)

    paramNamesIdx  = 1
    metricNamesIdx = 4
    paramNames     = []
    paramVals      = []
    metricNames    = None
    rawData        = []
    try:
        for idx, line in enumerate(f):
            if idx == paramNamesIdx:
                paramNames = re.split(',', line)
                paramNames.pop()
                continue
            elif idx == paramNamesIdx+1:
                paramVals = re.split(',', line)
                paramVals.pop()
                for paramNum, param in enumerate(paramNames):
                    radicalParams[param] = {'type':'string', 'value':paramVals[paramNum]}  #'string' is incorrect but has no effect
                continue
            elif idx == metricNamesIdx:
                metricNames = re.split(',', line)
                metricNames.pop()
                numSeries = len(metricNames)
                for col in range(numSeries):
                    rawData.append([])
                continue
            elif idx > metricNamesIdx:
                ar = re.split(',', line)
                if 'NaN' in ar:
                    print('Warning: NaN in %s at line %d'%(path,idx))
                    continue
                for col in range(numSeries):
                    rawData[col].append(float(ar[col]))
                continue
            elif idx < metricNamesIdx:
                continue  #empty lines in the header
            else:
                raise IOError('Malformed header structure!')
    except IOError as inst:
        print('Parse error in line #'+str(idx))
    finally:
        f.close()

    for col in range(numSeries):
        radicalData[metricNames[col]] = rawData[col]

    print('Loaded %d parameters'%len(paramNames))
    print('Loaded %d metrics and %d tics'%(len(metricNames),len(rawData[0])))
    return radicalParams, radicalData

def reportLHS_BatchStats(pklFileNames):
    dossier = []

    for jobNum,fName in enumerate(pklFileNames):
        try:
            pFile     = open(fName, 'rb')
            report    = pickle.load(pFile)

            dossier.append(report)
        except Exception as inst:
            print('Couln\'t open pickled data in file: ' + fName)
            print(inst)
            raise
        finally:
            pFile.close()

    if len(dossier) == 0:
        print('Dossier is empty!')
        return

    return dossier

def reportStatus(runData, progressFraction):
    import time
    timeNow = lambda : time.strftime('%Y_%m_%d__%H_%M_%S', time.localtime())

    try:
        fname   = 'jobStatus_%s__Completion=%.2f'%(timeNow(),progressFraction)
        print()
        print(fname)

        f       = open(runData['dataDir'] + os.sep + fname, 'w')
        #creates a file
        f.close()
    except Exception as inst:
        print(inst)
    return 0

    '''
    #these work only if a mail server is available. 
    import smtplib
    import email.Message
    emailTxt = 'Job is now %f%% complete'%progressFraction

    sender ='LHS batch'
    to     ='' 
    subject='batch progress'
    serverURL='localhost'
    message = email.Message.Message()
    message["To"]      = to
    message["From"]    = sender
    message["Subject"] = subject
    message.set_payload(emailTxt)
    mailServer = smtplib.SMTP(serverURL)
    mailServer.sendmail(sender, to, message.as_string())
    mailServer.quit()
    '''

def roundHalfAwayFromZero(x):
    #Python 2's round(): ties go away from zero (Python 3 rounds ties to even)
    return int(np.sign(x)*np.floor(abs(x)+0.5))

def runJob(cmd, sample, jobFilePath, dataDir, jobDataFileDir, curWorkDir, burnin):
    #debug
    #cmd = 'notepad.exe %s'
   
    #debug - does this call ever succeed:
    #return 13
    
    #paste:
    #  python \Python25\Scripts\ppserver.py -r -s <secret>
    #  ls -d lhs_2009_03_19/* > jobs   ;will generate the absolute path too
 
    os.chdir(curWorkDir)

    jobFilePathQuoted = '\"%s\"'%jobFilePath
    if cmd == None:
        #consider running a bash script.. containing the command.  need to write the cmd into it, though.
        cmd = defaultCmd
        #WARNING: make the right choice otherwise it runs out of memory during full simulations (somewhere in the middle)
        #if sys.platform == 'win32': 
        #    cmd = 'java -jar radicalization.jar -b %s -NS'
        #else:
        #    cmd = 'java -Xms500m -Xmx2G -jar radicalization.jar -b %s -NS'
    outputFilePath = '\"%s\"'%(jobFilePath + '_out.txt')
    
    #ref. http://publib.boulder.ibm.com/infocenter/systems/index.jsp?topic=/com.ibm.aix.baseadmn/doc/baseadmndita/korn_shell_inout_redir.htm
    cmdl = cmd%(jobFilePathQuoted,) + ' 1> ' + outputFilePath + ' 2>&1'
    #cmdl = cmd%(jobFilePathQuoted,) + ' >> ' + outputFilePath
    print(cmdl)
    try:
        #WARNING: os.system conflicts with pp
        #ref: http://www.parallelpython.com/documentation/Python-Documentation-2.4.4c1/lib/module-subprocess.html
        #retCode = os.system(cmdl)
        retCode = subprocess.call(cmdl, shell=True)
    except Exception as inst:
        retCode = 10    
        try:
            outputFile = open(outputFilePath, 'w')
            outputFile.write(str(inst))
        finally:
            if 'outputFile' in dir():
                outputFile.close()
        #do not re-raise

    #not using spawn b/c of difficulties making it accept the environment, even on simple commands like:
    #os.spawnve(os.P_WAIT, 'notepad.exe', ['notepad.exe', 'c.txt'], os.environ)

    #argv = (cmd,  '-jar', 'radicalization.jar', '-b', '"' + jobFileName +'"', '-NS')
    #argv = (cmd,  )
    #retCode = os.spawnle(os.P_WAIT, cmd, argv, os.environ)

    if retCode != 0:
        return retCode

    print('Run completed. Analyzing data and writing a .pkl ...')
    if burnin > 0:
        print('Statistics will be pruned: burnin=%d'%burnin)

    foundDataFile = False
    for f in os.listdir(jobDataFileDir):
        if 'raw.csv' in f:
            #assumes that .raw is the unique data file
            print('Found data file: %s'%f)
            foundDataFile = True
            break
    if foundDataFile:
        radicalParams, radicalData = parseDataFile(jobDataFileDir+os.sep+f)
        means, stds                = analyzeRunData(radicalData, burnin)
        if sample == None:
            sample = radicalParams
        report      = {'sample': sample,
                       'metrics':{'means':means, 'stds':stds}
                      }
    else:
        print('Could not open the data file in directory: %s'%jobDataFileDir)
        return 10

    try:
        pklFilename = jobFilePath + '.pkl'
        outputFile  = open(pklFilename, 'wb')
        pickle.dump(report, outputFile)
        outputFile.close()
        print('Pickle: ' + pklFilename + ' written!')
    except Exception as inst:
        print(inst)
        print('Unable to pickle...')
        #do not re-raise

    return retCode

def writeJobFile(dataDir, sample, jobNum, timeNow):
#writes the job file. the output directory is indicated by jobNum
    jobFilePath    = os.path.join(dataDir, 'job_t=' + timeNow + '_sample=' + str(jobNum) + '.pf')
    jobFileDataDir = getJobFileDataDir(jobFilePath)

    #repast can create the directory itself
    #os.mkdir(jobDataFileDir)

    outputDir = jobFileDataDir
    if ' ' in outputDir or '"' in outputDir:
        print('WARNING: In OutputDirectory parameter (%s): repast does not accept spaces or quotes in parameter values...'%outputDir)
        print('Attempting to fix...')
        if outputDir.startswith(os.getcwd()):
            outputDir = outputDir[len(os.getcwd())+1:]
    outputDir = outputDir.replace(os.getcwd()+os.sep, '') #note: os.path.relpath is available but only in Python >= 2.6
    outputDir = outputDir.replace('\\', '/')

    try:
        f = open(jobFilePath, 'w')
        f.write('runs: 1' + '\n')
        for param in sample:
            f.write(param + '\n')
            if param == 'OutputDirectory':

                f.write('{' + '\n')
                f.write('  set_string: %s\n'%outputDir) #overwrites the setting in the lhs file!
                f.write('}' + '\n')
                continue
            f.write('{' + '\n')
            pData = sample[param]
            if pData['type'] == 'boolean':
                f.write('  set_boolean: %s\n'%str(pData['value']) )
            elif pData['type'] == 'string':
                f.write('  set_string: %s\n'%str(pData['value']) )
            elif pData['type'] == 'int':
                f.write('  set: %d\n'%roundHalfAwayFromZero(pData['value']) ) 
            else:
                f.write('  set: %f\n'%float(pData['value']) ) 
            f.write('}' + '\n')
        f.close()
    except Exception as inst:
        print('Unable to write job file...')
        print(inst)
        #do not re-raise
    
    return jobFilePath, jobFileDataDir
