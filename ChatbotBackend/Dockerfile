# Use the official Python image
FROM python:3.9.0-slim-buster

WORKDIR /usr/app

COPY requirements.txt .

# upgrade pip
RUN pip install --upgrade pip

# Install the dependencies
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

COPY start.sh /
RUN chmod +x /start.sh
ENTRYPOINT ["/start.sh"]