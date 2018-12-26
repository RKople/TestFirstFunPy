from random import *

def PrintCalcul(nb1, nb2):
    print(nb1, "*", nb2)

def CheckRight(nb1, nb2, nbTook):
    if nbTook != "quit" and nbTook != "score" and nbTook != "interval" and nbTook != "cheat":
        if nb1*nb2 == int(nbTook):
            return 1 #1=True
        else:
            return 2 #2=False
    else:
        return 3 #3=command

def GenNb(mini, maxi): return randrange(mini, maxi)

def AddScore(nb1, nb2, nbTook, score):
    if CheckRight(nb1, nb2, nbTook) == 1:
        return 1
    elif CheckRight(nb1, nb2, nbTook) == 2:
        return -2
    else:
        return 0

def Command(nbTook, score, mini, maxi):
    if nbTook == "score":
        print("Votre score : ", score)
    elif nbTook == "interval":
        print("[", mini, ",", maxi, "]")

def CheckEntry(nbTook):
    if "*" in nbTook or nbTook == '':
        return False
    else:
        return True
def MinMax(score):
    if score < 0:
        return 0
    else:
        return score*2

nbTook = ""
score = 0
mini = score * 2
maxi = score * 2 + 10

while nbTook != "quit":
    nb1 = GenNb(mini, maxi)
    nb2 = GenNb(mini, maxi)
    print("")
    PrintCalcul(nb1, nb2)
    nbTook = str(input(">>> "))
    if nbTook == "cheat":
        score += 10
    if CheckEntry(nbTook):
        Command(nbTook, score, mini, maxi)
        score += AddScore(nb1, nb2, nbTook, score)
        #mini = MinMax(score)
        maxi = MinMax(score) + 10
    else:
        print("Vous ne pouvez pas saisir ceci :(")
