FROM python:3.12-slim-bookworm

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

RUN rm -f /etc/apt/sources.list.d/debian.sources \
    && printf '%s\n' \
    'deb https://mirrors.aliyun.com/debian bookworm main contrib non-free non-free-firmware' \
    'deb https://mirrors.aliyun.com/debian bookworm-updates main contrib non-free non-free-firmware' \
    'deb https://mirrors.aliyun.com/debian-security bookworm-security main contrib non-free non-free-firmware' \
    > /etc/apt/sources.list \
    && apt-get update -o Acquire::Retries=3 \
    && apt-get install -y --no-install-recommends \
    build-essential \
    curl \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    libxml2-dev \
    libxslt1-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./
RUN pip install \
    --index-url https://mirrors.aliyun.com/pypi/simple/ \
    --trusted-host mirrors.aliyun.com \
    --retries 5 \
    --timeout 120 \
    -r requirements.txt

COPY . .

EXPOSE 5000

CMD ["python", "main.py", "-m", "http", "-p", "5000"]
