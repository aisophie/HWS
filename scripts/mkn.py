#!/usr/bin/env python
#encoding:utf-8

###################################################
# MKN smoothing
#
# @author Xiaoyi Wu
###################################################

import sys
import getopt
import collections
import cPickle as pickle
import math

def progressbar(complete,total):
    if complete > total-1:return
    progress = float(complete+1) / total
    com = int(progress * 50)
    incom = int((1 - progress) * 50)
    sys.stdout.write("\r[%s>%s] %0.2f%%" % ('-'*com, ' '*incom, progress * 100))
    sys.stdout.flush()
    if complete == total-1:sys.stdout.write('\n')


def getDiscountDict():
    dic = {}
    for order in dists.keys():
        dic.setdefault(order,{1:0,2:0,3:0})
        Y = dists[order].n1 / float(dists[order].n1 + dists[order].n2)
        dic[order][1] = 1 - 2 * Y * dists[order].n2 / dists[order].n1
        dic[order][2] = 2 - 3 * Y * dists[order].n3 / dists[order].n2
        dic[order][3] = 3 - 4 * Y * dists[order].n4 / dists[order].n3 if dists[order].n3!=0 else 0
    return dic
    
def MKN(seq,backoff=False):
    def getGamma(order,cxt):
        gamma = dDic[order][1] * dists[order].N1[cxt] + dDic[order][2] * dists[order].N2[cxt] + dDic[order][3] * dists[order].N3plus[cxt]
        return gamma
    order = len(seq)
    cxt = tuple(seq[-order:-1])
    cw = tuple(seq[-order:])
    print "cw:",order,backoff,cw
    c = dists[order+1].N1bcw[cw] if backoff else dists[order][cw]
    D = 0 if c == 0 else dDic[order][1] if c == 1 else dDic[order][2] if c == 2 else dDic[order][3]
    gamma = getGamma(order,cxt)
    denom = dists[order+1].N1bcw[cxt] if backoff else dists[order].cc[cxt]
    print c,D,gamma,denom
    if order == 1:return (c - D + gamma / V) / dists[1].n
    if denom == 0:return MKN(seq[1:],True)
    p = (max((c - D),0) + gamma * MKN(seq[1:],True)) / denom
    return p


class Distribution(dict):
    def __init__(self):
        dict.__init__(self)
        self.n = self.n1 = self.n2 = self.n3 = self.n4 = 0
        self.N1 = collections.defaultdict(int)
        self.N2 = collections.defaultdict(int)
        self.N3plus = collections.defaultdict(int)
        self.cc = collections.defaultdict(int)#count of certain context
        self.N1bcw = collections.defaultdict(int)#type of backoff cw
        self.N1bc = collections.defaultdict(int)#type of backoff c
        
    def __setitem__(self, seq, freq):
        dict.__setitem__(self, seq, freq)
        self.n += freq
        cxt = seq[:-1]
        w = seq[-1]
        bcw = seq[1:]
        bc = cxt[1:]
        self.cc[cxt] += freq
        self.N1bcw[bcw] += 1
        self.N1bc[bc] += 1
        if freq == 1:
            self.n1 += 1
            self.N1[cxt] += 1
        if freq == 2:
            self.n2 += 1
            self.N2[cxt] += 1
        if freq == 3: self.n3 += 1
        if freq == 4: self.n4 += 1
        if freq >= 3: self.N3plus[cxt] += 1
    def __getitem__(self, seq):
        return dict.__getitem__(self, seq) if seq in self else 0


def eval(sens,order,output):
    global V,dDic
    #calculate some parameters
    V = float(len(dists[1].keys()))
    #create discount dict
    print 'creating discount dictionary...'
    dDic = getDiscountDict()
    print 'calculating perplexity...'
    pps = []
    probs = []
    for i,sen in enumerate(sens):
		#progressbar(i,len(sens))
        if sen.strip() == '':continue
        pp_mkn = []
        ps = []
        seqs = []
        for seq in sen.split("\n"):
            if seq.strip() == '':continue
            tokens = seq.split(" ")
            tokens = ['<B>'] * (order-len(tokens)) + tokens
            seqs.append(tokens)
        for seq in seqs:
            mkn = MKN(seq)
            print seq,mkn
            ps.append("%s\t%s" % (' '.join(seq),mkn))
            pp_mkn.append(-math.log(mkn))
        pps.append((sen.replace("\n","|"),pp_mkn))
        probs.append('\n'.join(ps))
    #output results
    results = []
    all_mkn = []
    for sen,pp_mkn in pps:
        all_mkn += pp_mkn
        result = "%s\t%s" % (sen, math.exp(sum(pp_mkn)/len(pp_mkn)))
        results.append(result)
    f = open(output,'w')
    f.write('\n'.join(results).encode('utf-8'))
    f.close()
    print 'perplexity using MKN:%.3f' % math.exp(sum(all_mkn)/len(all_mkn))



def train(sens,order,output):
    #train
    print 'creating frequency distribution...'
    freqs = {}
    for i in range(order):
        freqs.setdefault(i+1,{})
    for i,sen in enumerate(sens):
        progressbar(i,len(sens))
        if sen.strip() == '':continue
        for seq in sen.split("\n"):
            if seq.strip() == '':continue
            tokens = seq.split(" ")
            tokens = ['<B>'] * (order-len(tokens)) + tokens
            for j in range(len(tokens)):
                subseq = tuple(tokens[j:])
                suborder = len(subseq)
                freqs[suborder][subseq] = freqs[suborder].get(subseq,0) + 1
    print freqs
    #create model
    dists = {}
    for order in freqs:
        print 'constructing order %i model...' % order
        dists[order] = Distribution()
        for i,seq in enumerate(freqs[order]):
            progressbar(i,len(freqs[order]))
            freq = freqs[order][seq]
            dists[order][seq] = freq
        print dists[order].N3plus
    #write
    print 'saving model...'
    pickle.dump(dists,open(output,'w'))
    print 'Done!'
	

def main():
    global dists
    sens = sys.stdin.read().decode("utf-8").split("</s>")
    #check options
    try:
        opts = dict(getopt.getopt(sys.argv[1:],"m:n:o:i:")[0])#-m:mode;-o:output;-n:order;-i:input model
    except getopt.GetoptError as err:
        print (err)
        sys.exit()
    order = 3 if '-n' not in opts else int(opts['-n'])
    output = opts['-o']
    mode = opts["-m"]
    if mode not in ["train","eval"]:
        print "mode has to be 'train' or 'eval'" 
        sys.exit()
    seqs = []
    if mode == 'train':
         train(sens,order,output)
    elif mode == "eval":
        try:
            print 'reading model file...'
            dists = pickle.load(open(opts['-i'],'r'))
        except:
            print 'model file does not exits...'
            sys.exit()
        eval(sens,order,output)
    else:
         sys.exit()


if __name__ == '__main__':
	main()
