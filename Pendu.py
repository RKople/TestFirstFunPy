def cls(): print ("\n" * 40)

def PrintLetter(mot, lettres):
    affiche = ""
    for k in mot:
        if k in lettres:
            affiche += k
        else:
            affiche += "_"
    print(affiche)
#PrintLetter("salut", ["a","e","l","t"])

def PrintPendu(mot, lettres):
    countWrong = 0
    for k in lettres:
        if k not in mot:
            countWrong += 1
    if countWrong == 0:
        print()
    elif countWrong == 1:
        print("|")
        print("|")
        print("|")
        print("|")
        print("|")
    elif countWrong == 2:
        print("|----")
        print("|")
        print("|")
        print("|")
        print("|")
    elif countWrong == 3:
        print("|----")
        print("|   |")
        print("|")
        print("|")
        print("|")
    elif countWrong == 4:
        print("|----")
        print("|   |")
        print("|   O")
        print("|")
        print("|")
    elif countWrong == 5:
        print("|----")
        print("|   |")
        print("|   O")
        print("|   |")
        print("|")
    elif countWrong == 6:
        print("|----")
        print("|   |")
        print("|   O")
        print("|  /|")
        print("|")
    elif countWrong == 7:
        print("|----")
        print("|   |")
        print("|   O")
        print("|  /|\ ")
        print("|")
    elif countWrong == 8:
        print("|----")
        print("|   |")
        print("|   O")
        print("|  /|\ ")
        print("|   /")
    elif countWrong == 9:
        print("|----")
        print("|   |")
        print("|   O")
        print("|  /|\ ")
        print("|   /\ ")
#PrintPendu("salut", ["a","e","l","t","n","i","z","k","m","o"])

def CheckWin(mot, lettres):
    check = True
    for k in mot:
        if k not in lettres:
            check = False
    return check
#print(CheckWin("salut", ["s","a","l","u","t"]))

def CheckLose(mot, lettres):
    countWrong = 0
    for k in lettres:
        if k not in mot:
            countWrong += 1
    if countWrong >= 9:
        return True
    else:
        return False
#print(CheckLose("salut", ["z","e","r","y","i","o","p","q","d"]))

mot = input("Choisie un mot : ")
cls()
lettres = []
while(not CheckWin(mot, lettres) and not CheckLose(mot, lettres)):
    lettre = input("Choisie une lettre : ")
    cls()
    lettres.append(lettre)
    PrintPendu(mot, lettres)
    PrintLetter(mot, lettres)

if CheckWin(mot, lettres) == True:
    print("Vous avez gagner !")
else:
    print("Vous avez perdu :(")
