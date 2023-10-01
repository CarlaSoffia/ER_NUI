# Importing the required libraries
import re
import numpy as np
import pandas as pd
from sklearn.preprocessing import OneHotEncoder
from keras.models import Sequential,Model
from keras.layers import Dense,Bidirectional
from tensorflow.keras.preprocessing.sequence import pad_sequences
from keras.layers import *


df=pd.read_csv('eng_dataset.csv')
df.head()

Sentences = df['content']
Sentiments = df['sentiment']

len(Sentences), len(Sentiments)

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

Glove = load_glove_embeddings(glove)

def cosine_similarity(a, b):
    """
    Computes the cosine similarity between two vectors a and b.
    """
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

Gunjan = Glove['gunjan']
Pawan = Glove['pawan']
Kumar = Glove['kumar']

cosine_similarity(Pawan, Gunjan)

def preprocess(Sentences):
    sentences = tf.strings.substr(Sentences, 0, 300)
    sentences = tf.strings.regex_replace(sentences, b"<br\\s*/?>", b" ")
    sentences = tf.strings.regex_replace(sentences, b"[^a-zA-Z']", b" ")
    sentences = tf.strings.split(sentences)
    sentences = tf.strings.lower(sentences)
    sentences = sentences.to_tensor(default_value=b"<pad>")
    return sentences

sentences = preprocess(Sentences)
sentences.shape


def encoding(sentences, Glove):
    Encoded_vec = []
    for sentence in sentences:
        sent_vec = []
        for token in sentence:
            token = token.numpy().decode('utf-8')
            if token in Glove:
                sent_vec.append(Glove[token])
            else:
                sent_vec.append(np.zeros(50))
        Encoded_vec.append(sent_vec)
    return Encoded_vec

Encoded_vec = encoding(sentences, Glove)
X = np.array(Encoded_vec)
print(X.shape)

# Perform one-hot encoding on df[0] i.e emotion
enc = OneHotEncoder(handle_unknown='ignore')
Y = enc.fit_transform(np.array(Sentiments).reshape(-1,1)).toarray()
print(Y.shape)

# Split into train and test
from keras.layers import Embedding
from sklearn.model_selection import train_test_split
X_train, X_test, Y_train, Y_test = train_test_split(X, Y, test_size=0.2, random_state=23)

#Defining the BiLSTM Model
class BiLSTMModel:
    def __init__(self):
        self.model = Sequential()
        self.model.add(Bidirectional(LSTM(100, input_shape=(100, 50))))
        self.model.add(Dropout(0.2))
        self.model.add(Dense(4, activation='softmax'))
        self.model.compile(optimizer='Adam', loss='categorical_crossentropy', metrics=['accuracy'])

    def fit(self, X, Y, epochs, batch_size):
        self.model.fit(X, Y, epochs=epochs, batch_size=batch_size)

    def evaluate(self, X, Y, batch_size):
        return self.model.evaluate(X, Y, batch_size=batch_size)

    def predict(self, X):
        return self.model.predict(X)
    
# create an instance of the BiLSTMModel class
model = BiLSTMModel()

# fit the model on the input and target data
model.fit(X_train,Y_train, epochs=20, batch_size=64)

model.model.summary()

from keras.utils.vis_utils import plot_model
plot_model(model.model, to_file='model_plot.png', show_shapes=True, show_layer_names=True)


Loss, acc = model.evaluate(X_test, Y_test, batch_size=64)
print("Loss: %.2f" % (Loss))
print("acc: %.2f" % (acc))

#First, initialize it.
twt = ['Nothing is more relentless than a dog begging for food']
#Next, tokenize it.
Twt = preprocess(twt)

# Encoding
Twt = encoding(Twt, Glove)
Twt = np.array(Twt)
print(Twt.shape)
#Predict the sentiment by passing the sentence to the model we built.
sentiment = model.predict(Twt)[0]
label = np.argmax(sentiment)
enc.categories_[0][label]