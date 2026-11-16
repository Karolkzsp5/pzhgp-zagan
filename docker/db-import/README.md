# Import danych do bazy w Dockerze

Plik `.sql` umieszczony w tym katalogu jest wczytywany automatycznie przy **pierwszym** starcie kontenera
z bazą, czyli wtedy, gdy baza w Dockerze jest jeszcze pusta. Przy kolejnych startach jest pomijany.

1. Zrób kopię lokalnej bazy w formacie SQL, bez właściciela i uprawnień:

   ```
   pg_dump -U nazwa_uzytkownika -d pzhgp_zagan --no-owner --no-privileges -f docker/db-import/pzhgp_zagan.sql
   ```

   W pgAdmin: baza → Backup… → format **Plain**, a w zakładce opcji zaznacz „Do not save” dla **Owner** i **Privileges**.

2. Uruchom aplikację: `docker compose up --build`.

Aby wczytać kopię ponownie (np. z nowszymi danymi), usuń bazę z Dockera poleceniem `docker compose down -v`,
podmień plik i uruchom aplikację jeszcze raz.

Pliki `.sql` z tego katalogu nie trafiają do repozytorium (`.gitignore`), bo zawierają dane użytkowników.
