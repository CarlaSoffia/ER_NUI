import re
import numpy as np
import pandas as pd
from sklearn.preprocessing import OneHotEncoder
from keras.models import Sequential,Model
from keras.layers import Dense,Bidirectional,Dropout,LSTM
from tensorflow.keras.preprocessing.sequence import pad_sequences
from keras.layers import *
from keras.layers import Embedding
from sklearn.model_selection import train_test_split
from unidecode import unidecode
from keras.regularizers import l2
glove ='glove.6B.50d.txt'

def load_glove_embeddings(path):
    embeddings_index = {}
    with open(path, 'r', encoding='utf8') as f:
        for line in f:
            values = line.split()
            word = values[0]
            coefs = np.asarray(values[1:], dtype='float32')
            embeddings_index[word] = coefs
    return embeddings_index

def cosine_similarity(a, b):
    """
    Computes the cosine similarity between two vectors a and b.
    """
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))
import unicodedata
# Function to remove accents from characters
def remove_accents(text):
    return ''.join(c for c in unicodedata.normalize('NFD', text) if unicodedata.category(c) != 'Mn')

def remove_accents(text):
    return unidecode(text)

def preprocess(Sentences):
    series_without_accents = Sentences.apply(remove_accents)
    sentences = tf.strings.substr(series_without_accents, 0, 500)
    sentences = tf.strings.regex_replace(sentences, b"[^a-zA-Z']", b" ")
    sentences = tf.strings.split(sentences)
    sentences = tf.strings.strip(sentences)
    sentences = tf.strings.lower(sentences)
    sentences = sentences.to_tensor(default_value=b"<pad>")
    return sentences

def encoding(sentences, Glove):
    Encoded_vec = []
    for sentence in sentences:
        sent_vec = []
        for token in sentence:
            token = token.numpy().decode('utf8')
            if token in Glove:
                sent_vec.append(Glove[token])
            else:
                sent_vec.append(np.zeros(50))
        Encoded_vec.append(sent_vec)
    return Encoded_vec

#Defining the BiLSTM Model
class BiLSTMModel:
    def __init__(self):
        self.model = Sequential()
        self.model.add(Bidirectional(LSTM(50, input_shape=(100, 50), kernel_regularizer=l2(0.01))))
        self.model.add(Dropout(0.3))
        self.model.add(Dense(7, activation='softmax'))
        self.model.compile(optimizer='Adam', loss='categorical_crossentropy', metrics=['accuracy'])

    def fit(self, X, Y, epochs, batch_size):
        self.model.fit(X, Y, epochs=epochs, batch_size=batch_size)

    def evaluate(self, X, Y, batch_size):
        return self.model.evaluate(X, Y, batch_size=batch_size)

    def predict(self, X):
        return self.model.predict(X)
    
Glove = load_glove_embeddings(glove)

df=pd.read_csv('ISEAR_only_text_pt_data_augmentation.csv', sep=";")
df.head()

Sentences = df['text']
Sentiments = df['sentiment']

sentences = preprocess(Sentences)

Encoded_vec = encoding(sentences, Glove)
X = np.array(Encoded_vec)

enc = OneHotEncoder(handle_unknown='ignore')
Y = enc.fit_transform(np.array(Sentiments).reshape(-1,1)).toarray()

X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.3, random_state=23)

model = BiLSTMModel()

# fit the model on the input and target data
print(X_train.shape)
print(Y_train.shape)
exit()
model.fit(X_train,Y_train, epochs=100, batch_size=32)

model.model.summary()

Loss, acc = model.evaluate(X_test, Y_test, batch_size=32)
print("Loss: %.2f" % (Loss))
print("acc: %.2f" % (acc))