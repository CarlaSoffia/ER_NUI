from email.mime import audio
import os
import pandas as pd
import csv
path = './pt'
os.chdir(path)

all_df = []
#audio_table = pd.read_csv('validated.tsv', sep='\t', header=0, usecols=[1], quoting=csv.QUOTE_NONE)
audio_tableTrain = pd.read_csv('train.tsv', sep='\t', header=0, usecols=[1], quoting=csv.QUOTE_NONE)
audio_tableTest = pd.read_csv('test.tsv', sep='\t', header=0, usecols=[1], quoting=csv.QUOTE_NONE)
audio_tableDev = pd.read_csv('dev.tsv', sep='\t', header=0, usecols=[1], quoting=csv.QUOTE_NONE)
all_df.append(audio_tableTrain)
all_df.append(audio_tableTest)
all_df.append(audio_tableDev)
table = pd.concat(all_df)
path = './clips/'
os.chdir(path)
filesClips = os.listdir()
totalFiles = 0
deleted = 0
found = False
percentage = 0
counter = 0
for file in filesClips:
    found = False
    percentage = round((counter * 100)/len(filesClips),0)

    if percentage == 15:
        print("Currently - 15%")
    if percentage == 25:
        print("Currently - 25%")
    if percentage == 50:
        print("Currently - 50%")   
    if percentage == 75:
        print("Currently - 75%") 
    if percentage == 95:
        print("Currently - 95%")         
    for idx2, row2 in table.iterrows():
        if file == row2[0]:
            totalFiles = totalFiles + 1
            found = True
            counter = counter + 1
            break
    if found == False:
        deleted = deleted + 1
        if os.path.exists(file):
            os.remove(file)    
    counter = counter + 1
print("dropped", str(deleted)) 
   
print("kept", str(totalFiles)) 