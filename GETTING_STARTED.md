# Getting Started - Первый запуск

## Шаг 1: Создание Telegram бота

1. Откройте Telegram и найдите [@BotFather](https://t.me/BotFather)
2. Отправьте команду `/newbot`
3. Введите имя вашего бота (например: "Forte Bank Parser")
4. Введите username бота (должен заканчиваться на "bot", например: "forte_bank_parser_bot")
5. Сохраните полученный токен (выглядит как `1234567890:ABCdefGHIjklMNOpqrsTUVwxyz`)

## Шаг 2: Узнайте свой Telegram User ID

1. Откройте [@userinfobot](https://t.me/userinfobot)
2. Нажмите "Start"
3. Бот отправит вам ваш User ID (например: 123456789)
4. Сохраните этот ID

## Шаг 3: Настройка PostgreSQL

### Установка PostgreSQL (если еще не установлен)

**macOS:**
```bash
brew install postgresql@16
brew services start postgresql@16
```

**Linux:**
```bash
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Создание базы данных

```bash
# Подключитесь к PostgreSQL
psql -U postgres

# Создайте базу данных
CREATE DATABASE forte_saver;

# Выйдите
\q
```

## Шаг 4: Настройка переменных окружения

### Вариант A: Создание .env файла

```bash
cd /Users/alex/IdeaProjects/forte-saver
cp .env.example .env
```

Отредактируйте файл `.env`:
```bash
BOT_TOKEN=ваш_токен_от_BotFather
ALLOWED_USERS=ваш_user_id
DATABASE_URL=jdbc:postgresql://localhost:5432/forte_saver?user=postgres&password=ваш_пароль
```

### Вариант B: Export переменных (для временного использования)

```bash
export BOT_TOKEN="ваш_токен_от_BotFather"
export ALLOWED_USERS="ваш_user_id"
export DATABASE_URL="jdbc:postgresql://localhost:5432/forte_saver?user=postgres&password=ваш_пароль"
```

Если хотите разрешить доступ нескольким пользователям:
```bash
export ALLOWED_USERS="123456789,987654321,555555555"
```

Если хотите разрешить доступ всем (не рекомендуется):
```bash
export ALLOWED_USERS=""
```

## Шаг 5: Запуск бота

**Важно:** При первом запуске бот автоматически применит миграции БД через Flyway.

### Способ 1: Через Gradle (для разработки)

```bash
cd /Users/alex/IdeaProjects/forte-saver

# Убедитесь, что переменные окружения установлены
echo $BOT_TOKEN
echo $ALLOWED_USERS

# Запустите бота
./gradlew run
```

### Способ 2: Через собранный JAR

```bash
# Соберите проект
./gradlew build

# Запустите JAR
java -jar build/libs/forte-saver-1.0-SNAPSHOT.jar
```

### Способ 3: Через Docker

```bash
# Соберите Docker образ
docker build -t forte-saver .

# Запустите контейнер
docker run -d \
  --name forte-saver \
  -e BOT_TOKEN="ваш_токен" \
  -e ALLOWED_USERS="ваш_user_id" \
  --restart unless-stopped \
  forte-saver
```

### Способ 4: Через Docker Compose (рекомендуется)

```bash
# Создайте .env файл (см. Шаг 3)
# Затем запустите:
docker-compose up -d

# Проверьте логи:
docker-compose logs -f

# Остановите:
docker-compose down
```

## Шаг 6: Проверка работы

1. Откройте Telegram
2. Найдите вашего бота по username (который вы создали в Шаге 1)
3. Нажмите "Start" или отправьте команду `/start`
4. Вы должны увидеть приветственное сообщение

Если бот не отвечает:
- Проверьте, что бот запущен (смотрите логи)
- Проверьте правильность токена бота
- Проверьте, что ваш User ID указан в ALLOWED_USERS
- Проверьте подключение к базе данных

## Шаг 7: Тестирование с PDF файлом

1. Отправьте боту PDF файл выписки из Forte Bank
2. Дождитесь сообщения "Processing your bank statement..."
3. Получите краткую статистику импорта:
   - Общее количество транзакций
   - Количество новых транзакций
   - Количество пропущенных дубликатов

Пример выписки находится в корне проекта: `Formed statement (2).pdf`

**Повторная загрузка:** Если вы загрузите ту же выписку повторно, бот не создаст дубликатов - все транзакции будут пропущены.

## Полезные команды

### Просмотр логов (если запущено через Gradle)
```bash
# Логи выводятся в консоль
```

### Просмотр логов (если запущено через Docker)
```bash
docker logs -f forte-saver
```

### Остановка бота
```bash
# Gradle: Ctrl+C

# Docker:
docker stop forte-saver

# Docker Compose:
docker-compose down
```

### Перезапуск бота
```bash
# Docker:
docker restart forte-saver

# Docker Compose:
docker-compose restart
```

## Тестирование парсера (без бота)

Если хотите просто протестировать парсинг PDF без запуска бота:

```bash
# Поместите PDF файл в корень проекта с именем "Formed statement (2).pdf"
# Затем запустите:
./gradlew testParser
```

Это покажет извлеченные данные в консоли без необходимости запуска Telegram бота.

## Управление миграциями БД

Проект использует Flyway для управления схемой базы данных.

### Расположение миграций
Все SQL миграции находятся в: `src/main/resources/db/migration/`

### Именование миграций
Миграции должны следовать формату: `V{версия}__{описание}.sql`

Примеры:
- `V1__initial_schema.sql` - начальная схема
- `V2__add_user_settings.sql` - добавление настроек пользователя

### Создание новой миграции

1. Создайте SQL файл в `src/main/resources/db/migration/`
2. Назовите его согласно формату (например: `V2__add_indexes.sql`)
3. Напишите SQL команды для изменения схемы
4. Миграция применится автоматически при следующем запуске

### Проверка статуса миграций

Flyway автоматически ведет таблицу `flyway_schema_history` в БД, где хранится информация о всех примененных миграциях.

```sql
-- Подключитесь к БД
psql -U postgres -d forte_saver

-- Посмотрите историю миграций
SELECT * FROM flyway_schema_history;
```

## Возможные проблемы

### "BOT_TOKEN environment variable is not set"
- Убедитесь, что переменная окружения BOT_TOKEN установлена
- Проверьте .env файл или экспорт переменной

### Ошибки подключения к базе данных
- Проверьте, что PostgreSQL запущен: `pg_isready`
- Проверьте DATABASE_URL (правильный хост, порт, пользователь, пароль)
- Проверьте, что база данных forte_saver создана
- Убедитесь, что у пользователя есть права на создание таблиц

### "Access denied"
- Ваш User ID не в списке ALLOWED_USERS
- Проверьте правильность вашего User ID
- Или оставьте ALLOWED_USERS пустым для доступа всем

### "Please send a PDF file"
- Убедитесь, что отправляете именно PDF файл
- Файл должен иметь MIME type "application/pdf"

### "Failed to parse bank statement"
- Формат PDF может отличаться от ожидаемого
- Проверьте логи для деталей ошибки
- Возможно потребуется настройка парсера в ForteBankStatementParser.kt

### Ошибки при вставке в БД
- Проверьте логи бота на наличие SQL ошибок
- Убедитесь, что таблицы были созданы корректно
- При необходимости пересоздайте базу данных

## Следующие шаги

После успешного запуска см.:
- [README.md](README.md) - полная документация
- [QUICKSTART.md](QUICKSTART.md) - краткое руководство
- [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) - обзор проекта
