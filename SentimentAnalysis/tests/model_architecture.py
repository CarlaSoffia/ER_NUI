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
    path = "."
else:
    typeData = "final"  
    path = "./datasets/preprocessed" 
file = path + "/ISEAR_only_text_"+data+"_"+typeData+".csv"
labels = ["anger","disgust","fear","guilt","joy","sadness","shame"]
if train:
    dataset = pd.read_csv(file,sep=";")
    version = version + 1
    model_name = "models/"+data+"_model_"+str(version)+"_"+str(batch)+"b_"+str(epochs)+"e_"+str(np_seed)+"nps_"+str(ts_seed)+"ts_"+str(dropout)[2:]+"d_"+str(test_size)[2:]+"ts_"+str(num_words)+"nw_"+typeData+"td.h5"
    saveModelArchitecture("architectures/model_"+str(version)+"_architecture.py")
    np.random.seed(np_seed)
    tf.random.set_seed(ts_seed)

    # Convert labels to numerical values
    le = LabelEncoder()
    dataset['sentiment'] = le.fit_transform(dataset['sentiment'])
    # Split the dataset into training and testing sets
    train_texts, test_texts, train_labels, test_labels = train_test_split(dataset['text'], dataset['sentiment'], test_size=test_size, random_state=np_seed)

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
      tf.keras.layers.Conv1D(filters=filter_units, kernel_size=3, activation='relu'),
      tf.keras.layers.Dropout(dropout),
      tf.keras.layers.Flatten(),
      #tf.keras.layers.Dense(units=filter_units, activation='relu'),
      #tf.keras.layers.Dropout(dropout),
      tf.keras.layers.Dense(units=7, activation='softmax')
    ])
    early_stop = EarlyStopping(monitor='val_loss', patience=patience)
    checkpoint = ModelCheckpoint(filepath=model_name, monitor='val_loss', save_best_only=True, mode='min', verbose=1)
    # Compile the model
    model.compile(optimizer='adam', loss='sparse_categorical_crossentropy', metrics=['accuracy'])

