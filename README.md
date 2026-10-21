# UnoRank: aplikacja Android

Pierwszy etap: Kotlin i Jetpack Compose. Działające logowanie administratora do backendu Laravel, weryfikacja zapisanej sesji, menu główne, wylogowanie urządzenia i wyjście z aplikacji. Motyw jasny i ciemny korzystają z koloru RGB(255,125,255).

Menu pokazuje przyszłe funkcje: rozpoczęcie gry, użytkowników i historię rozgrywek. Są oznaczone „Wkrótce” i jeszcze nie wykonują działań. Obsługa graczy, gier oraz trwała kolejka offline należą do następnych etapów. Ta wersja wymaga połączenia z serwerem podczas logowania i otwierania menu.

## Uruchomienie projektu

Wymagania: JDK 17 lub 21, Android SDK z platformą 36, Build Tools 35.0.0 i Platform Tools. Telefon: Android 8.0 lub nowszy. Projekt można otworzyć w Android Studio obsługującym Android Gradle Plugin 8.13.2.

Ustaw `ANDROID_HOME` na katalog SDK albo utwórz nieśledzony plik `local.properties` z wpisem `sdk.dir=/pełna/ścieżka/do/sdk`.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Instalator: `app/build/outputs/apk/debug/app-debug.apk`. Na telefonie aplikacja nazywa się „UnoRank Test”. Jest podpisana lokalnym kluczem testowym. Aktualizacje wymagają tego samego klucza. APK z innego komputera lub GitHub Actions może mieć inny podpis. Klucze i pliki konfiguracyjne komputera nie trafiają do repozytorium.

## Pierwszy test na telefonie przez USB

1. Uruchom backend UnoRank. Adres `http://localhost:8000/up` na komputerze powinien potwierdzać działanie aplikacji.
2. W telefonie włącz opcje programistyczne i debugowanie USB. Zwykle wymaga to kilkukrotnego dotknięcia numeru kompilacji w ustawieniach telefonu.
3. Podłącz telefon kablem umożliwiającym przesyłanie danych. Odblokuj ekran i zaakceptuj zgodę na debugowanie z tego komputera.
4. W katalogu projektu uruchom `./bin/install-phone.sh`. Skrypt wymaga dokładnie jednego dostępnego urządzenia, instaluje APK, ustawia przekierowanie portu i otwiera aplikację.
5. Wpisz login i hasło administratora utworzonego w backendzie. Nie ma fabrycznego hasła. Login gracza nie jest kontem administratora.

Skrypt można uruchomić ponownie po odłączeniu kabla, aby odnowić połączenie. Sama aplikacja pozostanie zainstalowana. Alternatywnie użyj `adb reverse tcp:8000 tcp:8000`, bez ponownej instalacji.

Dzięki przekierowaniu przez USB domyślny adres `http://127.0.0.1:8000/api/v1/` w aplikacji prowadzi do backendu na komputerze. Nie trzeba otwierać portu w sieci domowej ani modyfikować konfiguracji Docker. Komputer i backend muszą działać przez cały test. Szczegóły narzędzia: [Android Debug Bridge](https://developer.android.com/tools/adb).

Jeżeli nie utworzono jeszcze konta, w katalogu backendu uruchom `docker compose exec app php artisan unorank:create-admin admin`. Polecenie zapyta o hasło. W instalacji Dockera wymagającej uprawnień administratora poprzedź je `sudo`.

## Emulator i serwer docelowy

Emulator Android Studio: w ekranie logowania rozwiń „Ustawienia połączenia” i wpisz `http://10.0.2.2:8000`. Jest to adres komputera widziany z emulatora. Debug dopuszcza HTTP wyłącznie dla `127.0.0.1`, `localhost` i `10.0.2.2`.

Dla serwera docelowego podaj adres HTTPS. Akceptowany jest adres bez ścieżki lub zakończony `/api/v1`. Release wymaga HTTPS, nie ma wpisanego domyślnego serwera i nie zawiera testowych wyjątków dla HTTP. `assembleRelease` tworzy niepodpisany pakiet; podpisywanie wersji docelowej przygotujemy przy publikacji.

## Sesja administratora

Aplikacja używa `POST /api/v1/auth/login`, `GET /api/v1/auth/me` i `POST /api/v1/auth/logout`. Wysyła nagłówek `Accept: application/json`. Token pozostaje przypisany do adresu serwera, który go wydał. Przekierowania HTTP są wyłączone.

Hasło jest trzymane tylko w pamięci ekranu i czyszczone przy wysłaniu. Nie trafia do zapisanego stanu ani preferencji. Token i data wygaśnięcia są szyfrowane AES GCM kluczem z Android Keystore. Dane sesji są wyłączone z kopii zapasowych i przenoszenia na inny telefon.

Po ponownym uruchomieniu aplikacja sprawdza datę wygaśnięcia tokenu i potwierdza konto przez API. Błąd sieci zachowuje sesję i pozwala ponowić sprawdzenie. Brak autoryzacji usuwa zapisany token i przywraca logowanie. Wylogowanie unieważnia token tego urządzenia na serwerze. Jeśli serwer jest niedostępny, aplikacja pokazuje błąd i pozwala spróbować ponownie. Wyjście z aplikacji nie wylogowuje konta.

## Sprawdzenie pierwszego etapu

1. Błędne hasło: komunikat po polsku i brak wejścia do menu.
2. Poprawny login: menu z nazwą administratora, różowym akcentem i trzema funkcjami oznaczonymi „Wkrótce”.
3. Ponowne otwarcie: sprawdzenie zapisanego konta bez ponownego wpisywania hasła.
4. Brak serwera podczas ponownego otwarcia: komunikat i przycisk ponowienia, bez utraty sesji.
5. Wylogowanie: powrót do logowania i unieważnienie tokenu na backendzie.

Testy JVM sprawdzają kontrakt HTTP, obsługę błędów i stan logowania. Testy Compose w Robolectric sprawdzają ekrany, dostępność pól, blokowanie pustego formularza oraz większy tekst na małym ekranie. Zrzuty powstają w `app/build/screenshots`. Kontrola na fizycznym telefonie jest nadal potrzebna dla instalacji, klawiatury, Android Keystore i połączenia z rzeczywistym backendem.
