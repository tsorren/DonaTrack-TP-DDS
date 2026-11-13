# Seeder de demo (SPEC-10): corre una vez contra el stack levantado y termina.
FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY seeder ./seeder
RUN useradd --create-home --uid 10001 seeder
USER seeder

ENTRYPOINT ["python", "-m", "seeder"]
CMD ["todo"]
