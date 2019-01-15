import pickle

##string = "salut"
##
##pickle.dump(string, open('mypicklefile', 'wb'))
##
##f0 = open('mypicklefile', 'rb')
##user_name_list = pickle.load(f0)
##f0.close()
##
##print(user_name_list)

LQ = ["Acteurs", "Mechants", "Humour", "Scenario", "Action", "Envie de le revoir", "Effets speciaux", "Lieux"]
LM = ["Iron Man", "The Incredible Hulk", "Iron Man 2", "Thor", "Captain America: The First Avenger", "Marvel's The Avengers", "Iron Man 3", "Thor: The Dark World", "Captain America: The Winter Soldier", "Guardians of the Galaxy", "Avengers: Age of Ultron", "Ant-Man", "Captain America: Civil War", "Doctor Strange", "Guardians of the Galaxy Vol. 2", "Spider-Man: Homecoming", "Thor: Ragnarok", "Black Panther", "Avengers: Infinity War", "Ant-Man and the Wasp"]
dico = {}
movie = ""
totalPts = 0

def Moy(totalPts, LQ): return totalPts/len(LQ)

def SaveData(moy, movie, dico):
    dico[movie] = moy
    pickle.dump(dico, open('mypicklefile', 'wb'))

def CheckMovie(LM, movie):
    if movie in LM:
        return False
    else:
        return True

def PrintLM(LM, dico):
    k=0
    while(k<len(LM)):
        if LM[k] in dico:
            print('+', LM[k])
        else:
            print('-', LM[k])
        k+=1

def RankCat(Q):
    display = "Notez sur 20 cette categorie - " + Q + ": "
    return int(input(display))

f0=open('mypicklefile', 'rb')
dico = pickle.load(f0)
f0.close()


while(CheckMovie(LM, movie)):
    PrintLM(LM, dico)
    movie = input("Saisir le film à noter : ")

print("Film = ",movie)

for k in LQ:
    note = RankCat(k)
    totalPts += note
moy = Moy(totalPts, LQ)
SaveData(moy, movie, dico)
