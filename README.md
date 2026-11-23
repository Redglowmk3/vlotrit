# VLOTRIT Android

## Automatische Android-releases

De GitHub Actions-workflow bouwt bij wijzigingen aan de app een release voor `versionName` uit `app/build.gradle.kts`. Voor een nieuwe release moet `versionCode` ook met minimaal 1 worden verhoogd. De APK wordt steeds als `Vlotrit.apk` aan de GitHub-release toegevoegd; bestaande tags worden overgeslagen.

Eenmalig vereist: voeg de bestaande APK-ondertekeningssleutel als Actions-secret `VLOTRIT_KEYSTORE_BASE64` toe via **Settings → Secrets and variables → Actions**. De waarde is de Base64-tekst van de bestaande `debug.keystore`. Zonder dezelfde sleutel kan Android de app niet als update installeren. De workflow stopt als de sleutel ontbreekt.

Na het bijwerken van versiecode en bronbestanden push je naar `main`; GitHub bouwt en publiceert de release automatisch.
