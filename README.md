# Forte Bank Statement Parser Bot

Telegram бот на Kotlin для автоматического извлечения и анализа данных из PDF выписок Forte Bank.

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## Функциональность

Бот парсит PDF выписки из Forte Bank и извлекает следующую информацию:

- Владелец счета
- Номер счета
- Валюта
- Период выписки
- Начальный и конечный баланс
- Список транзакций с датами, описаниями и суммами
- Общая сумма дебетовых и кредитовых операций

## Требования

- Java 17 или выше
- Kotlin 2.2.20
- Gradle 8.x

## Настройка

1. Клонируйте репозиторий
2. Создайте файл `.env` или установите переменные окружения:

```bash
export BOT_TOKEN="your_bot_token_here"
export ALLOWED_USERS="123456789,987654321"  # опционально, список ID пользователей через запятую
```

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
docker run -e BOT_TOKEN=your_token forte-saver
```

## Использование

1. Запустите бота командой `/start`
2. Отправьте PDF файл выписки из Forte Bank
3. Получите отформатированный отчет с информацией о выписке

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
└── commands/
    ├── StartCommand.kt              # Команда /start
    ├── HelpCommand.kt               # Команда /help
    └── ParseStatementCommand.kt     # Обработчик PDF файлов
```

## Технологии

- [ktgbotapi](https://github.com/InsanusMokrassar/TelegramBotAPI) - Kotlin библиотека для Telegram Bot API
- [Apache PDFBox](https://pdfbox.apache.org/) - Парсинг PDF файлов
- [Apache POI](https://poi.apache.org/) - Парсинг Excel файлов (для будущего использования)
- [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) - Сериализация данных

## Лицензия

MIT
