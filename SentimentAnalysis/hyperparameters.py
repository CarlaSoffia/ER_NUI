import glob
import optuna as optuna
import pickle
import numpy as np
import pandas as pd
import tensorflow as tf
from keras.callbacks import ModelCheckpoint
from keras.preprocessing.text import Tokenizer
from sklearn.preprocessing import LabelEncoder
from tensorflow.keras.callbacks import EarlyStopping
from sklearn.model_selection import train_test_split
from keras.preprocessing.sequence import pad_sequences


version = len(glob.glob(f"models/*.h5"))+1
file = "ISEAR_only_text_pt_data_augmentation.csv"
labels = ["anger","disgust","fear","guilt","joy","sadness","shame"]
np.random.seed(0)
tf.random.set_seed(42)
dataset = pd.read_csv(file,sep=";")

# Define the hyperparameter search space
def objective(trial):
    # Define hyperparameters to tune
    num_words = trial.suggest_int('num_words', 1000, 10000)
    num_filters = trial.suggest_int('num_filters', 16, 128)
    batch = trial.suggest_int('batches', 32, 128)
    filter_size = trial.suggest_int('filter_size', 3, 7)
    learning_rate = trial.suggest_loguniform('learning_rate', 1e-5, 1e-2)
    dropout_rate = trial.suggest_uniform('dropout_rate', 0.5, 0.9)
    
    le = LabelEncoder()
    dataset['sentiment'] = le.fit_transform(dataset['sentiment'])
    train_texts, test_texts, train_labels, test_labels = train_test_split(dataset['text'], dataset['sentiment'], test_size=0.3, random_state=0)

    # Tokenize the texts and convert them into sequences
    tokenizer = Tokenizer(num_words=num_words, oov_token='<OOV>')
    tokenizer.fit_on_texts(train_texts)
    train_sequences = tokenizer.texts_to_sequences(train_texts)
    test_sequences = tokenizer.texts_to_sequences(test_texts)

    # Pad the sequences to have the same length
    train_padded = pad_sequences(train_sequences, maxlen=num_words, padding='post', truncating='post')
    test_padded = pad_sequences(test_sequences, maxlen=num_words, padding='post', truncating='post')


    # Define the model
    model = tf.keras.Sequential([
      tf.keras.layers.Embedding(input_dim=num_words, output_dim=100, input_length=num_words),
      tf.keras.layers.Conv1D(filters=num_filters, kernel_size=filter_size, activation='relu'),
      tf.keras.layers.Dropout(dropout_rate),
      tf.keras.layers.Flatten(),
      tf.keras.layers.Dense(units=7, activation='softmax')
    ])
    
    # Compile the model
    optimizer = tf.keras.optimizers.Adam(learning_rate=learning_rate)
    model.compile(optimizer=optimizer, loss='sparse_categorical_crossentropy', metrics=['accuracy'])
    early_stop = EarlyStopping(monitor='val_loss', patience=5)

    # Train the model
    history = model.fit(train_padded, train_labels, epochs=30, batch_size=batch, validation_data=(test_padded, test_labels), callbacks=[early_stop])
    # Evaluate the model
    val_loss = history.history['val_loss'][-1]

    # Return the validation loss as the score for Optuna to optimize
    return val_loss

# Define the optuna study object and start the optimization process
study = optuna.create_study(direction='minimize')
study.optimize(objective, n_trials=20, timeout=3600)

print(f'Best hyperparameters: {study.best_params}')
print(f'Mean squared error: {study.best_value}')
