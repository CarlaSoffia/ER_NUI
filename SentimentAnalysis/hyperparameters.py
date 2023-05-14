import torch
import torch.nn as nn
from torch.utils.data import DataLoader
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import optuna
import pytorch_lightning as pl
import torch.nn.functional as F
from keras.preprocessing.text import Tokenizer
from keras.utils import pad_sequences
import logging
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

# configure logging at the root level of Lightning
logging.getLogger("lightning.pytorch").setLevel(logging.DEBUG)

logger = logging.getLogger("lightning.pytorch.core")
 
# Define the hyperparameter search space
def objective(trial):
    # Define hyperparameters to tune
    file = logging.FileHandler(f"trials_epochs/trial_{trial.number}.log")
    logger.addHandler(file)  
    num_words = trial.suggest_categorical("num_words", [1000, 2000, 3000, 4000, 5000])
    batches = trial.suggest_categorical("batches", [ 32, 64, 128])
    kernel_size = trial.suggest_int("kernel_size", 2, 5)
    filter_units = trial.suggest_categorical("filter_units", [16, 32, 64])
    learning_rate = trial.suggest_float("learning_rate", 0.001, 0.1)
    dropout_rate = trial.suggest_categorical("dropout_rate", [0.4, 0.5, 0.6])
    test_size = trial.suggest_categorical("test_size", [0.3, 0.35, 0.4])
    
    le = LabelEncoder()
    dataset["sentiment"] = le.fit_transform(dataset["sentiment"])
    train_texts, test_texts, train_labels, test_labels = train_test_split(
        dataset["text"], dataset["sentiment"], test_size=test_size, random_state=0
    )
    
    train_labels = np.array(train_labels)
    test_labels = np.array(test_labels)

    tokenizer = Tokenizer(num_words=num_words, oov_token='<OOV>')
    tokenizer.fit_on_texts(train_texts)
    train_sequences = tokenizer.texts_to_sequences(train_texts)
    test_sequences = tokenizer.texts_to_sequences(test_texts)

    # Pad the sequences to have the same length
    train_sequences = pad_sequences(train_sequences, maxlen=num_words, padding='post', truncating='post')
    test_sequences = pad_sequences(test_sequences, maxlen=num_words, padding='post', truncating='post')

    device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
    train_sequences = torch.tensor(train_sequences, dtype=torch.long).clone().detach().to(device)
    test_sequences = torch.tensor(test_sequences, dtype=torch.long).clone().detach().to(device)
    train_labels = torch.tensor(train_labels, dtype=torch.long).clone().detach().to(device)
    test_labels = torch.tensor(test_labels, dtype=torch.long).clone().detach().to(device)

    class SentimentClassifier(pl.LightningModule):
        def __init__(self, num_words, filter_units, kernel_size, dropout_rate, output_dim=100):
            super(SentimentClassifier, self).__init__()
            self.embedding = nn.Embedding(num_words, output_dim)
            self.conv1d = nn.Conv1d(in_channels=output_dim, out_channels=filter_units, kernel_size=kernel_size)
            self.flatten = nn.Flatten()
            self.dropout = nn.Dropout(dropout_rate)
            self.fc = nn.Linear(filter_units, 7)
        def forward(self, x):
            x = self.embedding(x)
            x = x.transpose(1, 2)
            x = self.conv1d(x)
            x = F.relu(x)
            x = nn.AdaptiveMaxPool1d(1)(x)
            x = x.squeeze(2)
            x = torch.flatten(x, start_dim=1)
            x = self.dropout(x)
            x = self.fc(x)
            x = F.softmax(x, dim=1) 
            return x

        def training_step(self, batch, batch_idx):
            inputs, labels = batch
            outputs = self(inputs)
            loss = F.cross_entropy(outputs, labels)
            acc = (outputs.argmax(dim=1) == labels).float().mean()
            self.log('train_loss', loss)
            self.log('train_acc', acc)
            return loss

        def validation_step(self, batch, batch_idx):
            inputs, labels = batch
            outputs = self(inputs)
            loss = F.cross_entropy(outputs, labels)
            acc = (outputs.argmax(dim=1) == labels).float().mean()
            self.log('val_loss', loss)
            self.log('val_acc', acc)
            return loss
        def test_step(self, batch, batch_idx):
            inputs, labels = batch
            outputs = self(inputs)
            loss = F.cross_entropy(outputs, labels)
            acc = (outputs.argmax(dim=1) == labels).float().mean()
            self.log('test_loss', loss)
            self.log('test_acc', acc)
            return loss
        def on_train_epoch_end(self):
            train_loss = self.trainer.callback_metrics['train_loss']
            train_acc = self.trainer.callback_metrics['train_acc']
            val_loss = self.trainer.callback_metrics['val_loss']
            val_acc = self.trainer.callback_metrics['val_acc']
            logger.debug(f'[Epoch {self.current_epoch}] - Training results: train_loss={train_loss:.4f}, train_acc={train_acc:.4f}')
            logger.debug(f'[Epoch {self.current_epoch}] - Validation results: val_loss={val_loss:.4f}, val_acc={val_acc:.4f}')
        def configure_optimizers(self):
            optimizer = torch.optim.Adam(self.parameters(), lr=learning_rate)
            lr_scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(optimizer, mode='max', factor=0.01, patience=2, verbose=True, min_lr=1e-7)
            monitor_metric = 'val_loss'  # replace this with the name of the metric you want to monitor
            return {
                'optimizer': optimizer,
                'lr_scheduler': lr_scheduler,
                'monitor': monitor_metric
            }
    
    model = SentimentClassifier(num_words, filter_units, kernel_size, dropout_rate, output_dim=100)

    # Define the dataloaders
    train_dataset = torch.utils.data.TensorDataset(train_sequences, train_labels)
    test_dataset = torch.utils.data.TensorDataset(test_sequences, test_labels)
    train_loader = DataLoader(train_dataset, batch_size=batches, shuffle=True, pin_memory=True)
    test_loader = DataLoader(test_dataset, batch_size=batches, shuffle=False, pin_memory=True)

    # Train the model
    model = model.to(device)
    early_stop_callback = EarlyStoppingCallback(monitor='val_loss', patience=10)

    trainer = pl.Trainer(max_epochs = 100, callbacks=[early_stop_callback])
    trainer.fit(model, train_loader, test_loader)
    val_loss = trainer.callback_metrics['val_loss']
    trainer = pl.Trainer()
    logger.removeHandler(file)  
    torch.cuda.empty_cache()
    # Return the accuracy as the score for Optuna to optimize
    return val_loss

def print_best_params(study, trial):
    with open("optuna_results.txt", "a") as file:
        file.write(
            f"[Trial {trial.number}] Validation loss: {trial.value} | Best parameters - {trial.params}\n"
        )

# Define the optuna study object and start the optimization process
study = optuna.create_study(direction="minimize")
study.optimize(objective, n_trials=100, callbacks=[print_best_params])

print(f"Best hyperparameters: {study.best_params}")
print(f"Best trial: {study.best_trial} - Validation loss: {study.best_value}")