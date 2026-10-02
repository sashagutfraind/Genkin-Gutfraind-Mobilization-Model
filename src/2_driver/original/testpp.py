import pp

def runCmd(cmd):
    return os.system(cmd)

def runPP_Test():
    ppservers = tuple('localhost')
    job_server = pp.Server(ppservers=ppservers)

    #cmd = 'java -jar radicalization.jar' #works
    #cmd = 'java -jar radicalization.jar -b sample1.pf' #fails
    #cmd = 'java -jar radicalization.jar -b sample%d.pf -NS >> hmm%d.txt' #works
    cmd = 'java -jar radicalization.jar -b sample%d.pf -NS >> hmm%d.txt' #works
    cmd = 'java -jar radicalization.jar -b "c:\\temp\\lhs\\job_t=2009.3.18_16.10.18_sample=7" -NS >> hmm%d.txt'

    jobs = []
    for i in xrange(1):
        cmdN  = cmd%(i,)
        print cmdN
        args  = (cmdN,)
        fcall = job_server.submit(runCmd, args=args, depfuncs=(), modules=('os',) )
        jobs.append((i,fcall))

    print jobs
    #for job in jobs:
    #    ret = job[1]()
    #    print ret
    
    return jobs

jobs=runPP_Test()
