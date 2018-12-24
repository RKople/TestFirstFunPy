from random import *

def PrintCalcul(nb1, nb2):
    print(nb1, "*", nb2)

def CheckRight(nb1, nb2, nbTook):
    if nbTook != "quit" and nbTook != "score":
        if nb1*nb2 == int(nbTook):
            return True
        else:
            return False
    else:
        return False

def GenNb(mini, maxi): return randrange(mini, maxi)

def AddScore(nb1, nb2, nbTook, score):
    if CheckRight(nb1, nb2, nbTook):
        return 1
    else:
        return -2

def Command(nbTook, score):
    if nbTook == "score":
        print("Votre score : ", score)

def CheckEntry(nbTook):
    if "*" in nbTook:
        return False
    else:
        return True

nbTook = ""
score = 0
mini = score * 2
maxi = score * 2 + 10

while nbTook != "quit":
    nb1 = GenNb(mini, maxi)
    nb2 = GenNb(mini, maxi)
    PrintCalcul(nb1, nb2)
    nbTook = str(input(">>> "))
    if CheckEntry(nbTook):
        Command(nbTook, score)
        score += AddScore(nb1, nb2, nbTook, score)
        mini = score * 2
        maxi = score * 2 + 10
    else:
        print("Vous ne pouvez pas saisir ceci :(")
