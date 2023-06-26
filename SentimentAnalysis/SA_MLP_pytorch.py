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
import spacy
from torchtext.data import get_tokenizer
import string
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
    def __init__(self, vocab_size, embedding_dim, conv_embedding_dim, n_filters, filter_sizes, output_dim, dropout_rate):
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, embedding_dim)
        self.convs = nn.ModuleList([nn.Conv1d(conv_embedding, 
                                              n_filter, 
                                              filter_size) 
                                    for n_filter, filter_size, conv_embedding in zip(n_filters, filter_sizes, conv_embedding_dim)])
        self.fc = nn.Linear(len(filter_sizes) * n_filters[-1], output_dim)
        self.dropout = nn.Dropout(dropout_rate)
        
    def forward(self, ids):
        # ids = [batch size, seq len]
        embedded = self.dropout(self.embedding(ids))
        # embedded = [batch size, seq len, embedding dim]
        embedded = embedded.permute(0,2,1)
        # embedded = [batch size, embedding dim, seq len]
        conved = [torch.relu(conv(embedded)) for conv in self.convs]
        # conved_n = [batch size, n filters, seq len - filter_sizes[n] + 1]
        pooled = [conv.max(dim=-1).values for conv in conved]
        # pooled_n = [batch size, n filters]
        cat = self.dropout(torch.cat(pooled, dim=-1))
        # cat = [batch size, n filters * len(filter_sizes)]
        prediction = self.fc(cat)
        # prediction = [batch size, output dim]
        return prediction
        
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
        optimizer = torch.optim.Adam(self.parameters())
        lr_scheduler = torch.optim.lr_scheduler.ReduceLROnPlateau(optimizer, mode='min', factor=0.1, patience=3)
        monitor_metric = 'val_loss'  # replace this with the name of the metric you want to monitor
        return {
            'optimizer': optimizer,
            'lr_scheduler': lr_scheduler,
            'monitor': monitor_metric
        }

def initialize_weights(m):
    if isinstance(m, nn.Linear):
        nn.init.xavier_normal_(m.weight)
        nn.init.zeros_(m.bias)
    elif isinstance(m, nn.Conv1d):
        nn.init.kaiming_normal_(m.weight, nonlinearity='relu')
        nn.init.zeros_(m.bias)

embedding_dim = 300
conv_embedding_dim = [300,300]#,100]
n_filters = [10,10]#,10]
filter_sizes = [3,5]#,3]
batch = 128
output_dim = 7
dropout_rate = 0.30
test_size=0.30

# configure logging at the root level of Lightning
logging.getLogger("lightning.pytorch").setLevel(logging.DEBUG)

logger = logging.getLogger("lightning.pytorch.core")
logger.addHandler(logging.FileHandler("logMLP.log"))
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

# Create a vocabulary of tokens
vocab = set()
for text in train_texts_processed:
    doc = nlp(text)
    tokens = [token.text for token in doc]
    vocab.update(tokens)

for text in test_texts_processed:
    doc = nlp(text)
    tokens = [token.text for token in doc]
    vocab.update(tokens)
# Assign unique indices to tokens
token2index = {token: i for i, token in enumerate(vocab)}

# Convert tokens to indices
train_sequences = [
    torch.tensor([token2index[token.text] for token in nlp(text)], dtype=torch.long)
    for text in train_texts_processed
]
test_sequences = [
    torch.tensor([token2index[token.text] for token in nlp(text)], dtype=torch.long)
    for text in test_texts_processed
]
vocab_size = len(vocab)
train_sequences = torch.nn.utils.rnn.pad_sequence(train_sequences, batch_first=True)
test_sequences = torch.nn.utils.rnn.pad_sequence(test_sequences, batch_first=True)

device = torch.device("cuda:0" if torch.cuda.is_available() else "cpu")
train_sequences = torch.tensor(train_sequences, dtype=torch.long).clone().detach().to(device)
test_sequences = torch.tensor(test_sequences, dtype=torch.long).clone().detach().to(device)

train_labels = torch.tensor(train_labels, dtype=torch.long).clone().detach().to(device)
test_labels = torch.tensor(test_labels, dtype=torch.long).clone().detach().to(device)
model = SentimentClassifier(vocab_size, embedding_dim, conv_embedding_dim, n_filters, filter_sizes, output_dim, dropout_rate)
model.apply(initialize_weights)

# Define the dataloaders
train_dataset = torch.utils.data.TensorDataset(train_sequences, train_labels)
test_dataset = torch.utils.data.TensorDataset(test_sequences, test_labels)

train_loader = DataLoader(train_dataset, batch_size=batch, shuffle=True, pin_memory=True, num_workers=3)
test_loader = DataLoader(test_dataset, batch_size=batch, shuffle=False, pin_memory=True, num_workers=3)

# Train the model
model = model.to(device)
early_stop_callback = EarlyStoppingCallback(monitor='val_loss', patience=10)

print(model)
trainer = pl.Trainer(max_epochs = 500, callbacks=[early_stop_callback],log_every_n_steps=1)
trainer.fit(model, train_loader, test_loader)    

torch.save(model.state_dict(), ".\models_pytorch\model_MLP.pt")
