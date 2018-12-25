from random import *
import time

def GenNb(mini, maxi): return randrange(mini, maxi)
def PrintInter(mini, maxi): print("[", mini, ",", maxi, "]")

#mini = 0
#maxi = 10

#entry = 0
#nb = GenNb(mini, maxi)
#PrintInter(mini, maxi)
#count = 0

L=[]

debut = time.time()

for k in range(1):
    mini = 0
    maxi = 10**9
    entry = 0
    nb = GenNb(mini, maxi)
    ##PrintInter(mini, maxi)
    count = 0
    while entry != nb:
        #entry = int(input(">>> "))
    
        entry = ((maxi-mini)//2)+mini
        ##print(">>> ", entry)

        #entry = randrange(mini, maxi)
        #print(">>> ", entry)
    
        if entry < nb:
            ##print("Plus grand /\ ")
            mini = entry
            count += 1
            ##PrintInter(mini, maxi)
        elif entry > nb:
            ##print("Plus petit \/")
            maxi = entry
            count += 1
            ##PrintInter(mini, maxi)
    L.append(count)
    #print("Bravo !              Nb : ", nb)
    ##print("                     Essais : ", count)
fin = time.time()
time = fin - debut
somme = 0
for k in L:
    somme += k
moyenne = somme/len(L)
print("MOYENNE d'essais:", moyenne)
print("TEMPS :", time)
print("Plus grand nombre d'essais :", max(L))
