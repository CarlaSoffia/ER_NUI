import speech_recognition as sr
import sys

r=sr.Recognizer()

def getMicrophoneAudio():
    mic = sr.Microphone(device_index=sr.Microphone.list_microphone_names().index('Conjunto de microfones (Realtek'))
    with mic as source:
        r.adjust_for_ambient_noise(source,duration=1)
        print("Speak: ")
        audio = r.listen(source, timeout=30)
    return audio 
       
def SpeechTexticrophone():
    try:
        if sys.argv[2] == '--google':
            print("Google: ", r.recognize_google(audio))
        if sys.argv[2] == '--sphinx':
            print("Sphinx: ", r.recognize_sphinx(audio))
    except sr.UnknownValueError:
        print("Could not understand audio")
    except sr.RequestError as e:
        print("Error; {0}".format(e))


def SpeechTextFile():
    file = sr.AudioFile('audio_example.wav')
    with file as source:
        audio = r.listen(source)
        text = r.recognize_google(audio)
        print(text)  

def main():
    if len(sys.argv) < 1 or len(sys.argv) > 4:
        print("\tMissing parameter!\n\tAvailable options: -l live audio | -f file audio | --google | --sphinx")
        exit()
    if sys.argv[1] != '-l' and sys.argv[1] != '-f' and sys.argv[2] != '-google' and sys.argv[2] != '-sphinx' :
        print("\tInvalid parameter!\n\tAvailable options: -l live audio | -f file audio | --google | --sphinx")
        exit()
    if sys.argv[1] == '-l':
        audio = getMicrophoneAudio()
        SpeechTexticrophone()
    else:
        SpeechTextFile()     
main()
