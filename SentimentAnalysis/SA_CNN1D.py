from keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
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
import matplotlib.pyplot as plt
import string
# def saveModelArchitecture(file):
#     start_line = 33
#     end_line = 95

#     # Open the source file for reading
#     with open(__file__, 'r') as f:
#         # Read all the lines of the source file
#         lines = f.readlines()

#     # Extract the desired lines
#     new_lines = lines[start_line-1:end_line]

#     # Open a new file for writing
#     with open(file, 'w') as f:
#         # Write the extracted lines to the new file
#         for line in new_lines:
#             f.write(line)

# modelo 2 - Test loss: 1.206/Test accuracy: 0.610

train = True
version = len(glob.glob(f"models/*.h5"))
num_words = 5000
np_seed = 0
ts_seed = 42
test_size = 0.3
patience = 5
epochs = 100
batch = 128
dropout = 0.8
filter_units = 32
data = "pt"
data_augmentation = True
if data_augmentation:
    typeData = "data_augmentation"
    path = "./"
else:
    typeData = "final"
    path = "./datasets"
file = path + "/ISEAR_only_text_"+data+"_"+typeData+".csv"
labels = ["anger","disgust","fear","guilt","joy","sadness","shame"]
if train:
    dataset = pd.read_csv(file,sep=";")
    version = version + 1
    model_name = "models/"+data+"_model_"+str(version)+"_"+str(batch)+"b_"+str(epochs)+"e_"+str(np_seed)+"nps_"+str(ts_seed)+"ts_"+str(dropout)[2:]+"d_"+str(test_size)[2:]+"ts_"+str(num_words)+"nw_"+typeData+"td.h5"
    #saveModelArchitecture("architectures/model_"+str(version)+"_architecture.py")
    np.random.seed(np_seed)
    tf.random.set_seed(ts_seed)

    # Convert labels to numerical values
    le = LabelEncoder()
    dataset['sentiment'] = le.fit_transform(dataset['sentiment'])
    # Split the dataset into training and testing sets
    train_texts, test_texts, train_labels, test_labels = train_test_split(dataset['text'], dataset['sentiment'], test_size=test_size, random_state=np_seed)

    def preprocess_texts(texts):
        processed_texts = []
        for text in texts:
            # Remove punctuation
            text = text.translate(str.maketrans("", "", string.punctuation))
            
            # Convert to lowercase
            text = text.lower()
            
            processed_texts.append(text)
        
        return processed_texts
    
    train_texts_processed  = preprocess_texts(train_texts)
    test_texts_processed = preprocess_texts(test_texts)
    
    # Tokenize the texts and convert them into sequences
    tokenizer = Tokenizer(num_words=num_words, oov_token='<OOV>')
    tokenizer.fit_on_texts(train_texts)
    train_sequences = tokenizer.texts_to_sequences(train_texts_processed)
    test_sequences = tokenizer.texts_to_sequences(test_texts_processed)

    with open('models/tokenizer.pickle', 'wb') as handle:
        pickle.dump(tokenizer, handle, protocol=pickle.HIGHEST_PROTOCOL)

    # Pad the sequences to have the same length
    train_padded = pad_sequences(train_sequences, maxlen=num_words, padding='post', truncating='post')
    test_padded = pad_sequences(test_sequences, maxlen=num_words, padding='post', truncating='post')

    
    # Define the model
    model = tf.keras.Sequential([
      tf.keras.layers.Embedding(input_dim=num_words, output_dim=100, input_length=num_words),
      tf.keras.layers.Conv1D(filters=filter_units, kernel_size=3, activation='relu'),
      tf.keras.layers.Dropout(dropout),
      tf.keras.layers.Flatten(),
      tf.keras.layers.Dense(units=7, activation='softmax')
    ])
    early_stop = EarlyStopping(monitor='val_loss', patience=patience)
    checkpoint = ModelCheckpoint(filepath=model_name, monitor='val_loss', save_best_only=True, mode='min', verbose=1)
    # Compile the model
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])

    # Train the model
    history = model.fit(train_padded, train_labels, epochs=epochs, batch_size=batch,validation_data=(test_padded, test_labels), callbacks=[early_stop, checkpoint])

    # Evaluate the model
    loss, accuracy = model.evaluate(test_padded, test_labels)
    print(f'Test loss: {loss:.3f}')
    print(f'Test accuracy: {accuracy:.3f}')

    AccPNG = "graphics/accuracy_model_"+str(version)+"_"+str(batch)+"b_"+str(epochs)+"e_"+str(np_seed)+"nps_"+str(ts_seed)+"ts_"+"s_"+str(dropout)[2:]+"d_"+str(test_size)[2:]+"ts_"+str(num_words)+"_"+data+"_"+typeData+".png"
    LossPNG = "graphics/loss_model_"+str(version)+"_"+str(batch)+"b_"+str(epochs)+"e_"+str(np_seed)+"nps_"+str(ts_seed)+"ts_"+"s_"+str(dropout)[2:]+"d_"+str(test_size)[2:]+"ts_"+str(num_words)+"_"+data+"_"+typeData+".png"
    # Plot training and validation accuracy
    plt.plot(history.history['accuracy'])
    plt.plot(history.history['val_accuracy'])
    plt.title('Model Accuracy')
    plt.ylabel(f'Accuracy - test accuracy: {accuracy:.3f}')
    plt.xlabel('Epoch')
    plt.legend(['train', 'val'], loc='upper left')
    #plt.show()
    plt.savefig(AccPNG)
    # Plot training and validation loss
    fig, ax = plt.subplots()
    ax.plot(history.history['loss'], label='Training Loss')
    ax.plot(history.history['val_loss'], label='Validation Loss')
    ax.set_title('Training and Validation Loss')
    ax.set_xlabel('Epoch')
    ax.set_ylabel(f'Loss - test loss: {loss:.3f}')
    ax.legend()
    #plt.show()
    plt.savefig(LossPNG)
else:
    model_name = "models/_"+data+"_model_"+str(version)+"_"+str(batch)+"b_"+str(epochs)+"e_"+str(seed)+"s.h5"    
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