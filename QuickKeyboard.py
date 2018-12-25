from random import *
import time

def GenL():
    L = []
    with open("Sentences.txt", "r") as f:
        for line in f.readlines():
            L.append(line.strip())
    return L

def SelectSentence(L):
    rand = randrange(len(L))
    return L[rand]

def CheckRight(entry, sentence):
    if entry == sentence:
        return True
    else:
        return False
def PrintScore(time, sentence):
    print("Votre score :", len(sentence)/time, "lettres par seconde")

def CountDown(sentence):
    print(sentence)
    for k in range(5, 0, -1):
        time.sleep(1)
        print(k)

L = GenL()
sentence = SelectSentence(L)
CountDown(sentence)
debut = time.time()
entry = input(">>> ")
fin = time.time()
time = fin-debut
if CheckRight(entry, sentence):
    PrintScore(time, sentence)
else:
    print("Wrong")
