import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
import numpy as np
import pandas as pd
from torchtext.vocab import build_vocab_from_iterator
from torchtext.vocab import Vocab
from collections import Counter, OrderedDict
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import optuna
import spacy
import pytorch_lightning as pl
import torch.nn.functional as F

class EarlyStoppingCallback(pl.Callback):
    def __init__(self, monitor='val_loss', patience=3):
        super().__init__()
        self.monitor = monitor
        self.patience = patience
        self.wait = 0
        self.stopped_epoch = 0
        self.best_score = None

    def on_validation_end(self, trainer, pl_module):
        val_loss = trainer.callback_metrics.get(self.monitor)
        if self.best_score is None:
            self.best_score = val_loss
        elif val_loss > self.best_score:
            self.wait += 1
            if self.wait >= self.patience:
                self.stopped_epoch = trainer.current_epoch
                trainer.should_stop = True
        else:
            self.best_score = val_loss
            self.wait = 0
            
np.random.seed(0)
torch.manual_seed(42)
dataset = pd.read_csv("ISEAR_only_text_pt_data_augmentation.csv", sep=";")
nlp = spacy.load("pt_core_news_lg")  # python -m spacy download pt_core_news_lg
    
# Define the hyperparameter search space
def objective(trial):
    # Define hyperparameters to tune
    num_words = trial.suggest_int("num_words", 1000, 5000)
    num_filters = trial.suggest_int("num_filters", 16, 128)
    batch = trial.suggest_int("batches", 32, 256)
    filter_size = trial.suggest_int("filter_size", 3, 7)
    learning_rate = trial.suggest_float("learning_rate", 1e-5, 1e-2)
    dropout_rate = trial.suggest_float("dropout_rate", 0.5, 0.9)
    test_size = trial.suggest_float("test_size", 0.3, 0.4)

    le = LabelEncoder()
    dataset["sentiment"] = le.fit_transform(dataset["sentiment"])
    train_texts, test_texts, train_labels, test_labels = train_test_split(
        dataset["text"], dataset["sentiment"], test_size=test_size, random_state=0
    )
    
    # Define a tokenizer function
    tokenizer = lambda x: x.split()

    # Define a function that yields tokens from the dataset iterator
    def yield_tokens(data_iter):
        for text in data_iter:
            yield tokenizer(text)

    # Build the vocabulary using the iterator
    vocab = build_vocab_from_iterator(yield_tokens(train_texts), min_freq=1, specials=['<pad>', '<unk>'])
    # Get the ordered list of tokens by frequency 
    counter = Counter(vocab.get_itos())
    sorted_by_freq_tuples = sorted(counter.items(), key=lambda x: x[1], reverse=True)

    # Select the top 'num_words' tokens
    top_tokens = sorted_by_freq_tuples[:num_words]
    # Build the final vocabulary using the selected tokens
    vocab = Vocab(OrderedDict([(token, freq) for token, freq in top_tokens]))
    # Tokenize and pad the sequences
    def tokenize_and_pad(texts):
        tokenized_texts = [tokenizer(text) for text in texts]
        max_len = max(len(t) for t in tokenized_texts)
        padded_texts = [t + ["<pad>"] * (max_len - len(t)) for t in tokenized_texts]
        return padded_texts

    train_padded = tokenize_and_pad(train_texts)
    test_padded = tokenize_and_pad(test_texts)
    
    # Convert tokens to their corresponding indices in the vocabulary
    def numericalize(texts):
        return [[vocab[token] if token in vocab else vocab['<unk>'] for token in text] for text in texts]

    train_sequences = numericalize(train_padded)
    test_sequences = numericalize(test_padded)

    train_labels = np.array(train_labels)
    test_labels = np.array(test_labels)

    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    train_sequences, test_sequences = torch.tensor(
        train_sequences, dtype=torch.long
    ).to(device), torch.tensor(test_sequences, dtype=torch.long).to(device)
    train_labels, test_labels = torch.tensor(train_labels, dtype=torch.long).to(
        device
    ), torch.tensor(test_labels, dtype=torch.long).to(device)

    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    train_sequences, test_sequences = train_sequences.to(device), test_sequences.to(
        device
    )
    train_labels, test_labels = torch.tensor(train_labels).to(device), torch.tensor(
        test_labels
    ).to(device)

    # Define the model
    class SentimentClassifier(pl.LightningModule):
        def __init__(self, num_filters, filter_size, dropout_rate, output_dim=100):
            super(SentimentClassifier, self).__init__()
            self.embedding = nn.Embedding(output_dim, output_dim)
            self.conv1d = nn.Conv1d(output_dim, num_filters, filter_size)
            self.dropout = nn.Dropout(dropout_rate)
            self.fc = nn.Linear(num_filters, 7)

        def forward(self, x):
            x = self.embedding(x)
            x = x.transpose(1, 2)
            x = self.conv1d(x)
            x = F.relu(x)
            x = nn.AdaptiveMaxPool1d(1)(x)
            x = x.squeeze(2)
            x = self.dropout(x)
            x = torch.flatten(x, start_dim=1)
            x = self.fc(x)
            return x

        def training_step(self, batch, batch_idx):
            inputs, labels = batch
            outputs = self(inputs)
            loss = F.cross_entropy(outputs, labels)
            self.log('train_loss', loss)
            return loss

        def validation_step(self, batch, batch_idx):
            inputs, labels = batch
            outputs = self(inputs)
            loss = F.cross_entropy(outputs, labels)
            acc = (outputs.argmax(dim=1) == labels).float().mean()
            self.log('val_loss', loss)
            self.log('val_acc', acc)

        def test_step(self, batch, batch_idx):
            inputs, labels = batch
            outputs = self(inputs)
            loss = F.cross_entropy(outputs, labels)
            acc = (outputs.argmax(dim=1) == labels).float().mean()
            self.log('test_loss', loss)
            self.log('test_acc', acc)

        def configure_optimizers(self):
            optimizer = torch.optim.Adam(self.parameters(), lr=learning_rate)
            return optimizer

    model = SentimentClassifier(num_filters, filter_size, dropout_rate, output_dim=100)

    # Define the dataloaders
    train_dataset = torch.utils.data.TensorDataset(train_sequences, train_labels)
    test_dataset = torch.utils.data.TensorDataset(test_sequences, test_labels)
    train_loader = DataLoader(train_dataset, batch_size=batch, shuffle=True)
    test_loader = DataLoader(test_dataset, batch_size=batch, shuffle=False)

    # Train the model
    model = model.to(device)
    early_stop_callback = EarlyStoppingCallback(monitor='val_loss', patience=3)

    trainer = pl.Trainer(max_epochs = 50, callbacks=[early_stop_callback])
    trainer.fit(model, train_loader, test_loader)
    # Return the accuracy as the score for Optuna to optimize
    return trainer.callback_metrics['val_acc']

def print_best_params(study, trial):
    with open("optuna_results.txt", "a") as file:
        file.write(
            f"[Trial {trial.number}] Accuracy: {study.best_value} | Best parameters - {trial.params}\n"
        )

# Define the optuna study object and start the optimization process
study = optuna.create_study(direction="maximize")
study.optimize(objective, n_trials=20, timeout=3600, callbacks=[print_best_params])

print(f"Best hyperparameters: {study.best_params}")
print(f"Accuracy: {study.best_value}")