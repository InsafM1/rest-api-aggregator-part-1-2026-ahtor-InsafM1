Выбранные API

В приложении реализованы три источника данных. Каждый из них соответствует требованиям задания: не менее пяти полей в ответе, вложенные структуры, query-параметры, изменяемость данных.

Open-Meteo (погода)

    URL: https://api.open-meteo.com/v1/forecast
    Параметры: latitude, longitude, current, hourly
    Особенности: ответ содержит вложенный объект current и массив hourly; работает без API-ключа

Frankfurter (курсы валют)

    URL: https://api.frankfurter.app/latest
    Параметры: base, symbols
    Особенности: ответ содержит вложенный объект rates (валюта → курс); данные обновляются ежедневно; работает без API-ключа

JSONPlaceholder (тестовые посты)

    URL: https://jsonplaceholder.typicode.com/posts
    Параметры: _limit, userId
    Особенности: возвращает массив объектов, каждый пост — отдельная запись; количество записей ограничивается параметром _limit

Архитектура

Приложение разделено на слои по принципу единственной ответственности.

    CLI (cli) — разбор аргументов, вызов агрегатора и хранилища, вывод результата пользователю. HTTP-запросов не выполняет.
    Агрегация (core.aggregation) — оркестрация опроса источников, сбор данных, обработка ошибок, вызов хранилища.
    Источники (core.source) — каждый API в отдельном подпакете. Отвечает за сборку URL, HTTP-запрос, парсинг JSON (DTO) и маппинг в единую модель.
    Хранилища (core.storage) — интерфейс Storage и две реализации: JsonStorage и CsvStorage.
    HTTP (core.http) — интерфейс HttpClientWrapper и реализация на java.net.http.HttpClient.
    Модель (core.model) — единая модель AggregatedItem для всех API.
    Сборка (AppFactory) — единственное место создания объектов (new). Зависимости передаются через конструкторы.

Логирование: SLF4J + Logback. Логи пишутся в консоль и в файл logs/app.log.
Сборка и запуск

Требования: Java 17+, Maven.

Сборка проекта (компиляция кода и создание исполняемого jar):

bash

mvn clean package

Запуск опроса API (сохранение данных в файл):

bash

java -jar target/rest-aggregator-1.0.jar poll --sources open-meteo,frankfurter --format json --mode new

Параметры:

    --sources — список источников через запятую; если не указан, опрашиваются все;
    --format — формат файла (json или csv);
    --mode — режим записи (new — перезаписать файл, append — дописать в конец).

Просмотр сохранённых данных (вывод на экран):

bash

# весь файл
java -jar target/rest-aggregator-1.0.jar show --format json

# только данные указанного источника
java -jar target/rest-aggregator-1.0.jar show --format json --source open-meteo

Тестирование

Тесты покрывают бизнес-логику, хранилища, агрегатор и CLI. Сетевые вызовы к реальным API не выполняются: HTTP-клиент подменяется заглушкой через Mockito.

Запуск всех тестов:

bash

mvn test

Что покрыто:

    OpenMeteoSourceTest, FrankfurterSourceTest, JsonPlaceholderSourceTest — маппинг JSON в единую модель, сборка URL, обработка ошибок;
    JsonStorageTest, CsvStorageTest — цикл save/load, режимы NEW/APPEND, фильтрация по источнику;
    AggregatorTest — оркестрация, устойчивость к падению одного из источников;
    JavaHttpClientWrapperTest — обработка статусов 200 и 500, недоступность сети (локальный тестовый сервер);
    CliTest — валидация аргументов (неверный формат, отсутствие значения).
