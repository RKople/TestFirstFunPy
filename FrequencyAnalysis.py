def Txt():
    txt = input("Saisir le texte : \n")
    txt = txt.lower()
    return txt

def CbInTxt(lettre, txt):
    count = 0
    for k in txt:
        if k == lettre:
            count += 1
    return count

def PrintStats(lettre, count):
    print(lettre, "=", count, "%")
    print("")

txt = Txt()
total = len(txt.replace(" ",""))
for k in range(ord('a'),ord('z')+1):
    lettre = chr(k)
    cb = CbInTxt(lettre, txt)
    percant = (cb*100)/total
    PrintStats(lettre, percant)
