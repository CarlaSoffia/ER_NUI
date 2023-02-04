# convert mp3 files to wav
import os
from os import path
from pydub import AudioSegment
from tqdm import tqdm  
# assign files
os.chdir("dataset/clips")  
print(os.getcwd() )
audioFiles = os.listdir()
for audio in tqdm(audioFiles):
    fileName = audio.split('.')[0] + ".wav"    
    # convert mp3 file to wav file
    sound = AudioSegment.from_mp3(audio)
    sound.export(fileName, format="wav")
print("[Completed] - converted "+ str(len(audioFiles)) +" files from mp3 to wav")    