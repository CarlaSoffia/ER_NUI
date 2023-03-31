import optuna as optuna
import numpy as np
import pandas as pd
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import DataLoader
from torchtext import data
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import spacy

np.random.seed(0)
torch.manual_seed(42)
dataset = pd.read_csv("ISEAR_only_text_pt_data_augmentation.csv", sep=";")
nlp = spacy.load('pt_core_news_sm') # python -m spacy download pt_core_news_sm

# Define the hyperparameter search space
def objective(trial):
    # Define hyperparameters to tune
    num_words = trial.suggest_int('num_words', 1000, 5000)
    num_filters = trial.suggest_int('num_filters', 16, 128)
    batch = trial.suggest_int('batches', 32, 256)
    filter_size = trial.suggest_int('filter_size', 3, 7)
    learning_rate = trial.suggest_float('learning_rate', 1e-5, 1e-2)
    dropout_rate = trial.suggest_float('dropout_rate', 0.5, 0.9)
    test_size = trial.suggest_float('test_size', 0.1, 0.4)
    #output_dim = trial.suggest_int('output_dim', 32, 256)
    le = LabelEncoder()
    dataset['sentiment'] = le.fit_transform(dataset['sentiment'])
    train_texts, test_texts, train_labels, test_labels = train_test_split(dataset['text'], dataset['sentiment'], test_size=test_size, random_state=0)

    # Tokenize the texts
    text_field = data.Field(
        tokenize=lambda text: [token.text for token in nlp(text)],
        lower=True,
        include_lengths=False
    )
    text_field.build_vocab(train_texts, max_size=num_words)
    #train_sequences = text_field.process(train_texts)[1]
    #test_sequences = text_field.process(test_texts)[1]
    #train_sequences = text_field.numericalize(text_field.pad(train_texts))
    #test_sequences = text_field.numericalize(text_field.pad(test_texts))
    train_padded = text_field.pad(train_texts)
    test_padded = text_field.pad(test_texts)
    
    train_sequences = text_field.numericalize(train_padded).transpose(0, 1)
    test_sequences = text_field.numericalize(test_padded).transpose(0, 1)
    train_labels = np.array(train_labels)
    test_labels = np.array(test_labels)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    train_sequences, test_sequences = train_sequences.to(device), test_sequences.to(device)
    train_labels, test_labels = torch.tensor(train_labels).to(device), torch.tensor(test_labels).to(device)
    
    # Define the model
    class SentimentClassifier(nn.Module):
        def __init__(self, num_filters, filter_size, dropout_rate,output_dim=100):
            super(SentimentClassifier, self).__init__()
            self.embedding = nn.Embedding(output_dim, output_dim)
            self.conv1d = nn.Conv1d(output_dim, num_filters, filter_size)
            self.dropout = nn.Dropout(dropout_rate)
            self.fc = nn.Linear(num_filters, 7)

        def forward(self, x):
            x = self.embedding(x)
            x = x.transpose(1, 2)
            x = self.conv1d(x)
            x = nn.ReLU()(x)
            x = nn.AdaptiveMaxPool1d(1)(x)
            x = x.squeeze(2)
            x = self.dropout(x)
            x = torch.flatten(x, start_dim=1)
            x = self.fc(x)
            return x

    model = SentimentClassifier(num_filters, filter_size, dropout_rate, output_dim=100)
    
    # Define the loss and optimizer
    criterion = nn.CrossEntropyLoss()
    optimizer = optim.Adam(model.parameters(), lr=learning_rate)

    # Define the dataloaders
    train_dataset = torch.utils.data.TensorDataset(train_sequences, train_labels)
    test_dataset = torch.utils.data.TensorDataset(test_sequences, test_labels)
    train_loader = DataLoader(train_dataset, batch_size=batch, shuffle=True)
    test_loader = DataLoader(test_dataset, batch_size=batch, shuffle=False)

    # Train the model
    model = model.to(device)
    num_epochs = 30

    for epoch in range(num_epochs):
        model.train()
        train_losses = []
        for inputs, labels in train_loader:
            inputs, labels = inputs.to(device), labels.to(device)
            optimizer.zero_grad()
            outputs = model(inputs)
            loss = criterion(outputs, labels)
            loss.backward()
            optimizer.step()
            train_losses.append(loss.item())
        model.eval()
        test_losses = []
        correct = 0
        total = 0
        with torch.no_grad():
            for inputs, labels in test_loader:
                inputs, labels = inputs.to(device), labels.to(device)
                outputs = model(inputs)
                loss = criterion(outputs, labels)
                test_losses.append(loss.item())
                #predicted = torch.max(outputs.data, 1)[1]
                predicted = torch.max(outputs.data, 1)
                total += labels.size(0)
                correct += (predicted == labels).sum().item()
        accuracy = correct / total
        if epoch % 5 == 0:
            print(f"Epoch: {epoch}, Train Loss: {np.mean(train_losses)}, Test Loss: {np.mean(test_losses)}, Accuracy: {accuracy}")

    # Return the accuracy as the score for Optuna to optimize
    return accuracy

def print_best_params(study, trial):
    with open('optuna_results.txt', 'a') as file:
        file.write(f"[Trial {trial.number}] Accuracy: {study.best_value} | Best parameters - {trial.params}\n")

# Define the optuna study object and start the optimization process
study = optuna.create_study(direction='maximize')
study.optimize(objective, n_trials=20, timeout=3600, callbacks=[print_best_params])

print(f'Best hyperparameters: {study.best_params}')
print(f'Accuracy: {study.best_value}')