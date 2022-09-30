import os
import pandas as pd
from pydub import AudioSegment
import sys
if len(sys.argv) != 2:
    exit("Missing tsv file")
else:
    file_tsv =  sys.argv[1]


def redoCSVFiles():
    path = './pt/clips/'
    totalFiles = len(os.listdir(path))  

    path = './pt/'
    os.chdir(path)
    audio_table = pd.read_csv(file_tsv, sep='\t', header=0)
    total = len(audio_table)
    if file_tsv == 'train.tsv' or file_tsv == 'test.tsv':
        lines = round(total*0.25)
    if file_tsv == 'validated.tsv': 
        lines = round(total*0.4)

    if file_tsv == 'train.tsv' or file_tsv == 'test.tsv' or file_tsv == 'validated.tsv':
        deleteRows = total-lines   
    else:
        deleteRows = total
    print("[Begin] - "+ str(deleteRows) + "/"+str(total)+" of the file")    
    path = './clips/'
    os.chdir(path)
    deleted=0
    if file_tsv == 'train.tsv' or file_tsv == 'test.tsv' or file_tsv == 'validated.tsv':
        for idx, row in audio_table.iterrows():
            if deleted >= deleteRows:
                break 
            if os.path.exists(row[1]) == False:
                audio_table.drop(idx, inplace=True)
                deleted = deleted + 1
    else:
        for idx, row in audio_table.iterrows():        
            audio_table.drop(idx, inplace=True)
            deleted = deleted + 1
    totalFiles = len(audio_table) 
    path = './../'
    os.chdir(path)
    audio_table.to_csv('new_'+file_tsv, sep="\t")         
    print("[End] - Deleted "+str(deleted) +" files, remaining "+str(totalFiles)+" files in total")  
    
def deleteFiles():
    path = './pt/clips/'
    totalFiles = len(os.listdir(path))  

    path = './pt/'
    os.chdir(path)
    audio_table = pd.read_csv(file_tsv, sep='\t', header=0, usecols=[1])
    total = len(audio_table)
    deleteRows = 0
    #if file_tsv == 'train.tsv' or file_tsv == 'test.tsv':
    #    lines = round(total*0.25)
    #if file_tsv == 'validated.tsv': 
    #    lines = round(total*0.4)

    #if file_tsv == 'train.tsv' or file_tsv == 'test.tsv' or file_tsv == 'validated.tsv':
    #    deleteRows = total-lines   
    #else:
    #    deleteRows = total
    #print("[Begin] - "+ str(deleteRows) + "/"+str(total)+" of the file | "+str(totalFiles)+" dataset files will be deleted")

    path = './clips/'
    os.chdir(path)
    deleted=0
    if file_tsv == 'train.tsv' or file_tsv == 'test.tsv' or file_tsv == 'validated.tsv':
        for _, row in audio_table.iterrows():
            if deleted >= deleteRows:
                break 
            if os.path.exists(row[0]):
                os.remove(row[0])
                deleted = deleted + 1
    else:
        for _, row in audio_table.iterrows():        
            if os.path.exists(row[0]):
                os.remove(row[0])
                deleted = deleted + 1
    totalFiles = len(os.listdir())                     
    print("[End] - Deleted "+str(deleted) +" files, remaining "+str(totalFiles)+" files in total")  

deleteFiles()    