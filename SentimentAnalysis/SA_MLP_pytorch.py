import torch
import torch.nn as nn
from torch.utils.data import DataLoader
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import pytorch_lightning as pl
import torch.nn.functional as F
import logging
#from keras.preprocessing.text import Tokenizer
#from keras.utils import pad_sequences
import spacy
from torchtext.data import get_tokenizer

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

class SentimentClassifier(pl.LightningModule):
    def __init__(self):
        super(SentimentClassifier, self).__init__()
        self.embedding = nn.Embedding(14277, 100)
        #self.conv1d = nn.Conv1d(100, 16, kernel_size=3)
        #self.conv1d_2 = nn.Conv1d(16, 32, kernel_size=3)
        #self.max_pooling_1 = nn.MaxPool1d(kernel_size=2)
        #self.max_pooling_2 = nn.MaxPool1d(kernel_size=2)
        #self.fc = nn.Linear(1408, 128)
        #self.relu = nn.ReLU()
        #self.output = nn.Linear(128, 7)
        self.fc1 = nn.Linear(14277, 128)
        self.relu = nn.ReLU()
        self.fc2 = nn.Linear(128, 7)
    def forward(self, x):
        embedded = self.embedding(x)  # Shape: (batch_size, sequence_length, embedding_dim)
        embedded = embedded.permute(0, 2, 1)  # Reshape for Conv1d input: (batch_size, embedding_dim, sequence_length)
        
        #conv1_out = self.relu(self.conv1d(embedded))  # Shape: (batch_size, num_filters, sequence_length - 2)
        #pooling_out = self.max_pooling_1(conv1_out)
        #conv2_out = self.relu(self.conv1d_2(pooling_out))  # Shape: (batch_size, num_filters, sequence_length - 4)
        #pooled = self.relu(pooling_out)
        #pooled = self.max_pooling_2(conv2_out)  # Shape: (batch_size, num_filters, (sequence_length - 4)/2)
        #pooled = pooled.view(pooling_out.size(0), -1)  # Flatten the tensor
        #hidden = self.relu(self.fc(pooled))  # Shape: (batch_size, hidden_dim)
        #output = self.output(hidden)  # Shape: (batch_size, output_dim)
        out = self.fc1(x)
        out = self.relu(out)
        out = self.fc2(out)
        return out

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
        optimizer = torch.optim.Adam(self.parameters(), lr=0.001)
        lr_scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(optimizer, mode='min', factor=0.01, patience=5, verbose=True, min_lr=0.000001)
        monitor_metric = 'val_loss'  # replace this with the name of the metric you want to monitor
        return {
            'optimizer': optimizer,
            'lr_scheduler': lr_scheduler,
            'monitor': monitor_metric
        }


# configure logging at the root level of Lightning
logging.getLogger("lightning.pytorch").setLevel(logging.DEBUG)

logger = logging.getLogger("lightning.pytorch.core")
logger.addHandler(logging.FileHandler("logs2.log"))
np.random.seed(0)
torch.manual_seed(42)
dataset = pd.read_csv("ISEAR_only_text_pt_data_augmentation.csv", sep=";")

le = LabelEncoder()
dataset['sentiment'] = le.fit_transform(dataset['sentiment'])
train_texts, test_texts, train_labels, test_labels = train_test_split(dataset['text'], dataset['sentiment'], test_size=test_size, random_state=0)

train_labels = np.array(train_labels)
test_labels = np.array(test_labels)

#spacy.cli.download("pt_core_news_sm")

nlp = spacy.load("pt_core_news_sm")

# Create a vocabulary of tokens
vocab = set()
for text in train_texts:
    doc = nlp(text)
    tokens = [token.text for token in doc]
    vocab.update(tokens)

for text in test_texts:
    doc = nlp(text)
    tokens = [token.text for token in doc]
    vocab.update(tokens)
# Assign unique indices to tokens
token2index = {token: i for i, token in enumerate(vocab)}

# Convert tokens to indices
train_sequences = [
    torch.tensor([token2index[token.text] for token in nlp(text)], dtype=torch.long)
    for text in train_texts
]
test_sequences = [
    torch.tensor([token2index[token.text] for token in nlp(text)], dtype=torch.long)
    for text in test_texts
]

train_sequences = torch.nn.utils.rnn.pad_sequence(train_sequences, batch_first=True)
test_sequences = torch.nn.utils.rnn.pad_sequence(test_sequences, batch_first=True)

device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
train_sequences = torch.tensor(train_sequences, dtype=torch.long).clone().detach().to(device)
test_sequences = torch.tensor(test_sequences, dtype=torch.long).clone().detach().to(device)

train_labels = torch.tensor(train_labels, dtype=torch.long).clone().detach().to(device)
test_labels = torch.tensor(test_labels, dtype=torch.long).clone().detach().to(device)
model = SentimentClassifier()

# Define the dataloaders
train_dataset = torch.utils.data.TensorDataset(train_sequences, train_labels)
test_dataset = torch.utils.data.TensorDataset(test_sequences, test_labels)

train_loader = DataLoader(train_dataset, batch_size=batch, shuffle=False, pin_memory=True)
test_loader = DataLoader(test_dataset, batch_size=batch, shuffle=False, pin_memory=True)

# Train the model
model = model.to(device)
early_stop_callback = EarlyStoppingCallback(monitor='val_loss', patience=10)

trainer = pl.Trainer(max_epochs = 100, callbacks=[early_stop_callback],log_every_n_steps=1)
trainer.fit(model, train_loader, test_loader)    

torch.save(model.state_dict(), ".\models_pytorch\model_0.pt")
