'''
Run Latin Hypercube Sampling on the specified lhsTask


#    Copyright (c) 2007-2010, Michael Genkin and Alexander Gutfraind and Cornell University
#    Distributed under the terms of Creative Commons Attribution License
# 

Usage help
----------

python lhsdriver.py -h

lhsTask.py must contain a script with certain methods:
    * analyzeRunData()
    * displayLHS_BatchStats()
    * getJobFileDataDir(): the directory where the job will produce output
    * getJobAndDataFilePaths()
    * parseDataFile()
    * prepareSamples()  (optional)
    * reportLHS_BatchStats()
    * runJob()
    * writeJobFile()
    
    
Release Notes
-------------
TODO: 
1. Install pp,rpy,R on servers
2. LHS should report which samples were generated
3. scan min should not scan over fixed parameters
5. write statistical analysis package, with pkl of data
8. verify that all parameters are being scanned in lhs.ini

Options
1. Create a .pkl and .csv containing samples (useful for debugging)
2. To introduce repetitions of a run with a particular set of parameters but different seeds,
    the simplest place is during generation of samples is during creation of samples.  
    This parameter could be tagger especially.
    Alternatively, the driven software could know to do multiple runs.
    An alternative is to do so during lhsTask.runJob, but it complicates splitting a job into separate run per seed value.
3. it might be more sensible to hide writing of job files within runLHS_batch or even within lhsTask.runJob
4. Implement multiple runs per sample (maybe)

''' 

import sys
import numpy as np
import numpy.random as npr
import random
import re, os, subprocess, sys, time, pickle
import ConfigParser
import pp

#WARNING: when debugging with pp, reload() will fail to update the code (use -r switch)!

import lhsTask

timeNow = lambda : time.strftime('%Y_%m_%d__%H_%M_%S', time.localtime())

try:
    import rpy2
    import rpy2.robjects as robj
except ImportError, obj:
    print obj
    print 'Couldn\'t import RPy. Samples cannot be generated.'
    #sys.exit(1)

defaultServerList = ('localhost:60001', 'localhost:60002', 'localhost:60003', 'localhost:60004', 'localhost:60005', 'localhost:60006', 'localhost:60007', 'localhost:60008', 'localhost:60009', 'localhost:60010',)

defaultSecret = ''

def copyBatchFile(filepath, targetDir):
    #copy batch into the target dir: purely for backup
    import shutil

    filename = os.path.split(filepath)[1]
    newFilepath = targetDir + os.sep + filename + '_' + timeNow()
    shutil.copyfile(filepath, newFilepath)

def create_simulated_job_server():
    class Struct:
        def __init__(self, **entries): self.__dict__.update(entries)

    job_server = Struct()
    job_server.submit = lambda func,args,depfuncs,modules:lambda :func(*args) #creates a lambda fn which takes no arguments and returns the evaluated number  
    job_server.get_ncpus = lambda : -10
    job_server.get_active_nodes = lambda : {}
    job_server.wait = lambda : ''
    job_server.destroy = lambda : ''
    job_server.print_stats = lambda : ''

    return job_server

def initialize():
    import getopt
    opts, args = getopt.getopt(sys.argv[1:], 'b:c:d:f:m:h', [''])

    cmdStr    = None
    batchFile = ''
    dataDir   = ''
    jobListFilepath = ''
    execMode = set()
    
    for o, a in opts:
       if o in ('-h'):
          usage()
          sys.exit(0)
       if o in ('-b'):
          batchFile = a
          if not os.path.isabs(batchFile):
             batchFile = os.path.join(os.getcwd(), batchFile)
          print 'Using batch file:'
          print batchFile
       if o in ('-c'):
          cmdStr = a
       if o in ('-d'):
          dataDir = a
          if not os.path.isabs(dataDir):
             dataDir = os.path.join(os.getcwd(), dataDir)
       if o in ('-f'):
          jobListFilepath = a
          if not os.path.isabs(jobListFilepath):
              jobListFilepath = os.path.join(os.getcwd(), jobListFilepath)
       if o in ('-m'):
          if 'a' in a:
              execMode.update(['analyze'])
          if 'b' in a:
              execMode.update(['parseBatch', 'generate', 'runJobList', 'analyze'])
          if 'g' in a:
              execMode.update(['parseBatch', 'generate'])
          if 'j' in a:
              execMode.update(['runJobList'])

    print 'Execution mode: ' 
    print list(execMode)

    if len(execMode) == 0:
        print 'Nothing to do...'
        print 
        usage()

    if 'parseBatch' in execMode and (not os.path.exists(batchFile) or not os.path.isfile(batchFile)):
        usage()
        raise ValueError, 'Batch file "%s" does not exists'%batchFile

    if 'analyze' in execMode and 'parseBatch' not in execMode and (not os.path.exists(dataDir) or not os.path.isdir(dataDir)):
        usage()
        raise ValueError, 'Invalid or empty output directory "%s". Review -d option'%dataDir

    if 'runJobList' in execMode and 'parseBatch' not in execMode and (not os.path.exists(jobListFilepath) or not os.path.isfile(jobListFilepath)):
        usage()
        raise ValueError, 'Invalid or empty jobs list file "%s". Review -f option'%jobListFilepath

    if 'runJobList' in execMode and 'parseBatch' not in execMode and (not os.path.exists(dataDir) or not os.path.isdir(dataDir)):
        usage()
        raise ValueError, 'Invalid or empty output directory "%s" (needed for task output .txt files). Review -d option'%dataDir
        
    driverParams = {}
    driverParams['batchFile']       = batchFile
    driverParams['dataDir']         = dataDir
    driverParams['execMode']        = execMode
    driverParams['cmdStr']          = cmdStr
    driverParams['jobListFilepath'] = jobListFilepath
    return driverParams


def parseLHSbatch(path):
    config = ConfigParser.ConfigParser()
    config.read(path)

    runData    = {}
    paramsData = {}
    for section in config.sections():
        if section == 'LHSconfig':
            runData['technique']  = str.lower(config.get(section, 'technique')) #random or lhs
            runData['seed']       = config.getint  (section, 'seed')
            runData['numSamples'] = config.getint  (section, 'numSamples')
            #wishlist runData['offset']     = config.getfloat(section, 'seed')
            runData['dataDir']    = config.get(section, 'dataDir')
            runData['serverList'] = config.get(section, 'serverList')
            runData['burnin']     = config.getint(section, 'burnin')
            runData['scanMinima'] = config.getboolean(section, 'scanMinima')
            runData['scanMinimaSamplesPerParam'] = config.getint(section, 'scanMinimaSamplesPerParam')
            #must be done on each machine in advance
            #runData['serverCaps'] = config.get(section, 'serverCaps')
            continue

        param = {}
        #note: we enforce that some 'min' exists in every numerical parameter
        param['type'] = str.lower(config.get(section, 'type')) #string, boolean, double, int[note: rounded]
        param['dist'] = str.lower(config.get(section, 'dist')) 
        if param['dist'] == 'uniform':
            param['max']   = config.getfloat(section, 'max')
            param['min']   = config.getfloat(section, 'min')
        elif param['dist'] == 'uniformrange':
            s = config.get(section, 'range')
            param['range'] = [int(x) for x in re.split(',', s)]
            param['min']   = min(param['range'])
        elif param['dist'] == 'normal':
            param['max']   = config.getfloat(section, 'max')
            param['min']   = config.getfloat(section, 'min')
            param['mean']  = config.getfloat(section, 'mean')
            param['sd']    = config.getfloat(section, 'sd')
        elif param['dist'] == 'power':
            param['power'] = config.getfloat(section, 'power')
            param['min']   = config.getfloat(section, 'min')
            #wishlist: add support.  in principle just like pnorm below.
        elif param['dist'] == 'zipf':
            param['power'] = config.getfloat(section, 'power')
            param['min']   = config.getfloat(section, 'min')
        elif param['dist'] == 'fixed':
            if param['type'] == 'boolean' or param['type'] == 'string':
                param['value'] = config.get(section, 'value')
            elif param['type'] == 'int' :
                param['value'] = config.getint(section, 'value')
            else:
                param['value'] = config.getfloat(section, 'value')
            param['min']   = param['value']
        else:
            raise ValueError, '%s: unknown distribution'%section
        paramsData[section] = param

    if not os.path.isabs(runData['dataDir']):
        runData['dataDir'] = os.path.join(os.getcwd(), runData['dataDir'])

    if not os.path.exists(runData['dataDir']):
       os.mkdir(runData['dataDir'])
    else:
       if os.path.exists(runData['dataDir']):
            print '!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!'
            print ''
            print '    data directory already exists'
            print ''
            print '!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!'
            print 'Dir:'
            print  runData['dataDir'] 
            print '1) For a new batch, you probably want to change the dataDir parameter'
            print '2) If you are repeating a run, make sure RNG seeds are different'
            print ''
            userIn = raw_input('Do you want to continue [Y]/N?: ')
            if userIn == 'N' or userIn == 'n' or userIn == 'No' or userIn == 'NO':
                sys.exit(0)

    if not os.path.isdir(runData['dataDir']):
       raise ValueError, 'Data directory "%s" path already exists and is not a directory'%runData['dataDir']
    
    random.seed(runData['seed'])
    npr   .seed(runData['seed'])

    runData['serverList'] = re.split(',', runData['serverList'])

    return runData, paramsData


def prepareSamples(runData, paramsData):
    if hasattr(lhsTask, 'prepareSamples'):
        prepareSamplesMet = lhsTask.prepareSamples
    else:
        if runData['technique'] != 'random':
            return prepareSamplesLHS(runData, paramsData)
        else:
            return prepareSamplesRandom(runData, paramsData)

def prepareSamplesLHS(runData, paramsData):
#generate the samples

#wishlist: use runData['offset'] to create some randomization
#see also R.set.seed(42)
    slices = {} #shuffled lists of values, keys by parameter
    for param in paramsData:
        slice = []
        pData = paramsData[param]
        if pData['dist'] == 'fixed':
            slice      = [pData['value']]*runData['numSamples']
        elif pData['dist'] == 'uniformrange':
            numRepeats = runData['numSamples'] / len(pData['range']) + 1 #too much is ok
            slice      = numRepeats*pData['range']
        elif pData['dist'] == 'uniform':
            mn         = pData.get('min', -1E60)
            mx         = pData.get('max', +1E60)
            slice      = np.arange(mn, mx, (mx-mn)/runData['numSamples'], dtype=np.double)
            #maybe use 'offset'
        elif pData['dist'] == 'normal':
            mn         = pData.get('min', -1E60)
            mx         = pData.get('max', +1E60)
            mean       = pData['mean']
            sd         = pData['sd']
            mnquantile = robj.r.pnorm(mn, mean=mean, sd=sd)[0]
            mxquantile = robj.r.pnorm(mx, mean=mean, sd=sd)[0]
            jump       = (mxquantile-mnquantile)/runData['numSamples']
            quantiles  = [mnquantile + jump*(0.5+x) for x in xrange(runData['numSamples'])]
            slice      = map(lambda x: robj.r.qnorm(x, mean=mean, sd=sd)[0], quantiles)

        npr.shuffle(slice)
        slices[param] = slice
        #wishlist: plot histograph of slices

    samples = []
    if not runData['scanMinima']:
        for i in xrange(runData['numSamples']):
            sample = {}
            for param in slices:
                sample[param] = {'type':paramsData[param]['type'], 'value':slices[param][i]}
            samples.append(sample)
    else:
        for flattenedParam in paramsData:
            flattenedValue = paramsData[flattenedParam]['min'] 
            for i in xrange(runData['scanMinimaSamplesPerParam']):
                sample = {}
                for param in slices:
                    if param == flattenedParam:
                        val = flattenedValue
                    else:
                        val = slices[param][i]
                    sample[param] = {'type':paramsData[param]['type'], 'value':val}
                samples.append(sample)

    return samples

def prepareSamplesRandom(runData, paramsData):
    raise NotImplementedError, 'Random sampling not implemented!'
    
def prepareSamplesTest():
#debugging assistance for normal variates
    import pylab
    mn         = -50
    mx         = +50
    mean       = 0.
    sd         = 5.0
    runData    = {'numSamples':30}
    mnquantile = robj.r.pnorm(mn, mean=mean, sd=sd)[0]
    mxquantile = robj.r.pnorm(mx, mean=mean, sd=sd)[0]
    jump       = (mxquantile-mnquantile)/runData['numSamples']
    quantiles  = [mnquantile + jump*(0.5+x) for x in xrange(runData['numSamples'])]
    slice      = map(lambda x: robj.r.qnorm(x, mean=mean, sd=sd)[0], quantiles)
    
    print quantiles
    print slice
    pylab.plot(quantiles, slice, '.')
    #x-axis runs at most 0..1
    #y-axis grows when sd increase, or when the interval is expanded
    #increasing sd should make the curve more steep (or expand the y range), 
    #since it ranges over more different values of the allowed interval


def runJobs(driverParams, serverList=('localhost',), cmdStr=None, burnin=500):
    #note: the jobFileNames refer to file containing instructions for each job, not the LHS instructions
    if 'jobListFilepath' not in driverParams:
        raise ValueError, 'No job file provided...'
    
    jobFilePaths  = []
    runJobsFile = open(driverParams['jobListFilepath'], 'r')
    for idx, line in enumerate(runJobsFile):
        while not line[-1].isalnum(): 
            line=line[:-1]
        filePath = line
        if not os.path.exists(filePath):
            raise ValueError, 'Job file "%s" does not exist!'%filePath
        jobFilePaths.append(filePath)
    runJobsFile.close()

    timeStr = timeNow()
    failuresFile = open(driverParams['dataDir'] + os.sep + 'failed_' + timeStr + '.txt', 'w')
    ppLogFile    = open(driverParams['dataDir'] + os.sep + 'ppLog_' + timeStr + '.txt', 'w')

    try:
        #FIXME
        ### real server ###
        print 'Server list: \n' + str(serverList)
        job_server = pp.Server(ppservers=tuple(serverList), logstream=ppLogFile, loglevel=pp.logging.DEBUG, restart=True, secret=defaultSecret)
        job_server.set_ncpus(0)
        time.sleep(5)
        #print 'Active nodes: \n' + str(job_server.get_active_nodes())

        ### simulated API for local execution (testing) ###
        #FIXME
        #job_server = create_simulated_job_server() 
        #warning: all parameters must be provided. keywords ok.  maybe also destroy?

        jobs = []
        for jobNum,jobFilePath in enumerate(jobFilePaths):
            jobFileDataDir = lhsTask.getJobFileDataDir(jobFilePath)

            args  = (cmdStr, None, jobFilePath, driverParams['dataDir'], jobFileDataDir, os.getcwd(), burnin)
            fcall = job_server.submit(lhsTask.runJob, args=args, depfuncs=(lhsTask.analyzeRunData,lhsTask.parseDataFile), 
                                                                 modules=('os','re','subprocess','numpy','time'))
            jobs.append((jobFilePath,fcall))

            if float(jobNum)/len(jobFilePaths) < .33 and (float(jobNum)+1.0)/len(jobFilePaths) > .33:
                jobs.append(('', job_server.submit(lhsTask.reportStatus, args=(runData, .33,), depfuncs=tuple(), modules=('os',))))
            if float(jobNum)/len(jobFilePaths) < .66 and (float(jobNum)+1.0)/len(jobFilePaths) > .66:
                jobs.append(('', job_server.submit(lhsTask.reportStatus, args=(runData, .66,), depfuncs=tuple(), modules=('os',))))

        for job in jobs:
            ret = job[1]()
            if type(ret) == int:
                print 'Job returned: %d'%ret
            else:
                print 'Error: job failed to return a value!'
            print
            if str(ret) != str(0):
                failuresFile.write(str(job[0]) + '\n') #record just the file name, one per line, so it's easy to re-run
        print job_server.print_stats()
    except Exception, inst:
        print 'Couldn\'t run LHS batch: '
        print inst
        raise
    finally:
        if 'job_server' in dir():
            job_server.destroy()
        #errors unless closed by pp
        #if 'ppLogFile' in dir():
        #    ppLogFile.close()
        if 'failuresFile' in dir():
            failuresFile.close()
        #fixme: failuresFile becomes readonly...


def usage():
    print 'Script for running the Agent-Based Model in LHS mode and analyzing the data'
    print
    print 'The script can be used to:'
    print '1) prepare, run and analyze a batch specified by an ini file, or'
    print '2) prepare a batch'
    print '3) run a prepared batch'
    print '4) analyze a completed batch'
    print 
    print 'eg.'
    print 'To run and analyze a batch file'
    print 'python lhsdriver.py -m b -b lhs.ini'
    print
    print 'To analyze without running the batch file(data generated in the past)'
    print 'python lhsdriver.py -m a -d myOutputDir'
    print
    print 'Supported options are:'
    print '[-b  <file name>]   LHS batch file to execute (default: lhs.ini). The batch file describes in INI format the LHS run, including the sample space. '
    print '[-c  <Command>]     The command to execute when running java. Should contain "%s" at the position of the job file name.'
    print '[-d  <dataDir>]     Perform analysis on dataDir containing simulation output files. Overrided by value in batch file (if specified)'
    print '[-f  <fileOfJobs>]  Load jobs specified by fileOfJobs, one job file name per line.  Used with -m j'
    print '[-h              ]  Displays this'
    print '-m  <a|b|g|j>       Modes (not exclusive):'
    print '                       a=analyze existing output .pkl files.  Requires -d  <directoryToRead>'
    print '                       b=read a batch file, generate samples, run them and then analyze (default). Requires -b <batchFileName>'
    print '                       g=generate job files for lhs task (eg. .pf).  Equivalent to -m gja.  Requires -b <batchFileName>'
    print '                       j=load jobs and run them.  Requires -f <jobListFileName>'

def writeJobs(runData, samples):
    jobPaths = []
    timeStr  = timeNow()
    try:
        for jobNum,sample in enumerate(samples):
            jobFilePath, jobDataFileDir = lhsTask.writeJobFile(runData['dataDir'], sample, jobNum, timeStr)
            jobPaths.append(jobFilePath)
    except Exception, inst:
        print 'Couldn\'t write samples: '
        print inst
        raise

    try:
        jobsListFilepath = 'jobList_'+timeStr
        f = open(jobsListFilepath, 'w')
        for lineNum, jobFile in enumerate(jobPaths):
            f.write(jobFile +  '\n')
    except Exception, inst:
        print 'Error writing job file list '
        print 'see line %d'%lineNum
        print inst
        raise
    finally:
        f.close()

    return jobsListFilepath

if __name__ == '__main__':
    driverParams = initialize()
    runData = {}

    if 'parseBatch' in driverParams['execMode']:
       print 'Parsing batch file:'
       print  driverParams['batchFile']
       runData, paramsData             = parseLHSbatch(driverParams['batchFile']) 
       driverParams['dataDir']         = runData['dataDir'] #overrides command line value

    if 'generate' in driverParams['execMode']:
       print 'Generating samples and job files..'
       samples = prepareSamples(runData, paramsData)
       jobsListFilepath = writeJobs(runData, samples)
       driverParams['jobListFilepath'] = jobsListFilepath
       copyBatchFile(driverParams['batchFile'], driverParams['dataDir'])

    if 'runJobList' in driverParams['execMode']:
       print 'Running jobs from the jobs file:'
       print  driverParams['jobListFilepath']
       runJobs(driverParams, serverList=runData.get('serverList', defaultServerList), cmdStr=driverParams['cmdStr'], burnin=runData.get('burnin', 500))

    if 'analyze' in driverParams['execMode']:
       print 'Starting data analysis on directory:'
       print driverParams['dataDir']
       jobFilePaths, dataFilePaths = lhsTask.getJobAndDataFilePaths(driverParams['dataDir'])
       dossier = lhsTask.reportLHS_BatchStats(dataFilePaths)
       lhsTask.displayLHS_BatchStats(driverParams['dataDir'], dossier)

