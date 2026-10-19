# Multi Cloner

Aplicatie Android (Kotlin + Jetpack Compose) in stilul Parallel Space / App Cloner:

- **Add Clone** - alegi o aplicatie instalata si o clonezi (cont separat, date separate)
- **Add Folder** - creezi foldere si iti organizezi clonele in ele
- mutare clona in alt folder, redenumire / stergere folder (clonele raman, trec la "Fara folder"), pliere folder

## Motorul de clonare

Clonarea reala foloseste motorul de virtualizare **BlackBox**, inclus in proiect ca module Gradle:

| Modul | Sursa |
|---|---|
| `Bcore`, `black-reflection`, `compiler` | [ALEX5402/NewBlackbox](https://github.com/ALEX5402/NewBlackbox) (fork de BlackBox, Apache 2.0) |

Nota: repo-ul original `FBlackBox/BlackBox` a fost sters de autor (a ramas doar un README),
deci nu exista cod de integrat din el. NewBlackbox e un fork complet al lui si e singurul folosit.
Licenta motorului: `LICENSE-BlackBox` (Apache 2.0) - pastreaz-o in proiect.

Fisierele proprii: `app/src/main/java/com/example/multicloner/`
- `App.kt` - porneste motorul
- `CloneEngine.kt` - legatura cu BlackBox (install / launch / uninstall)
- `Store.kt` - foldere si clone (salvate local)
- `MainActivity.kt` - interfata

## Cum obtii APK-ul (GitHub)

1. Creeaza un repository nou pe GitHub si urca tot continutul acestui folder
   (inclusiv `gradlew` si folderul `gradle/`).
2. Tab-ul **Actions** -> workflow-ul **Build APK** ruleaza la fiecare push (sau **Run workflow**).
3. Dupa cateva minute (primul build e mai lung, compileaza cod nativ) descarci artefactul
   **MultiCloner-debug-apk** -> `app-debug.apk`. Instaleaza-l (permite "surse necunoscute").

Build local: JDK 21, Android SDK 35 si NDK `29.0.13846066`, apoi `./gradlew :app:assembleDebug`.

## Limitari

- Nu am putut compila proiectul in mediul in care l-am scris; daca primul build pe GitHub da eroare, uita-te in log.
- Aplicatii cu protectii (banci, jocuri cu anti-cheat, unele cu Play Integrity) pot refuza sa ruleze intr-un mediu virtual.
- `targetSdk` e 28 intentionat (compatibilitate cu motorul), deci nu poti publica aplicatia in Google Play in forma asta.
- APK-ul contine arhitecturile arm64 si arm32; aplicatiile doar pe 32 de biti pot sa nu apara/porneasca pe unele telefoane.
- Foloseste clone doar pentru conturile tale si respecta termenii aplicatiilor clonate.
