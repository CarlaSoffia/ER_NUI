from keras.preprocessing.text import Tokenizer
from keras.preprocessing.sequence import pad_sequences
import numpy as np
from sklearn.model_selection import train_test_split
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from sklearn.preprocessing import LabelEncoder
from tensorflow.keras.callbacks import EarlyStopping
from keras.callbacks import ModelCheckpoint
import glob
import pickle

train = False
version = len(glob.glob(f"models/*.h5"))
num_words = 10000
seed = 2
test_size = 0.03
patience = 10
epochs = 100
batch = 64
model_name = "models/model_"+str(version)+"_"+str(batch)+"b_"+str(epochs)+"e_"+str(seed)+"s.h5"
labels = ["anger","disgust","fear","guilt","joy","sadness","shame"]
if train:
    dataset = pd.read_csv("ISEAR_portuguese.csv",sep=";")

    np.random.seed(seed)
    tf.random.set_seed(seed)

    # Convert labels to numerical values
    le = LabelEncoder()
    dataset['sentiment'] = le.fit_transform(dataset['sentiment'])
    # Split the dataset into training and testing sets
    train_texts, test_texts, train_labels, test_labels = train_test_split(dataset['text'], dataset['sentiment'], test_size=test_size, random_state=seed)

    # Tokenize the texts and convert them into sequences
    tokenizer = Tokenizer(num_words=num_words, oov_token='<OOV>')
    tokenizer.fit_on_texts(train_texts)
    train_sequences = tokenizer.texts_to_sequences(train_texts)
    test_sequences = tokenizer.texts_to_sequences(test_texts)

    with open('models/tokenizer.pickle', 'wb') as handle:
        pickle.dump(tokenizer, handle, protocol=pickle.HIGHEST_PROTOCOL)

    # Pad the sequences to have the same length
    train_padded = pad_sequences(train_sequences, maxlen=num_words, padding='post', truncating='post')
    test_padded = pad_sequences(test_sequences, maxlen=num_words, padding='post', truncating='post')

    # Define the model
    model = tf.keras.Sequential([
        tf.keras.layers.Embedding(input_dim=num_words, output_dim=100, input_length=num_words),
        tf.keras.layers.Conv1D(filters=64, kernel_size=5, activation='relu'),
        tf.keras.layers.GlobalMaxPooling1D(),
        tf.keras.layers.Dense(units=64, activation='relu'),
        tf.keras.layers.Dense(units=7, activation='softmax')
    ])
    early_stop = EarlyStopping(monitor='val_loss', patience=patience)
    version = version+1
    checkpoint = ModelCheckpoint(model_name=model_name, monitor='val_loss', save_best_only=True, mode='min', verbose=1)
    # Compile the model
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])

    # Train the model
    model.fit(train_padded, train_labels, epochs=epochs, batch_size=batch,validation_data=(test_padded, test_labels), callbacks=[early_stop, checkpoint])

    # Evaluate the model
    loss, accuracy = model.evaluate(test_padded, test_labels)
    print(f'Test loss: {loss:.3f}')
    print(f'Test accuracy: {accuracy:.3f}')
else:
    model = keras.models.load_model(model_name)    
    with open('models/tokenizer.pickle', 'rb') as handle:
        tokenizer = pickle.load(handle)
    text_sequence = tokenizer.texts_to_sequences(["Hoje estive a passear pela cidade e a ver o sol"])
    text_padded = pad_sequences(text_sequence, maxlen=num_words, padding='post', truncating='post')

    predictions = model.predict(text_padded)[0]
    maxVal = predictions.argmax()
    accuracy = predictions[maxVal]
    emotion = labels[maxVal]
    print(predictions, labels)
    print(emotion, accuracy)