# Forte Bank Statement Parser Bot

Telegram бот на Kotlin для автоматического извлечения и анализа данных из PDF выписок Forte Bank.

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## Функциональность

Бот парсит PDF выписки из Forte Bank и сохраняет данные в PostgreSQL базу данных:

- Владелец счета
- Номер счета
- Валюта
- Период выписки
- Начальный и конечный баланс
- Список транзакций с датами, описаниями и суммами
- Общая сумма дебетовых и кредитовых операций
- Автоматическая дедупликация транзакций при повторной загрузке

## Требования

- Java 24 или выше
- Kotlin 2.2.20
- Gradle 8.x
- PostgreSQL 12 или выше

## Настройка

1. Клонируйте репозиторий
2. Создайте PostgreSQL базу данных:

```sql
CREATE DATABASE forte_saver;
```

3. Создайте файл `.env` или установите переменные окружения:

```bash
export BOT_TOKEN="your_bot_token_here"
export ALLOWED_USERS="123456789,987654321"  # опционально, список ID пользователей через запятую
export DATABASE_URL="jdbc:postgresql://localhost:5432/forte_saver?user=postgres&password=yourpassword"
```

Примечание: Миграции БД применяются автоматически при запуске бота через Flyway.

## Сборка

```bash
./gradlew build
```

## Запуск

```bash
./gradlew run
```

Или с использованием Docker:

```bash
./gradlew jibDockerBuild
docker run \
  -e BOT_TOKEN=your_token \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/forte_saver?user=postgres&password=yourpassword" \
  forte-saver
```

## Использование

1. Запустите бота командой `/start`
2. Отправьте PDF файл выписки из Forte Bank
3. Бот сохранит все данные в базу данных и отправит краткую статистику импорта:
   - Общее количество транзакций
   - Количество новых транзакций
   - Количество пропущенных дубликатов

## Команды бота

- `/start` - Начать работу с ботом
- `/help` - Показать справку

## Структура проекта

```
src/main/kotlin/
├── Main.kt                          # Точка входа приложения
├── Config.kt                        # Конфигурация
├── BankStatement.kt                 # Модели данных
├── ForteBankStatementParser.kt      # Парсер PDF выписок
├── LocalDateSerializer.kt           # Сериализаторы для дат
├── database/
│   ├── DatabaseManager.kt           # Управление подключением к БД
│   ├── Tables.kt                    # Схема базы данных
│   └── StatementRepository.kt       # Репозиторий для работы с данными
└── commands/
    ├── StartCommand.kt              # Команда /start
    ├── HelpCommand.kt               # Команда /help
    └── ParseStatementCommand.kt     # Обработчик PDF файлов
```

## База данных

Структура БД состоит из двух таблиц:

### statements
- id (PK)
- account_holder
- account_number (indexed)
- currency
- period_from, period_to
- opening_balance, closing_balance
- created_at
- imported_by (Telegram user ID)

### transactions
- id (PK)
- statement_id (FK to statements)
- date_time
- description (text)
- debit, credit, balance
- reference
- transaction_hash (SHA-256, indexed)
- UNIQUE(statement_id, transaction_hash) - для дедупликации

## Технологии

- [ktgbotapi](https://github.com/InsanusMokrassar/TelegramBotAPI) - Kotlin библиотека для Telegram Bot API
- [Apache PDFBox](https://pdfbox.apache.org/) - Парсинг PDF файлов
- [Apache POI](https://poi.apache.org/) - Парсинг Excel файлов (для будущего использования)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) - Сериализация данных
- [Exposed](https://github.com/JetBrains/Exposed) - Kotlin SQL библиотека
- [PostgreSQL](https://www.postgresql.org/) - Реляционная база данных
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - Connection pooling
- [Flyway](https://flywaydb.org/) - Database migration tool

## Лицензия

MIT
